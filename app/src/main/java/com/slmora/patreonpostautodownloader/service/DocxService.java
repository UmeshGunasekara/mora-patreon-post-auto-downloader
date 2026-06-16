/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 2:49 PM
 */
package com.slmora.patreonpostautodownloader.service;

import com.slmora.common.logging.MoraLogger;
import com.slmora.common.logging.MoraLoggerThreadInfo;
import com.slmora.patreonpostautodownloader.model.DateParts;
import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.process.ProcessDocxProducer;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.docx4j.wml.Br;
import org.docx4j.wml.Drawing;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.P;
import org.docx4j.wml.PPr;
import org.docx4j.wml.PPrBase;
import org.docx4j.wml.R;
import org.docx4j.wml.Text;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The {@code DocxService} class is created for generating DOCX reports from
 * Patreon post Excel batches.
 * <p>
 * This service reads rows from the configured Excel sheet, converts post
 * metadata and content JSON into WordprocessingML paragraphs, downloads post
 * images for embedding, and saves a DOCX file whose name is derived from the
 * source Excel batch name.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Derives final DOCX file names from finalized Excel batch file names.</li>
 *     <li>Reads required Excel columns such as {@code published_at}, {@code title}, {@code content_json_string}, {@code thumb_url}, and {@code large_url}.</li>
 *     <li>Creates date, time, title, content, URL, and image sections in a DOCX document.</li>
 *     <li>Extracts readable text from Patreon content JSON and preserves line breaks in DOCX output.</li>
 *     <li>Embeds downloaded image bytes into the generated WordprocessingML package.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link ExcelJob}<br>
 * 2 - {@link ProcessDocxProducer}<br>
 * 3 - {@link DateParts}<br>
 * 4 - {@link WordprocessingMLPackage}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link DocxService#createDocx(ExcelJob, Path, String, String, String)}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>The service depends on the Excel column names produced by {@link ExcelService}.</li>
 *     <li>Image download failures inside document generation are logged and represented as text in the DOCX instead of failing the whole document.</li>
 *     <li>If the Excel file name does not match the configured pattern, the generated DOCX file name falls back to {@code _tmp}.</li>
 * </ul>
 *
 * @author: SLMORA
 * @since 1.0
 *
 * <h4>Revision History</h4>
 * <blockquote><pre>
 * <br>Version      Date            Editor              Note
 * <br>-------------------------------------------------------
 * <br>1.0          6/6/2026      SLMORA                Initial Code
 * </pre></blockquote>
 */
public class DocxService
{
    /**
     * Class-scoped logger used for DOCX naming, image download, JSON parsing,
     * and document generation diagnostics.
     */
    private final static MoraLogger LOGGER = MoraLogger.getLogger(DocxService.class);

    /**
     * Shared docx4j object factory used to create WordprocessingML paragraphs,
     * runs, text, drawings, and style objects.
     */
    private static final ObjectFactory WML_OBJECT_FACTORY = Context.getWmlObjectFactory();

    /**
     * Intended image request timeout used by image download request construction
     * when timeout configuration is enabled.
     */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    /**
     * <h3>Create DOCX report</h3>
     * Creates a DOCX report for the supplied Excel job and records the generated
     * DOCX path on the job.
     * <p>
     * The final DOCX name is derived from the Excel file name using the supplied
     * regular expression and temporary DOCX file name pattern. The method then
     * delegates workbook reading and document writing to the internal DOCX writer.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Derives the final DOCX file name with {@link DocxService#getFinalDocxFileName(String, String, String)}.</li>
     *     <li>Resolves the final output path under the configured DOCX output directory.</li>
     *     <li>Stores the generated DOCX path on the {@link ExcelJob}.</li>
     *     <li>Writes the DOCX content from the job Excel file and configured sheet.</li>
     * </ul>
     *
     * @param job Excel job containing the finalized Excel file path
     * @param docxOutputDirPath directory where the DOCX report should be written
     * @param docxFileNamePattern regular expression used to extract the Excel batch suffix
     * @param tempDocxFileName DOCX file name template containing {@code temp}
     * @param excelSheetName Excel sheet name containing post records
     *
     * @throws IOException when the Excel workbook cannot be read or DOCX output cannot be written
     * @throws Docx4JException when docx4j cannot create or save document content
     *
     * @apiNote The job must have a non-null Excel file path before this method is called.
     * @since 1.0
     */
    public void createDocx(ExcelJob job, Path docxOutputDirPath, String docxFileNamePattern, String tempDocxFileName, String excelSheetName) throws
            IOException,
            Docx4JException
    {

        String finalDocxFileName = getFinalDocxFileName(job.getExcelFile().getFileName().toString(), docxFileNamePattern, tempDocxFileName);
        LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                        Thread.currentThread().threadId(),
                        Thread.currentThread().getStackTrace()),
                "finalDocxFileName is {}", finalDocxFileName);

        Path docxFilePath = docxOutputDirPath.resolve(finalDocxFileName);

        job.setDocxFile(docxFilePath);

        writeOnDocxFromExcel(job.getExcelFile().toString(), docxFilePath.toString(), excelSheetName);
    }

    /**
     * <h3>Write DOCX content from Excel</h3>
     * Reads a Patreon post Excel workbook and writes its rows into a new DOCX
     * document.
     * <p>
     * The document is grouped by publication date. Each post contributes a time
     * heading, post id, title, optional embedded images, readable content text,
     * and Patreon URL.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Opens the Excel workbook and validates that a header row exists.</li>
     *     <li>Builds a column-name map so rows can be read by stable column names.</li>
     *     <li>Uses thumbnail URLs first and falls back to large image URLs when thumbnails are missing.</li>
     *     <li>Downloads and embeds images while keeping document generation alive if one image fails.</li>
     *     <li>Saves the generated WordprocessingML package to the requested DOCX path.</li>
     * </ul>
     *
     * @param excelFilePath source Excel workbook path
     * @param docxFilePath target DOCX file path
     * @param excelSheetName sheet name containing post records
     *
     * @throws IOException when the workbook cannot be read or the DOCX file cannot be written
     * @throws Docx4JException when docx4j cannot create or save document content
     * @since 1.0
     */
    private void writeOnDocxFromExcel(String excelFilePath, String docxFilePath, String excelSheetName) throws
            IOException, Docx4JException
    {
        try (FileInputStream fis = new FileInputStream(excelFilePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();

            Sheet sheet = workbook.getSheet(excelSheetName);
            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                throw new IllegalStateException("Excel header row is missing.");
            }

            Map<String, Integer> columnMap = buildColumnMap(headerRow, formatter);

            String currentDate = null;
            HttpClient httpClient = HttpClient.newHttpClient();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String id = getCellValue(row, columnMap, "id", formatter);
                String publishedAt = getCellValue(row, columnMap, "published_at", formatter);
                String title = getCellValue(row, columnMap, "title", formatter);
                String contentJsonString = getCellValue(row, columnMap, "content_json_string", formatter);
                String patreonUrl = getCellValue(row, columnMap, "patreon_url", formatter);

                String imageUrl = getCellValue(row, columnMap, "thumb_url", formatter);
                if (imageUrl.isBlank()) {
                    // Prefer thumbnails for smaller DOCX output, but fall back to large images when thumbnails are missing.
                    imageUrl = getCellValue(row, columnMap, "large_url", formatter);
                }

                DateParts dateParts = splitPublishedAt(publishedAt);

                if (!Objects.equals(currentDate, dateParts.getDate())) {
                    // Add a new date heading only when the sorted Excel rows move to a different publication date.
                    wordMLPackage.getMainDocumentPart().addObject(createStyledParagraph(dateParts.getDate(), "Heading2"));
                    currentDate = dateParts.getDate();
                }

                wordMLPackage.getMainDocumentPart().addObject(createStyledParagraph(dateParts.getTime(), "Heading3"));
                wordMLPackage.getMainDocumentPart().addObject(createParagraph(id));
                wordMLPackage.getMainDocumentPart().addObject(createStyledParagraph(title, "Heading4"));

                if (!imageUrl.isBlank()) {
                    List<String> images = new ArrayList<>();
                    if(imageUrl.contains("|")){
                        for(String image: imageUrl.split("\\|")){
                            String trimmedImage = image.trim();
                            if (!trimmedImage.isBlank()) {
                                images.add(trimmedImage);
                            }
                        }
                    }else {
                        images.add(imageUrl);
                    }
                    for(String image : images) {
                        try {
                            byte[] imageBytes = downloadImage(httpClient, image);
                            P imageParagraph = createImageParagraph(wordMLPackage,
                                    imageBytes,
                                    1,
                                    "patreon-image",
                                    450,
                                    280);
                            wordMLPackage.getMainDocumentPart().addObject(imageParagraph);
                        } catch (Exception ex) {
                            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                            Thread.currentThread().threadId(),
                                            Thread.currentThread().getStackTrace()), ex);
                            // Keep the report readable even when one remote image cannot be embedded.
                            wordMLPackage.getMainDocumentPart().addObject(
                                    createParagraph("[Image download failed] " + image)
                            );
                        }
                        wordMLPackage.getMainDocumentPart().addObject(createParagraph("")); // blank line
                    }
                }

                String readableText = extractReadableText(contentJsonString);
                wordMLPackage.getMainDocumentPart().addObject(createMultilineParagraph(readableText));
                wordMLPackage.getMainDocumentPart().addObject(createParagraph(patreonUrl));
                wordMLPackage.getMainDocumentPart().addObject(createParagraph("")); // blank line
            }

            wordMLPackage.save(new File(docxFilePath));
        }
    }

    /**
     * <h3>Resolve final DOCX file name</h3>
     * Builds the final DOCX file name from a completed Excel batch file name.
     * <p>
     * The configured Excel pattern is matched against the Excel file name. When
     * a match exists, group {@code 1} replaces {@code temp} in the configured
     * temporary DOCX file name.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Compiles and applies the configured Excel file-name regular expression.</li>
     *     <li>Uses the first captured group as the DOCX batch suffix.</li>
     *     <li>Returns {@code _tmp} when the Excel file name does not match.</li>
     * </ul>
     *
     * @param excelFileName finalized Excel file name
     * @param docxFileNamePattern regular expression expected to capture the batch suffix in group {@code 1}
     * @param tempDocxFileName temporary DOCX file name template containing {@code temp}
     *
     * @return resolved DOCX file name, or {@code _tmp} when the pattern does not match
     * @since 1.0
     */
    private String getFinalDocxFileName(String excelFileName, String docxFileNamePattern, String tempDocxFileName)
    {
        Pattern pattern = Pattern.compile(docxFileNamePattern);
        Matcher matcher = pattern.matcher(excelFileName);

        if (matcher.matches()) {
            String result = matcher.group(1);
            tempDocxFileName = tempDocxFileName.replace("temp", result);
            return tempDocxFileName;
        }

        return "_tmp";
    }

    /**
     * <h3>Build Excel column map</h3>
     * Creates a lookup from header text to column index for the current Excel
     * sheet.
     *
     * @param headerRow header row from the Excel sheet
     * @param formatter formatter used to read cell text consistently
     *
     * @return map of non-blank column names to zero-based column indexes
     * @since 1.0
     */
    private Map<String, Integer> buildColumnMap(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> map = new HashMap<>();
        for (Cell cell : headerRow) {
            String name = formatter.formatCellValue(cell).trim();
            if (!name.isBlank()) {
                map.put(name, cell.getColumnIndex());
            }
        }
        return map;
    }

    /**
     * <h3>Read Excel cell text</h3>
     * Reads a trimmed string value from the requested column in the supplied row.
     * <p>
     * Missing columns and missing cells are treated as empty strings so optional
     * DOCX fields can be omitted without failing document generation.
     * </p>
     *
     * @param row row containing post data
     * @param columnMap header-name to column-index lookup
     * @param columnName column name to read
     * @param formatter formatter used to convert cell values to text
     *
     * @return trimmed cell text, or an empty string when the value is unavailable
     * @since 1.0
     */
    private String getCellValue(Row row, Map<String, Integer> columnMap, String columnName, DataFormatter formatter) {
        Integer colIndex = columnMap.get(columnName);
        if (colIndex == null) {
            return "";
        }
        Cell cell = row.getCell(colIndex);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    /**
     * <h3>Split publication timestamp</h3>
     * Splits a Patreon {@code published_at} timestamp into date and time parts.
     * <p>
     * The preferred path parses the timestamp as an {@link OffsetDateTime}. When
     * parsing fails, the method falls back to splitting on {@code T}, then to
     * preserving the original value as the date.
     * </p>
     *
     * @param publishedAt Patreon publication timestamp text
     *
     * @return date and time parts used for DOCX headings
     * @since 1.0
     */
    private DateParts splitPublishedAt(String publishedAt) {
        DateParts result = new DateParts();

        try {
            OffsetDateTime odt = OffsetDateTime.parse(publishedAt);
            result.setDate(odt.toLocalDate().toString());
            result.setTime(odt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        } catch (Exception e) {
            // Keep malformed timestamps visible in the report instead of dropping the row.
            if (publishedAt != null && publishedAt.contains("T")) {
                String[] parts = publishedAt.split("T", 2);
                result.setDate(parts[0]);

                String timePart = parts[1];
                result.setTime(timePart.length() >= 8 ? timePart.substring(0, 8) : timePart);
            } else {
                result.setDate(publishedAt == null ? "" : publishedAt);
                result.setTime("");
            }
        }

        return result;
    }

    /**
     * <h3>Create styled paragraph</h3>
     * Creates a WordprocessingML paragraph with a named Word style.
     *
     * @param text paragraph text; {@code null} is rendered as an empty string
     * @param styleName Word style name such as {@code Heading2}, {@code Heading3}, or {@code Heading4}
     *
     * @return styled WordprocessingML paragraph
     * @since 1.0
     */
    private P createStyledParagraph(String text, String styleName) {
        P p = WML_OBJECT_FACTORY.createP();

        PPr pPr = WML_OBJECT_FACTORY.createPPr();
        PPrBase.PStyle pStyle = WML_OBJECT_FACTORY.createPPrBasePStyle();
        pStyle.setVal(styleName);
        pPr.setPStyle(pStyle);
        p.setPPr(pPr);

        R run = WML_OBJECT_FACTORY.createR();
        Text t = WML_OBJECT_FACTORY.createText();
        t.setValue(text == null ? "" : text);
        run.getContent().add(t);

        p.getContent().add(run);
        return p;
    }

    /**
     * <h3>Create plain paragraph</h3>
     * Creates a plain WordprocessingML paragraph containing one text run.
     *
     * @param text paragraph text; {@code null} is rendered as an empty string
     *
     * @return plain WordprocessingML paragraph
     * @since 1.0
     */
    private P createParagraph(String text) {
        P p = WML_OBJECT_FACTORY.createP();
        R run = WML_OBJECT_FACTORY.createR();
        Text t = WML_OBJECT_FACTORY.createText();
        t.setValue(text == null ? "" : text);
        run.getContent().add(t);
        p.getContent().add(run);
        return p;
    }

    /**
     * <h3>Create multiline paragraph</h3>
     * Creates a WordprocessingML paragraph that preserves line breaks from the
     * supplied text.
     *
     * @param text multiline text extracted from Patreon content JSON
     *
     * @return paragraph containing text runs separated by Word line breaks
     * @since 1.0
     */
    private P createMultilineParagraph(String text) {
        P p = WML_OBJECT_FACTORY.createP();

        if (text == null || text.isBlank()) {
            p.getContent().add(WML_OBJECT_FACTORY.createR());
            return p;
        }

        String[] lines = text.split("\\R", -1);

        for (int i = 0; i < lines.length; i++) {
            R run = WML_OBJECT_FACTORY.createR();
            Text t = WML_OBJECT_FACTORY.createText();
            t.setValue(lines[i]);
            run.getContent().add(t);
            p.getContent().add(run);

            if (i < lines.length - 1) {
                // WordprocessingML requires an explicit break element to preserve line boundaries inside one paragraph.
                R breakRun = WML_OBJECT_FACTORY.createR();
                Br br = WML_OBJECT_FACTORY.createBr();
                breakRun.getContent().add(br);
                p.getContent().add(breakRun);
            }
        }

        return p;
    }

    /**
     * <h3>Download image bytes</h3>
     * Downloads a remote image URL for embedding in the DOCX report.
     *
     * @param client HTTP client used for the request
     * @param imageUrl image URL read from the Excel workbook
     *
     * @return downloaded image bytes
     * @throws Exception when the request fails or returns a non-success HTTP status
     * @since 1.0
     */
    private byte[] downloadImage(HttpClient client, String imageUrl) throws Exception {
//        URI uri = URI.create(imageUrl.trim());
//        HttpRequest request = HttpRequest.newBuilder(uri)
//                .timeout(REQUEST_TIMEOUT)
//                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
//                .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
//                .header("Accept-Language", "en-US,en;q=0.9")
//                .GET()
//                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .GET()
                .header("User-Agent", "Mozilla/5.0")
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Image download failed. HTTP status: " + response.statusCode());
        }

        return response.body();
    }

    /**
     * <h3>Create image paragraph</h3>
     * Embeds image bytes in the WordprocessingML package and returns a paragraph
     * containing the drawing.
     *
     * @param wordMLPackage target WordprocessingML package that owns the image part
     * @param bytes image bytes to embed
     * @param id base drawing id used by docx4j
     * @param filenameHint filename hint passed to docx4j for the image inline
     * @param widthPx requested display width in pixels
     * @param heightPx requested display height in pixels
     *
     * @return paragraph containing the embedded image drawing
     * @throws Exception when docx4j cannot create the image part or inline drawing
     * @since 1.0
     */
    private P createImageParagraph(
            WordprocessingMLPackage wordMLPackage,
            byte[] bytes,
            int id,
            String filenameHint,
            int widthPx,
            int heightPx
    ) throws Exception {

        BinaryPartAbstractImage imagePart = BinaryPartAbstractImage.createImagePart(wordMLPackage, bytes);

        long cx = pxToEmu(widthPx);
        long cy = pxToEmu(heightPx);

        Inline inline = imagePart.createImageInline(filenameHint, filenameHint, id, id + 1, cx, cy, false);

        Drawing drawing = WML_OBJECT_FACTORY.createDrawing();
        drawing.getAnchorOrInline().add(inline);

        R run = WML_OBJECT_FACTORY.createR();
        run.getContent().add(drawing);

        P paragraph = WML_OBJECT_FACTORY.createP();
        paragraph.getContent().add(run);

        return paragraph;
    }

    /**
     * <h3>Convert pixels to EMU</h3>
     * Converts a pixel measurement into English Metric Units used by Word
     * drawing dimensions.
     *
     * @param px pixel measurement
     *
     * @return equivalent EMU measurement
     * @since 1.0
     */
    private long pxToEmu(int px) {
        return Math.round(px * 9525L);
    }

    /**
     * <h3>Extract readable content text</h3>
     * Extracts human-readable text from Patreon {@code content_json_string}.
     * <p>
     * When JSON parsing succeeds, all nested text nodes are appended into a
     * readable string. When parsing fails, the original content string is
     * returned so report generation can continue.
     * </p>
     *
     * @param contentJsonString serialized Patreon content JSON from the Excel row
     *
     * @return readable text extracted from JSON, original content on parse failure, or an empty string when input is blank
     * @since 1.0
     */
    private String extractReadableText(String contentJsonString) {
        if (contentJsonString == null || contentJsonString.isBlank()) {
            return "";
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(contentJsonString);

            StringBuilder sb = new StringBuilder();
            appendTextNodes(root, sb);

            return sb.toString().trim();
        } catch (Exception e) {
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()), e);
            return contentJsonString;
        }
    }

    /**
     * <h3>Append JSON text nodes</h3>
     * Recursively traverses a Patreon content JSON tree and appends text values
     * to the supplied builder.
     * <p>
     * Paragraph nodes add blank-line separation before their text when previous
     * text already exists.
     * </p>
     *
     * @param node current JSON node being traversed
     * @param sb output builder receiving readable text
     *
     * @since 1.0
     */
    private void appendTextNodes(JsonNode node, StringBuilder sb) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }

        if (node.isObject()) {
            String type = node.path("type").asText("");

            if ("paragraph".equals(type) && sb.length() > 0 && !sb.toString().endsWith(System.lineSeparator() + System.lineSeparator())) {
                // Separate Patreon paragraph blocks so DOCX text remains readable after JSON flattening.
                sb.append(System.lineSeparator()).append(System.lineSeparator());
            }

            String text = node.path("text").asText("");
            if (!text.isBlank()) {
                sb.append(text);
            }

            node.properties().forEach(entry -> appendTextNodes(entry.getValue(), sb));

        } else if (node.isArray()) {
            for (JsonNode child : node) {
                appendTextNodes(child, sb);
            }
        }
    }

}

/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 2:49 PM
 */
package com.slmora.patreonpostautodownloader.service;

import com.slmora.patreonpostautodownloader.model.DateParts;
import com.slmora.patreonpostautodownloader.model.DownloadStatus;
import com.slmora.patreonpostautodownloader.model.ExcelJob;
import com.slmora.patreonpostautodownloader.model.ImageRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.jaxb.Context;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * The {@code DocxService} Class created for
 * <h4>Key Features</h4>
 * <ul>
 *      <li>...</li>
 * </ul>
 * <h4>Codes</h4>
 * 1 - {@link }<br>
 * <h4>Methods</h4>
 * <ul>
 *      <li>{@link }</li>
 * </ul>
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>....</li>
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
    private static final ObjectFactory WML_OBJECT_FACTORY = Context.getWmlObjectFactory();

    public void createDocx(ExcelJob job, Path outputDir, String docxFileNamePattern, String docxFileName) throws Exception {
        Files.createDirectories(outputDir);

        Path docxFile = outputDir.resolve(getFinalDocxFileName(job.getExcelFile().getFileName().toString(), docxFileNamePattern, docxFileName));

        writeWordFromExcel(job.getExcelFile().toString(), docxFile.toString());

//        try (XWPFDocument document = new XWPFDocument();
//             OutputStream os = Files.newOutputStream(docxFile)) {
//
//            XWPFParagraph title = document.createParagraph();
//            XWPFRun titleRun = title.createRun();
//            titleRun.setBold(true);
//            titleRun.setFontSize(16);
//            titleRun.setText("Generated DOCX for Job " + job.getJobId());
//
//            for (ImageRecord record : job.getImageRecords()) {
//                if (record.getDownloadStatus() != DownloadStatus.SUCCESS) {
//                    continue;
//                }
//
//                Path imageFile = record.getDownloadedImagePath();
//
//                if (imageFile == null || !Files.exists(imageFile)) {
//                    continue;
//                }
//
//                XWPFParagraph paragraph = document.createParagraph();
//                XWPFRun run = paragraph.createRun();
//
//                run.setText("Row: " + record.getRowNumber());
//                run.addBreak();
//
//                try (InputStream imageStream = Files.newInputStream(imageFile)) {
//                    run.addPicture(
//                            imageStream,
//                            Document.PICTURE_TYPE_JPEG,
//                            imageFile.getFileName().toString(),
//                            300 * 9525,
//                            200 * 9525
//                    );
//                }
//
//                run.addBreak();
//            }
//
//            document.write(os);
//        }
    }

    private void writeWordFromExcel(String excelFile, String outputWord) throws Exception {
        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {

            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();

            Sheet sheet = workbook.getSheetAt(0);
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
                    imageUrl = getCellValue(row, columnMap, "large_url", formatter);
                }

                DateParts dateParts = splitPublishedAt(publishedAt);

                if (!Objects.equals(currentDate, dateParts.getDate())) {
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

            wordMLPackage.save(new File(outputWord));
        }
    }

    private String getFinalDocxFileName(String excelFileName, String docxFileNamePattern, String docxFileName)
    {
        Pattern pattern = Pattern.compile(docxFileNamePattern);
        Matcher matcher = pattern.matcher(excelFileName);

        if (matcher.matches()) {
            String result = matcher.group(1);
            docxFileName = docxFileName.replace("temp", result);
            return docxFileName;
        }

        return "_tmp";
    }

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

    private String getCellValue(Row row, Map<String, Integer> columnMap, String columnName, DataFormatter formatter) {
        Integer colIndex = columnMap.get(columnName);
        if (colIndex == null) {
            return "";
        }
        Cell cell = row.getCell(colIndex);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private DateParts splitPublishedAt(String publishedAt) {
        DateParts result = new DateParts();

        try {
            OffsetDateTime odt = OffsetDateTime.parse(publishedAt);
            result.setDate(odt.toLocalDate().toString());
            result.setTime(odt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        } catch (Exception e) {
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

    private P createParagraph(String text) {
        P p = WML_OBJECT_FACTORY.createP();
        R run = WML_OBJECT_FACTORY.createR();
        Text t = WML_OBJECT_FACTORY.createText();
        t.setValue(text == null ? "" : text);
        run.getContent().add(t);
        p.getContent().add(run);
        return p;
    }

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
                R breakRun = WML_OBJECT_FACTORY.createR();
                Br br = WML_OBJECT_FACTORY.createBr();
                breakRun.getContent().add(br);
                p.getContent().add(breakRun);
            }
        }

        return p;
    }

    private byte[] downloadImage(HttpClient client, String imageUrl) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .GET()
                .header("User-Agent", "Mozilla/5.0")
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("Image download failed. HTTP status: " + response.statusCode());
        }

        return response.body();
    }

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

    private long pxToEmu(int px) {
        return Math.round(px * 9525L);
    }

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
            return contentJsonString;
        }
    }

//    private static void appendTextNodes(JsonNode node, StringBuilder sb) {
//        if (node == null || node.isMissingNode() || node.isNull()) {
//            return;
//        }
//
//        if (node.isObject()) {
//            String type = node.path("type").asText("");
//
//            if ("paragraph".equals(type) && sb.length() > 0) {
//                sb.append(System.lineSeparator()).append(System.lineSeparator());
//            }
//
//            if (node.has("text")) {
//                sb.append(node.path("text").asText(""));
//            }
//
//            node.properties().forEach((key, value) -> appendTextNodes(value, sb));
//
//        } else if (node.isArray()) {
//            for (JsonNode child : node) {
//                appendTextNodes(child, sb);
//            }
//        }
//    }

    private void appendTextNodes(JsonNode node, StringBuilder sb) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }

        if (node.isObject()) {
            String type = node.path("type").asText("");

            if ("paragraph".equals(type) && sb.length() > 0 && !sb.toString().endsWith(System.lineSeparator() + System.lineSeparator())) {
                sb.append(System.lineSeparator()).append(System.lineSeparator());
            }

            String text = node.path("text").asText("");
            if (!text.isBlank()) {
                sb.append(text);
            }

//            node.properties().forEach((key, value) -> appendTextNodes(value, sb));
            node.properties().forEach(entry -> appendTextNodes(entry.getValue(), sb));

        } else if (node.isArray()) {
            for (JsonNode child : node) {
                appendTextNodes(child, sb);
            }
        }
    }
}

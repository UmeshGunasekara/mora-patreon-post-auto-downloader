/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 2:48 PM
 */
package com.slmora.patreonpostautodownloader.service;

import com.slmora.common.logging.MoraLogger;
import com.slmora.common.logging.MoraLoggerThreadInfo;
import com.slmora.patreonpostautodownloader.model.DateParts;
import com.slmora.patreonpostautodownloader.model.ImageRecord;
import com.slmora.patreonpostautodownloader.model.PostRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@code ExcelService} class is created for writing Patreon post records to
 * Excel workbooks and reading image download records back from those workbooks.
 * <p>
 * The Excel producer uses this service to append paginated {@link PostRecord}
 * values into batch workbooks. The image download worker later uses the same
 * workbook column contract to extract {@link ImageRecord} values from the
 * {@code large_url} column.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Creates new Excel workbooks with the pipeline post column structure.</li>
 *     <li>Appends additional post rows to existing workbooks during batch pagination.</li>
 *     <li>Reads image URLs from Excel rows and turns them into image download records.</li>
 *     <li>Builds deterministic image names from publication date, time, post id, title, and image index.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link PostRecord}<br>
 * 2 - {@link ImageRecord}<br>
 * 3 - {@link DateParts}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link ExcelService#createExcelFromRecords(List, Path, String)}</li>
 *     <li>{@link ExcelService#readImageRecords(Path, String)}</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>The Excel column names are part of the pipeline contract and are consumed by image download and DOCX generation.</li>
 *     <li>Pipe-separated image URLs in {@code large_url} are split into separate {@link ImageRecord} entries.</li>
 *     <li>Missing optional columns or cells are read as empty strings during image extraction.</li>
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
public class ExcelService
{
    /**
     * Class-scoped logger used for workbook creation, workbook reading, and
     * image-record extraction diagnostics.
     */
    private final static MoraLogger LOGGER = MoraLogger.getLogger(ExcelService.class);

    /**
     * <h3>Create Excel workbook from post records</h3>
     * Writes Patreon post records into the target Excel workbook.
     * <p>
     * If the workbook already exists, rows are appended to the configured sheet.
     * If it does not exist, a new workbook and header row are created.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Delegates workbook creation and append behavior to the internal writer.</li>
     *     <li>Preserves the configured sheet name used by downstream readers.</li>
     *     <li>Writes records using stable column names expected by image and DOCX processing.</li>
     * </ul>
     *
     * @param patreonPosts post records parsed from Patreon API responses
     * @param excelOutputFilePath workbook path to create or append
     * @param excelSheetName sheet name used for post rows
     *
     * @throws IOException when the workbook cannot be created, read, or written
     * @since 1.0
     */
    public void createExcelFromRecords(List<PostRecord> patreonPosts, Path excelOutputFilePath, String excelSheetName) throws IOException
    {
        writePostsToExcel(patreonPosts, excelOutputFilePath, excelSheetName);
    }

    /**
     * <h3>Read image records from Excel</h3>
     * Reads image download records from the configured workbook sheet.
     * <p>
     * Each non-blank image URL in the {@code large_url} column becomes one
     * {@link ImageRecord}. Pipe-separated image URLs are split into multiple
     * image records for the same Excel row.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Delegates workbook parsing to the internal image-record reader.</li>
     *     <li>Uses the sheet header row to locate required columns by name.</li>
     *     <li>Builds image names from row metadata for deterministic file naming.</li>
     * </ul>
     *
     * @param excelFile source workbook path
     * @param sheetName sheet name containing post rows
     *
     * @return image records extracted from the workbook
     * @throws Exception when workbook reading or row parsing fails
     * @since 1.0
     */
    public List<ImageRecord> readImageRecords(Path excelFile, String sheetName) throws Exception {
        return readImageRecordsFromExcel(excelFile, sheetName);
    }

    /**
     * <h3>Write post rows to Excel</h3>
     * Creates or appends a workbook sheet using the pipeline's stable post
     * columns.
     * <p>
     * The method opens an existing workbook when available and appends after the
     * last row. Otherwise it creates a new workbook, initializes headers, and
     * writes from the first data row.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Creates the canonical post header row when the workbook or sheet is new.</li>
     *     <li>Writes all supplied {@link PostRecord} values into fixed column positions.</li>
     *     <li>Applies wrapping styles and an auto-filter over the populated range.</li>
     *     <li>Closes the workbook in a {@code finally} block after writing.</li>
     * </ul>
     *
     * @param patreonPosts post records to write
     * @param excelOutputFilePath workbook path to create or append
     * @param excelSheetName sheet name to write
     *
     * @throws IOException when workbook I/O fails
     * @since 1.0
     */
    private void writePostsToExcel(List<PostRecord> patreonPosts, Path excelOutputFilePath, String excelSheetName) throws IOException
    {
        Workbook workbook = null;
        Sheet sheet;
        int rowIndex;

        String[] headers = {
                "id",
                "published_at",
                "title",
                "cleaned_teaser_text",
                "content_json_string",
                "comment_count",
                "patreon_url",
                "large_url",
                "thumb_url"
        };

        File excelFile = excelOutputFilePath.toFile();

        try {
            if (excelFile.exists()) {
                // Append to the active batch workbook so paginated Patreon responses stay in one Excel job until finalization.
                LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),
                        "Excel file exists in {}",excelOutputFilePath.toString());

                try (FileInputStream fis = new FileInputStream(excelFile)) {
                    workbook = WorkbookFactory.create(fis);
                    sheet = workbook.getSheet(excelSheetName);
                    if (sheet == null) {
                        sheet = workbook.createSheet(excelSheetName);
                        initiateHeaderForExcelSheet(workbook, sheet, headers);
                    }
                    rowIndex = sheet.getLastRowNum() + 1;
                }

            } else {
                // Create the canonical workbook structure expected by image extraction and DOCX generation.
                LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),
                        "Excel file not exists in {}",excelOutputFilePath.toString());

                workbook = new XSSFWorkbook();
                sheet = workbook.createSheet(excelSheetName);
                initiateHeaderForExcelSheet(workbook, sheet, headers);
                rowIndex = 1;
            }

            setColumnWithForSheet(sheet);
            CellStyle wrapStyle = createWrapStyle(workbook);

            for (PostRecord post : patreonPosts) {
                Row row = sheet.createRow(rowIndex++);

                createCell(row, 0, post.getId(), wrapStyle);
                createCell(row, 1, post.getPublishedAt(), wrapStyle);
                createCell(row, 2, post.getTitle(), wrapStyle);
                createCell(row, 3, post.getCleanedTeaserText(), wrapStyle);
                createCell(row, 4, post.getContentJsonString(), wrapStyle);

                Cell commentCell = row.createCell(5);
                commentCell.setCellValue(post.getCommentCount());
                commentCell.setCellStyle(wrapStyle);

                createCell(row, 6, post.getPatreonUrl(), wrapStyle);
                createCell(row, 7, post.getLargeUrl(), wrapStyle);
                createCell(row, 8, post.getThumbUrl(), wrapStyle);
            }

            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                    0, Math.max(1, rowIndex - 1), 0, headers.length - 1));

            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
                LOGGER.info(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),
                        "Excel file updated in {}",excelOutputFilePath.toString());
            }
        }finally {
            if(workbook != null){
                workbook.close();
            }
        }
    }

    /**
     * <h3>Set sheet column widths</h3>
     * Applies fixed widths for the known Patreon post columns.
     *
     * @param sheet sheet whose columns should be sized
     * @since 1.0
     */
    private static void setColumnWithForSheet(Sheet sheet)
    {
        sheet.setColumnWidth(0, 5000);  // id
        sheet.setColumnWidth(1, 7000);  // published_at
        sheet.setColumnWidth(2, 8000);  // title
        sheet.setColumnWidth(3, 15000); // cleaned_teaser_text
        sheet.setColumnWidth(4, 25000); // content_json_string
        sheet.setColumnWidth(5, 5000);  // comment_count
        sheet.setColumnWidth(6, 15000); // patreon_url
        sheet.setColumnWidth(7, 15000); // large_url
        sheet.setColumnWidth(8, 15000); // sheet.setColumnWidth(7, 15000); // large_url
    }

    /**
     * <h3>Initialize Excel header row</h3>
     * Writes the configured column names into row {@code 0} with header styling.
     *
     * @param workbook workbook used to create the header style
     * @param sheet sheet that receives the header row
     * @param headers ordered column names to write
     *
     * @since 1.0
     */
    private void initiateHeaderForExcelSheet(Workbook workbook, Sheet sheet, String[] headers)
    {
        CellStyle headerStyle = createHeaderStyle(workbook);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    /**
     * <h3>Create header style</h3>
     * Creates the bold, centered, wrapped style used for workbook headers.
     *
     * @param workbook workbook that owns the created style
     *
     * @return header cell style
     * @since 1.0
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    /**
     * <h3>Create wrapped cell style</h3>
     * Creates the wrapped top-aligned style used for post data cells.
     *
     * @param workbook workbook that owns the created style
     *
     * @return wrapped data cell style
     * @since 1.0
     */
    private CellStyle createWrapStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        return style;
    }

    /**
     * <h3>Create text cell</h3>
     * Writes a string value into a row cell and applies the supplied style.
     *
     * @param row row that receives the cell
     * @param columnIndex zero-based column index
     * @param value text value to write
     * @param style style to apply to the cell
     *
     * @since 1.0
     */
    private void createCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /**
     * <h3>Read image records from workbook</h3>
     * Parses the source workbook and creates image download records from the
     * {@code large_url} column.
     * <p>
     * The method uses row metadata to create deterministic image names. Multiple
     * image URLs in the same cell are supported when separated by {@code |}.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Opens the workbook with Apache POI and locates the configured sheet.</li>
     *     <li>Requires a header row and builds a column-name lookup.</li>
     *     <li>Reads post id, publication timestamp, title, and large image URL from each row.</li>
     *     <li>Creates one {@link ImageRecord} per non-blank image URL.</li>
     * </ul>
     *
     * @param excelFilePath source workbook path
     * @param excelSheetName sheet name containing post rows
     *
     * @return image download records extracted from the workbook
     * @throws IllegalStateException when the header row is missing
     * @throws RuntimeException when workbook reading fails
     * @since 1.0
     */
    private List<ImageRecord> readImageRecordsFromExcel(Path excelFilePath, String excelSheetName) {
        try (InputStream inputStream = Files.newInputStream(excelFilePath);
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            List<ImageRecord> items = new ArrayList<>();

            Sheet sheet = workbook.getSheet(excelSheetName);
            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                throw new IllegalStateException("Excel header row is missing.");
            }

            Map<String, Integer> columnMap = buildColumnMap(headerRow, formatter);

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String id = getCellValue(row, columnMap, "id", formatter);
                String publishedAt = getCellValue(row, columnMap, "published_at", formatter);
                String title = getCellValue(row, columnMap, "title", formatter);
                String imageUrl = getCellValue(row, columnMap, "large_url", formatter);

                DateParts dateParts = splitPublishedAt(publishedAt);
                String time = dateParts.getTime().replace(":", "-");

                LOGGER.debug(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                                Thread.currentThread().threadId(),
                                Thread.currentThread().getStackTrace()),
                        "Excel raw information \n\tid : {} \n\tpublished_at : {} \n\ttitle : {} \n\tlarge_url : {} \n\tdateParts : {} \n\ttime : {}",
                        id,publishedAt,title,imageUrl,dateParts.getDate(),time);

                if (!imageUrl.isBlank()) {
                    List<String> images = new ArrayList<>();
                    if(imageUrl.contains("|")){
                        // Patreon rows may contain multiple image URLs joined by '|'; each URL becomes its own download record.
                        for(String image: imageUrl.split("\\|")){
                            String trimmedImage = image.trim();
                            if (!trimmedImage.isBlank()) {
                                images.add(trimmedImage);
                            }
                        }
                    }else {
                        images.add(imageUrl);
                    }
                    int index = 1;
                    for(String image : images) {
                        // Include row metadata in the image name so downloaded files can be traced back to the source post.
                        String fileName = "D"+dateParts.getDate() + "-T" + time + "-" + id+ "-" +normalizeTitle(title)+"-"+String.format("%02d", index++);
                        items.add(new ImageRecord(r, image, fileName));
                    }
                }
            }

            return items;
        } catch (IOException e) {
            LOGGER.error(new MoraLoggerThreadInfo(Thread.currentThread().getName(),
                            Thread.currentThread().threadId(),
                            Thread.currentThread().getStackTrace()), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * <h3>Build Excel column map</h3>
     * Creates a lookup from header text to column index for the current sheet.
     *
     * @param headerRow header row from the Excel sheet
     * @param formatter formatter used to read cell text consistently
     *
     * @return map of non-blank column names to zero-based column indexes
     * @since 1.0
     */
    private Map<String, Integer> buildColumnMap(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> columnMap = new HashMap<>();
        for (Cell cell : headerRow) {
            String name = formatter.formatCellValue(cell).trim();
            if (!name.isBlank()) {
                columnMap.put(name, cell.getColumnIndex());
            }
        }
        return columnMap;
    }

    /**
     * <h3>Read Excel cell text</h3>
     * Returns a trimmed string value for the requested column in the given row.
     *
     * <p>If the column does not exist in the header mapping or the cell is missing,
     * an empty string is returned.</p>
     *
     * @param row the Excel row containing data
     * @param columnMap mapping of column names to column indexes
     * @param columnName the target column name to read
     * @param formatter the formatter used to convert cell content to text
     * @return trimmed cell text, or an empty string when unavailable
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
     * The preferred path parses an ISO offset timestamp. If parsing fails, the
     * method falls back to splitting on {@code T}, and finally keeps the original
     * value as the date with an empty time.
     * </p>
     *
     * @param publishedAt publication timestamp from the Excel row
     *
     * @return date and time parts used for image file naming
     * @since 1.0
     */
    private DateParts splitPublishedAt(String publishedAt) {
        DateParts result = new DateParts();

        try {
            OffsetDateTime odt = OffsetDateTime.parse(publishedAt);
            result.setDate(odt.toLocalDate().toString());
            result.setTime(odt.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        } catch (Exception e) {
            // Preserve malformed timestamps in generated names instead of dropping the image row.
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
     * <h3>Normalize post title for file name</h3>
     * Converts a post title into a file-name-friendly uppercase token.
     * <p>
     * Only alphanumeric characters and spaces are retained before spaces are
     * collapsed into hyphen separators.
     * </p>
     *
     * @param text source post title
     *
     * @return normalized title token, or an empty string when the input is blank
     * @since 1.0
     */
    private String normalizeTitle(String text) {

        if (text == null || text.isBlank()) {
            return "";
        }

        // Keep generated image file names portable by stripping punctuation and path-sensitive characters.
        String cleaned = text.replaceAll("[^a-zA-Z0-9 ]", "");

        // Uppercase titles make downloaded image names easier to scan in output folders.
        cleaned = cleaned.toUpperCase();

        // Collapse repeated spaces into one hyphen-separated title token.
        return cleaned.trim().replaceAll("\\s+", "-");
    }

}

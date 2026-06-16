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
 * The {@code ExcelService} Class created for
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
public class ExcelService
{
    private final static MoraLogger LOGGER = MoraLogger.getLogger(ExcelService.class);

    public void createExcelFromRecords(List<PostRecord> patreonPosts, Path excelOutputFilePath, String excelSheetName) throws IOException
    {
        writePostsToExcel(patreonPosts, excelOutputFilePath, excelSheetName);
    }

    public List<ImageRecord> readImageRecords(Path excelFile, String sheetName) throws Exception {
        return readImageRecordsFromExcel(excelFile, sheetName);
    }

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

    private CellStyle createWrapStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        return style;
    }

    private void createCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

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
     */
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

    private String normalizeTitle(String text) {

        if (text == null || text.isBlank()) {
            return "";
        }

        // remove special characters except spaces
        String cleaned = text.replaceAll("[^a-zA-Z0-9 ]", "");

        // convert to upper case
        cleaned = cleaned.toUpperCase();

        // replace spaces with "-"
        return cleaned.trim().replaceAll("\\s+", "-");
    }

}

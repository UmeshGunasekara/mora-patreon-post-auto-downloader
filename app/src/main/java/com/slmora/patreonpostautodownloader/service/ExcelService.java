/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 2:48 PM
 */
package com.slmora.patreonpostautodownloader.service;

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
import java.io.OutputStream;
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
    public void createExcelFromRecords(List<PostRecord> posts, Path outputFilePath, String sheetName) throws IOException
    {

        Files.createDirectories(outputFilePath.getParent());

        writePostsToExcel(posts, outputFilePath, sheetName);
    }

    public List<ImageRecord> readImageRecords(Path excelFile, String sheetName) throws Exception {
        return readItemsFromExcel(excelFile, sheetName);
    }



    private void writePostsToExcel(List<PostRecord> posts, Path outputFilePath, String sheetName) throws IOException
    {

        Workbook workbook;
        Sheet sheet;
        int rowIndex;

        String[] headers = {
//                    "source_url",
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

        File file = outputFilePath.toFile();

        if (file.exists()) {
            FileInputStream fis = new FileInputStream(file);
            workbook = WorkbookFactory.create(fis);
            sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                sheet = workbook.createSheet(sheetName);
            }
            rowIndex = sheet.getLastRowNum() + 1;
            fis.close();

        }else{
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet(sheetName);

            CellStyle headerStyle = createHeaderStyle(workbook);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            rowIndex = 1;
        }


        CellStyle wrapStyle = createWrapStyle(workbook);

        for (PostRecord post : posts) {
            Row row = sheet.createRow(rowIndex++);

//                createCell(row, 0, post.getSourceUrl(), wrapStyle);
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

//            sheet.setColumnWidth(0, 18000); // source_url
        sheet.setColumnWidth(0, 5000);  // id
        sheet.setColumnWidth(1, 7000);  // published_at
        sheet.setColumnWidth(2, 8000);  // title
        sheet.setColumnWidth(3, 15000); // cleaned_teaser_text
        sheet.setColumnWidth(4, 25000); // content_json_string
        sheet.setColumnWidth(5, 5000);  // comment_count
        sheet.setColumnWidth(6, 15000); // patreon_url
        sheet.setColumnWidth(7, 15000); // large_url
        sheet.setColumnWidth(8, 15000); // sheet.setColumnWidth(7, 15000); // large_url

        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                0, Math.max(1, rowIndex - 1), 0, headers.length - 1));

        try (FileOutputStream fos = new FileOutputStream(file)) {
            workbook.write(fos);
        }finally {
            workbook.close();
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

    //===================


    private List<ImageRecord> readItemsFromExcel(Path xlsx, String sheetName) throws Exception {
        try (InputStream in = Files.newInputStream(xlsx);
             Workbook workbook = new XSSFWorkbook(in)) {

            List<ImageRecord> items = new ArrayList<>();

            Sheet sheet = workbook.getSheetAt(0);
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

                String time = dateParts.getTime().replaceAll(":", "-");

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
                        String fileName = dateParts.getDate() + "-T" + time + "-" + id+ "-" +normalizeTitle(title)+"-"+String.format("%02d", index++);
                        items.add(new ImageRecord(r, image, fileName));
                    }
                }
            }

            return items;
        }
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

    private String normalizeTitle(String text) {

//        if (text == null || text.isBlank()) {
//            return "";
//        }
//
//        // remove special characters except spaces
//        String cleaned = text.replaceAll("[^a-zA-Z0-9 ]", "");
//
//        String[] words = cleaned.trim().split("\\s+");
//
//        StringBuilder result = new StringBuilder();
//
//        for (String word : words) {
//
//            if (word.isEmpty()) {
//                continue;
//            }
//
//            String formatted =
//                    word.substring(0, 1).toUpperCase() +
//                            word.substring(1).toLowerCase();
//
//            if (result.length() > 0) {
//                result.append("-");
//            }
//
//            result.append(formatted);
//        }
//
//        return result.toString();

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

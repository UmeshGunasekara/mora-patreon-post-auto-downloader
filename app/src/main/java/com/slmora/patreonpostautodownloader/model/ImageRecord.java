/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/6/2026 2:18 PM
 */
package com.slmora.patreonpostautodownloader.model;

import lombok.Data;

import java.nio.file.Path;

/**
 * The {@code ImageRecord} class is created for carrying one image download item
 * extracted from a generated Patreon post Excel file.
 * <p>
 * Each record preserves the source Excel row, image URL, generated image file
 * name, download output path, status, and error message. Image download and
 * retry services mutate this DTO while processing an {@link ExcelJob}.
 * </p>
 *
 * <h4>Key Features</h4>
 * <ul>
 *     <li>Identifies the Excel row and image URL that produced a download task.</li>
 *     <li>Tracks the generated image name used when writing files to the image output directory.</li>
 *     <li>Stores download status, downloaded path, and failure details for retry and persistence.</li>
 *     <li>Uses Lombok {@link Data} to provide standard accessors, mutators, {@code equals}, {@code hashCode}, and {@code toString} methods.</li>
 * </ul>
 *
 * <h4>Codes</h4>
 * 1 - {@link ExcelJob}<br>
 * 2 - {@link DownloadStatus}<br>
 *
 * <h4>Methods</h4>
 * <ul>
 *     <li>{@link ImageRecord#ImageRecord(int, String, String)}</li>
 *     <li>Lombok-generated getters and setters for mutable download fields</li>
 * </ul>
 *
 * <p>
 * <h4>Notes</h4>
 * <ul>
 *     <li>The identity fields are final because they come from the Excel source row and should remain stable during retries.</li>
 *     <li>The download status starts as {@link DownloadStatus#PENDING} until image processing updates it.</li>
 *     <li>This model does not validate URL format or file-system safety of the image name.</li>
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
@Data
public class ImageRecord
{
    /**
     * Zero-based or service-provided Excel row number used to trace this image
     * back to its source workbook row.
     */
    private final int rowNumber;

    /**
     * Source image URL extracted from the Excel row.
     */
    private final String imageUrl;

    /**
     * Generated image file name used by the image download service.
     */
    private final String imageName;

    /**
     * File-system path where the image was successfully downloaded.
     */
    private Path downloadedImagePath;

    /**
     * Current download state for this image record.
     */
    private DownloadStatus downloadStatus = DownloadStatus.PENDING;

    /**
     * Last error message captured while downloading or retrying this image.
     */
    private String errorMessage;

    /**
     * <h3>Create image download record</h3>
     * Creates a download item from one image entry found in an Excel workbook.
     * <p>
     * The constructor captures immutable source identity values. Runtime fields
     * such as downloaded path, status, and error message are updated later by the
     * image download and retry services.
     * </p>
     *
     * <p><b>Detailed Description:</b></p>
     * <ul>
     *     <li>Stores the Excel row number for diagnostics and failed-job reporting.</li>
     *     <li>Stores the image URL that will be requested by the downloader.</li>
     *     <li>Stores the target image name used to build the output path.</li>
     * </ul>
     *
     * @param rowNumber Excel row number associated with the image entry
     * @param imageUrl image URL extracted from the workbook
     * @param imageName generated image file name for the download output
     *
     * @since 1.0
     */
    public ImageRecord(int rowNumber, String imageUrl,  String imageName) {
        this.rowNumber = rowNumber;
        this.imageUrl = imageUrl;
        this.imageName = imageName;
    }
}

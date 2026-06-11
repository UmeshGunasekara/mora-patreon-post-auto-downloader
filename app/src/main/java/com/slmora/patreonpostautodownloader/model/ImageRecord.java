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
 * The {@code ImageRecord} Class created for
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
@Data
public class ImageRecord
{
    private final int rowNumber;
    private final String imageUrl;
    private final String imageName;

    private Path downloadedImagePath;
    private DownloadStatus downloadStatus = DownloadStatus.PENDING;
    private String errorMessage;

    public ImageRecord(int rowNumber, String imageUrl,  String imageName) {
        this.rowNumber = rowNumber;
        this.imageUrl = imageUrl;
        this.imageName = imageName;
    }
}

/*
 * Created by IntelliJ IDEA.
 * Language: Java
 * Property of Umesh Gunasekara
 * @Author: SLMORA
 * @DateTime: 6/9/2026 10:38 PM
 */
package com.slmora.patreonpostautodownloader.model;

import lombok.Data;

/**
 * The {@code PostRecord} Class created for
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
 * <br>1.0          6/9/2026      SLMORA                Initial Code
 * </pre></blockquote>
 */
@Data
public class PostRecord
{
    private String id;
    private String publishedAt;
    private String title;
    private String cleanedTeaserText;
    private String contentJsonString;
    private int commentCount;
    private String patreonUrl;
    private String largeUrl;
    private String thumbUrl;
}

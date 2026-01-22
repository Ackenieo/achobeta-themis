package com.achobeta.themis.api.review.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * <p>
 * 描述：
 * </p>
 *
 * @Author: ZGjie20
 * @version: 1.0.0
 */
@Data
@AllArgsConstructor
public class ReviewResult {
    private String id;
    private String filename;

    private String review;
}

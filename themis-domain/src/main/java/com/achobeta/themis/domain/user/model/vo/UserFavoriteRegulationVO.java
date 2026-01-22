package com.achobeta.themis.domain.user.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户收藏法律法规响应VO
 * 
 * @author Cascade AI
 * @date 2025/11/20
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserFavoriteRegulationVO {

    /**
     * 收藏ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 法规ID
     */
    private Integer regulationId;
    /**
     * 法规名称
     *
    * */
   private String regulationName;
    /**
     * 法律分类ID
     */
    private Long lawCategoryId;

    /**
     * 发布年月日
     */
    private String issueYear;

    /**
     * 条款号（第n条）
     */
    private Integer articleNumber;

    /**
     * 法条原文
     */
    private String originalText;

    /**
     * 收藏时间
     */
    private LocalDateTime createdTime;
}

package com.achobeta.themis.domain.laws.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户收藏法律法规关系表实体类
 * 
 * @author Cascade AI
 * @date 2025/11/20
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("user_favorite_regulations")
public class UserFavoriteRegulation {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID，关联 user.id
     */
    private Long userId;

    /**
     * 法规ID，关联 law_regulations.regulation_id
     */
    private Integer regulationId;

    /**
     * 收藏时间
     */
    private LocalDateTime createdTime;
}

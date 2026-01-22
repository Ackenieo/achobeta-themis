package com.achobeta.themis.domain.user.model.entity;

import com.achobeta.themis.common.annotation.FieldDesc;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author Aseubel
 * @date 2025/10/27 上午1:45
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {

    @FieldDesc(name = "用户ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @FieldDesc(name = "用户名")
    private String username;

    @FieldDesc(name = "密码")
    private String password;

    @FieldDesc(name = "手机号")
    private String phone;

    @FieldDesc(name = "六位数用户ID")
    private String userId;

    @FieldDesc(name = "用户类型")
    private Integer userType;

    @FieldDesc(name = "创建时间")
    private LocalDateTime createdTime;

    @FieldDesc(name = "更新时间")
    private LocalDateTime updatedTime;

}


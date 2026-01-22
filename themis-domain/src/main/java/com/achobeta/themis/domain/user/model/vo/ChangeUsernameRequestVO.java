package com.achobeta.themis.domain.user.model.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeUsernameRequestVO {
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "新用户名不能为空")
    private String newUsername;
}

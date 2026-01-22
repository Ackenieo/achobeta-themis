package com.achobeta.themis.domain.user.model.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequestVO {
     @NotNull(message = "用户ID不能为空")
     private Long userId;

     @NotBlank(message = "旧密码不能为空")
     private String oldPassword;

     @NotBlank(message = "密码不能为空")
     @Size(min = 6, message = "密码长度至少6个字符")
     private String newPassword;
}

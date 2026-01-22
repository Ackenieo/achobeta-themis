package com.achobeta.themis.domain.user.model;

import com.achobeta.themis.domain.user.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聚合，作为用户模块service的入参和出参
 * @author Aseubel
 * @date 2025/10/27 上午1:39
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserModel {
    private User user;
}

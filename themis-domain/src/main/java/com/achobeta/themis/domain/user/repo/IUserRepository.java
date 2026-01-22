package com.achobeta.themis.domain.user.repo;

import com.achobeta.themis.domain.user.model.entity.User;

/**
 * @author Aseubel
 * @date 2025/10/27 上午2:12
 */
public interface IUserRepository {

    User findUserByUserId(Long userId);

    User findUserByPhone(String phone);

    void save(User user);

    void update(User user);

}

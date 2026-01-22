package com.achobeta.themis.infrastructure.user.repo;

import com.achobeta.themis.domain.user.model.entity.User;
import com.achobeta.themis.domain.user.repo.IUserRepository;
import com.achobeta.themis.infrastructure.user.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * @author Aseubel
 * @date 2025/10/27 上午2:12
 */
@Repository
@RequiredArgsConstructor
public class UserRepository implements IUserRepository {

    private final UserMapper userMapper;

    @Override
    public User findUserByUserId(Long userId) {
        return userMapper.selectById(userId);
    }

    /**
     * 根据手机号查询用户
     * @param phone
     * @return
     */
    @Override
    public User findUserByPhone(String phone) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
    }

    /**
     * 保存用户
     * @param user
     */
    @Override
    public void save(User user) {
        userMapper.insert(user);
    }

    /**
     * 更新用户
     * @param user
     */
    @Override
    public void update(User user) {
        userMapper.updateById(user);
    }
}

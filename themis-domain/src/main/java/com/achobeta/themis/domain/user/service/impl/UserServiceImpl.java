package com.achobeta.themis.domain.user.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.achobeta.themis.common.exception.BusinessException;
import com.achobeta.themis.common.redis.service.IRedisService;
import com.achobeta.themis.common.redis.service.RedissonService;
import com.achobeta.themis.common.util.JwtUtil;
import com.achobeta.themis.domain.user.model.vo.*;
import com.achobeta.themis.domain.user.model.UserModel;
import com.achobeta.themis.domain.user.model.entity.User;
import com.achobeta.themis.domain.user.repo.IUserRepository;
import com.achobeta.themis.domain.user.service.IUserService;
import com.achobeta.themis.domain.user.service.IVerifyCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.achobeta.themis.common.Constant.SMS_CODE_EXPIRE_MINUTES;

/**
 * @author Aseubel
 * @date 2025/10/27 上午2:20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService{

    private final IUserRepository userRepository;

    private final JwtUtil jwtUtil;

    private final IRedisService redissonService;

    private final IVerifyCodeService verifyCodeService;

    private final BCryptPasswordEncoder bCryptEncoder = new BCryptPasswordEncoder(10);

    @Override
    public UserModel getUserByUserId(UserModel userModel) {
        log.info("根据用户ID获取用户信息，用户ID：{}", userModel.getUser().getId());
        User user = userRepository.findUserByUserId(userModel.getUser().getId());
        UserModel result = UserModel.builder()
                .user(user)
                .build();
        return result;
    }

    @Override
    public UserModel getUserInfo(Long userId) {
        log.info("根据用户ID获取用户信息，用户ID：{}", userId);
        if (ObjectUtil.isEmpty(userId)) {
            throw new BusinessException("用户ID不能为空");
        }
        User user = userRepository.findUserByUserId(userId);
        if (ObjectUtil.isEmpty(user)) {
            throw new BusinessException("用户不存在");
        }
        UserModel result = UserModel.builder()
                .user(user)
                .build();
        return result;
    }

    /**
     * 登录
     * @param request
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AuthResponseVO login(LoginRequestVO request) {
        log.info("用户登录，手机号：{}", request.getPhone());
        User user = userRepository.findUserByPhone(request.getPhone());
        if (ObjectUtil.isEmpty(user)) {
            // 注册用户 TODO(后期可优化：限制注册频率)
            String lockKey = "user_sign_up:" + request.getPhone();
            RLock lock = redissonService.getLock(lockKey);
            boolean isLocked = false;
            try {
                isLocked = lock.tryLock(3, 5, TimeUnit.SECONDS);
                if (isLocked) {
                    User existingUser = userRepository.findUserByPhone(request.getPhone());
                    if (existingUser != null) {
                        throw new BusinessException("该手机号已注册");
                    }
                    user = User.builder()
                            .username(UUID.randomUUID().toString().substring(0, 8))
                            .password(bCryptEncoder.encode(request.getPassword()))
                            .phone(request.getPhone())
                            .userType(request.getUserType())
                            .build();
                    userRepository.save(user);
                    user.setUserId(String.format("%06d", user.getId()));
                    userRepository.update(user);
                } else {
                    throw new BusinessException("注册请求过于频繁");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException("注册被中断，请重试");
            } finally {
                if (isLocked && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } else {
            // 校验密码
            if (!bCryptEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new BusinessException("密码错误");
            }
        }

        // 生成双token并存储
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), request.getClientId());
        String refreshTokenKey = "refresh_token:" + user.getId() + "-" + request.getClientId();
        String userFreshTokensKey = "refresh_token:" + user.getId();
        redissonService.setValue(refreshTokenKey, refreshToken, jwtUtil.getRefreshTokenExpiration());
        redissonService.addToMap(userFreshTokensKey, request.getClientId(), refreshToken);
        redissonService.setMapExpired(userFreshTokensKey, jwtUtil.getRefreshTokenExpiration() + Duration.ofMinutes(1).toMillis());

        return AuthResponseVO.builder()
                .id(user.getId())
                .userId(user.getId().toString())
                .username(user.getUsername())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * 刷新令牌
     * @param refreshToken
     * @return
     */
    @Override
    public AuthResponseVO refreshToken(String refreshToken) {
        log.info("刷新访问令牌，刷新令牌：{}", refreshToken);
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException("刷新令牌无效");
        }
        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw new BusinessException("刷新令牌无效");
        }
        String refreshTokenKey = "refresh_token:" + jwtUtil.getUserIdFromToken(refreshToken) + "-" + jwtUtil.getClientIdFromToken(refreshToken);
        String storedRefreshToken = redissonService.getValue(refreshTokenKey);
        if (ObjectUtil.isEmpty(storedRefreshToken)) {
            throw new BusinessException("刷新令牌无效");
        }
        if (!storedRefreshToken.equals(refreshToken)) {
            throw new BusinessException("刷新令牌无效");
        }
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        String username = jwtUtil.getUsernameFromToken(refreshToken);
        String accessToken = jwtUtil.generateAccessToken(userId, username);
        String newRefreshToken = jwtUtil.generateRefreshToken(userId, username, jwtUtil.getClientIdFromToken(refreshToken));

        redissonService.setValue(refreshTokenKey, newRefreshToken, jwtUtil.getRefreshTokenExpiration());
        String userFreshTokensKey = "refresh_token:" + userId;
        redissonService.addToMap(userFreshTokensKey, jwtUtil.getClientIdFromToken(refreshToken), newRefreshToken);
        redissonService.setMapExpired(userFreshTokensKey, jwtUtil.getRefreshTokenExpiration() + Duration.ofMinutes(5).toMillis());

        return AuthResponseVO.builder()
                .userId(userId.toString())
                .username(username)
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    /**
     * 登出
     * @param refreshToken
     */
    @Override
    public void logout(String refreshToken) {
        log.info("用户注销，刷新令牌：{}", refreshToken);
        // 校验刷新令牌是否有效
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException("刷新令牌无效");
        }
        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw new BusinessException("刷新令牌无效");
        }
        String refreshTokenKey = "refresh_token:" + jwtUtil.getUserIdFromToken(refreshToken) + "-" + jwtUtil.getClientIdFromToken(refreshToken);
        redissonService.remove(refreshTokenKey);
        String userFreshTokensKey = "refresh_token:" + jwtUtil.getUserIdFromToken(refreshToken);
        redissonService.removeFromMap(userFreshTokensKey, jwtUtil.getClientIdFromToken(refreshToken));
    }

    /**
     * 登出所有设备
     * @param userId
     */
    @Override
    public void logoutAll(Long userId) {
        log.info("用户登出所有设备，用户ID：{}", userId);
        User user = userRepository.findUserByUserId(userId);
        if (ObjectUtil.isEmpty(user)) {
            throw new BusinessException("用户不存在");
        }
        String userFreshTokensKey = "refresh_token:" + userId;
        redissonService.getMap(userFreshTokensKey).forEach((clientId, token) -> redissonService.remove(userFreshTokensKey + "-" + clientId));
        redissonService.remove(userFreshTokensKey);
    }

    /**
     * 修改密码
     * @param request
     */
    @Override
    public void changePassword(ChangePasswordRequestVO request) {
        log.info("用户修改密码，用户ID：{}", request.getUserId());
        User user = userRepository.findUserByUserId(request.getUserId());
        if (ObjectUtil.isEmpty(user)) {
            throw new BusinessException("用户不存在");
        }
        // 校验是否是当前用户在操作
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("未登录或token无效，禁止访问！");
        }
        Long currentUserId = (Long) ((Map<String, Object>) authentication.getPrincipal()).get("id");
        if (!currentUserId.equals(user.getId())) {
            throw new BusinessException("您没有权限修改其他用户的密码");
        }
        if (!bCryptEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        user.setPassword(bCryptEncoder.encode(request.getNewPassword()));
        userRepository.update(user);
    }

    /**
     * 修改用户名
     * @param request
     */
    @Override
    public void changeUsername(ChangeUsernameRequestVO request) {
        log.info("用户修改用户名，用户ID：{}", request.getUserId());
        User user = userRepository.findUserByUserId(request.getUserId());
        if (ObjectUtil.isEmpty(user)) {
            throw new BusinessException("用户不存在");
        }
        // 校验是否是当前用户在操作
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException("未登录或token无效，禁止访问！");
        }
        Long currentUserId = (Long) ((Map<String, Object>) authentication.getPrincipal()).get("id");
        if (!currentUserId.equals(user.getId())) {
            throw new BusinessException("您没有权限修改其他用户的用户名");
        }
        user.setUsername(request.getNewUsername());
        userRepository.update(user);
    }

    /**
     * 忘记密码
     * @param request
     */
    @Override
    public void forgetPassword(ForgetPasswdRequestVO request) {
        log.info("用户忘记密码，手机号：{}", request.getPhone());
        // 校验验证码是否正确
        String verifyCodeKey = "verify_code:" + request.getPhone();
        String storedVerifyCode = redissonService.getValue(verifyCodeKey);
        if (ObjectUtil.isEmpty(storedVerifyCode)) {
            throw new BusinessException("验证码已过期");
        }
        if (!storedVerifyCode.equals(request.getVerifyCode())) {
            throw new BusinessException("验证码错误");
        }
        // 校验手机号是否存在
        User user = userRepository.findUserByPhone(request.getPhone());
        if (ObjectUtil.isEmpty(user)) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(bCryptEncoder.encode(request.getNewPassword()));
        userRepository.update(user);
        redissonService.remove(verifyCodeKey);
    }

    /**
     * 发送验证码
     * @param phone
     */
    @Override
    public void sendVerifyCode(String phone) throws Exception {
        log.info("用户发送验证码，手机号：{}", phone);
        // 校验手机号是否存在
        User user = userRepository.findUserByPhone(phone);
        if (ObjectUtil.isEmpty(user)) {
            throw new BusinessException("手机号不存在");
        }
        String code = verifyCodeService.generateAndStoreCode(phone, Duration.ofMinutes(SMS_CODE_EXPIRE_MINUTES));

        // 发送验证码
        log.info("发送验证码：{}", code);


        // 控制台打印验证码
        System.out.println("\n\n\n\n-------------------------\n"
                + "验证码：" + code + "\n"
                + "-------------------------\n");


        verifyCodeService.sendVerifyCode(phone, code);
    }

}

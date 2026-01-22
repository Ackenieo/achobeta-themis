package com.achobeta.themis.trigger.KnowledgeBase.http;

import com.achobeta.themis.common.ApiResponse;
import com.achobeta.themis.common.annotation.LoginRequired;
import com.achobeta.themis.common.util.SecurityUtils;
import com.achobeta.themis.domain.user.model.vo.UserFavoriteRegulationVO;
import com.achobeta.themis.domain.laws.service.IUserFavoriteRegulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户收藏法律法规Controller
 * 提供添加、删除、查询收藏功能
 *
 * @Author: ZGjie20
 * @version: 2.0.0
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/lawFavor")
@RequiredArgsConstructor
@LoginRequired
public class LawFavor {

    private final IUserFavoriteRegulationService favoriteService;

    /**
     * 添加法律条款收藏
     * 
     * @param regulationId 法规ID
     * @return 操作结果
     */
    @PostMapping("/add")
    public ApiResponse<Boolean> addFavorite(@RequestParam("regulationId") Integer regulationId) {
        try {
            String userId = SecurityUtils.getCurrentUserId();
            Long userIdLong = Long.parseLong(userId);
            
            log.info("用户 {} 添加收藏法规 {}", userId, regulationId);
            
            boolean success = favoriteService.addFavorite(userIdLong, regulationId);
            
            if (success) {
                return ApiResponse.success(true);
            } else {
                return ApiResponse.error("收藏失败，该法规可能已收藏或不存在");
            }
        } catch (Exception e) {
            log.error("添加收藏失败", e);
            return ApiResponse.error("添加收藏失败: " + e.getMessage());
        }
    }

    /**
     * 删除法律条款收藏
     * 
     * @param regulationId 法规ID
     * @return 操作结果
     */
    @DeleteMapping("/remove")
    public ApiResponse<Boolean> removeFavorite(@RequestParam("regulationId") Integer regulationId) {
        try {
            String userId = SecurityUtils.getCurrentUserId();
            Long userIdLong = Long.parseLong(userId);
            
            log.info("用户 {} 删除收藏法规 {}", userId, regulationId);
            
            boolean success = favoriteService.removeFavorite(userIdLong, regulationId);
            
            if (success) {
                return ApiResponse.success(true);
            } else {
                return ApiResponse.error("取消收藏失败，该收藏可能不存在");
            }
        } catch (Exception e) {
            log.error("删除收藏失败", e);
            return ApiResponse.error("删除收藏失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户是否已收藏某法规
     * 
     * @param regulationId 法规ID
     * @return 是否已收藏
     */
    @GetMapping("/check")
    public ApiResponse<Boolean> isFavorite(@RequestParam("regulationId") Integer regulationId) {
        try {
            String userId = SecurityUtils.getCurrentUserId();
            Long userIdLong = Long.parseLong(userId);
            
            boolean isFavorite = favoriteService.isFavorite(userIdLong, regulationId);
            
            return ApiResponse.success(isFavorite);
        } catch (Exception e) {
            log.error("查询收藏状态失败", e);
            return ApiResponse.error("查询收藏状态失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户的所有收藏
     * 
     * @return 收藏列表
     */
    @GetMapping("/list")
    public ApiResponse<List<UserFavoriteRegulationVO>> getUserFavorites() {
        try {
            String userId = SecurityUtils.getCurrentUserId();
            Long userIdLong = Long.parseLong(userId);
            
            log.info("查询用户 {} 的所有收藏", userId);
            
            List<UserFavoriteRegulationVO> favorites = favoriteService.getUserFavorites(userIdLong);
            
            return ApiResponse.success(favorites);
        } catch (Exception e) {
            log.error("查询收藏列表失败", e);
            return ApiResponse.error("查询收藏列表失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户的收藏数量
     * 
     * @return 收藏数量
     */
    @GetMapping("/count")
    public ApiResponse<Long> getUserFavoriteCount() {
        try {
            String userId = SecurityUtils.getCurrentUserId();
            Long userIdLong = Long.parseLong(userId);
            
            long count = favoriteService.getUserFavoriteCount(userIdLong);
            
            return ApiResponse.success(count);
        } catch (Exception e) {
            log.error("查询收藏数量失败", e);
            return ApiResponse.error("查询收藏数量失败: " + e.getMessage());
        }
    }
}



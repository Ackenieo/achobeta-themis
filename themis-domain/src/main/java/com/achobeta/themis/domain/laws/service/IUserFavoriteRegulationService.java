package com.achobeta.themis.domain.laws.service;

import com.achobeta.themis.domain.user.model.vo.UserFavoriteRegulationVO;

import java.util.List;

/**
 * 用户收藏法律法规服务接口
 * 
 * @author Cascade AI
 * @date 2025/11/20
 */
public interface IUserFavoriteRegulationService {

    /**
     * 添加收藏
     * 
     * @param userId 用户ID
     * @param regulationId 法规ID
     * @return 是否添加成功
     */
    boolean addFavorite(Long userId, Integer regulationId);

    /**
     * 删除收藏
     * 
     * @param userId 用户ID
     * @param regulationId 法规ID
     * @return 是否删除成功
     */
    boolean removeFavorite(Long userId, Integer regulationId);

    /**
     * 查询用户是否已收藏某法规
     * 
     * @param userId 用户ID
     * @param regulationId 法规ID
     * @return 是否已收藏
     */
    boolean isFavorite(Long userId, Integer regulationId);

    /**
     * 查询用户的所有收藏（包含法规详细信息）
     * 
     * @param userId 用户ID
     * @return 收藏列表
     */
    List<UserFavoriteRegulationVO> getUserFavorites(Long userId);

    /**
     * 查询用户的收藏数量
     * 
     * @param userId 用户ID
     * @return 收藏数量
     */
    long getUserFavoriteCount(Long userId);
}

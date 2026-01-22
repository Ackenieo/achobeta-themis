package com.achobeta.themis.domain.laws.service.impl;

import com.achobeta.themis.domain.laws.model.entity.LawRegulation;
import com.achobeta.themis.domain.user.model.vo.UserFavoriteRegulationVO;
import com.achobeta.themis.domain.user.repo.IUserFavoriteRegulationRepository;
import com.achobeta.themis.domain.laws.service.IUserFavoriteRegulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户收藏法律法规服务实现
 * 
 * @author Cascade AI
 * @date 2025/11/20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserFavoriteRegulationServiceImpl implements IUserFavoriteRegulationService {

    private final IUserFavoriteRegulationRepository favoriteRepository;

    @Override
    public boolean addFavorite(Long userId, Integer regulationId) {
        log.info("用户 {} 添加收藏法规 {}", userId, regulationId);
        
        // 验证法规是否存在
        LawRegulation regulation = favoriteRepository.findRegulationById(regulationId);
        if (regulation == null) {
            log.warn("法规 {} 不存在", regulationId);
            return false;
        }
        
        return favoriteRepository.addFavorite(userId, regulationId);
    }

    @Override
    public boolean removeFavorite(Long userId, Integer regulationId) {
        log.info("用户 {} 删除收藏法规 {}", userId, regulationId);
        return favoriteRepository.removeFavorite(userId, regulationId);
    }

    @Override
    public boolean isFavorite(Long userId, Integer regulationId) {
        return favoriteRepository.isFavorite(userId, regulationId);
    }

    @Override
    public List<UserFavoriteRegulationVO> getUserFavorites(Long userId) {
        log.info("查询用户 {} 的所有收藏", userId);
        
        // 直接调用Repository层的方法，该方法已包含法规详细信息的关联查询
        return favoriteRepository.getUserFavoritesWithDetails(userId);
    }

    @Override
    public long getUserFavoriteCount(Long userId) {
        return favoriteRepository.getUserFavoriteCount(userId);
    }
}

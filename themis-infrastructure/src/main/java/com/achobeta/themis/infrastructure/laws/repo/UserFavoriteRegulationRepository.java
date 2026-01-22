package com.achobeta.themis.infrastructure.laws.repo;

import com.achobeta.themis.domain.laws.model.entity.LawCategory;
import com.achobeta.themis.domain.laws.model.entity.LawRegulation;
import com.achobeta.themis.domain.laws.model.entity.UserFavoriteRegulation;
import com.achobeta.themis.domain.user.model.vo.UserFavoriteRegulationVO;
import com.achobeta.themis.domain.user.repo.IUserFavoriteRegulationRepository;
import com.achobeta.themis.infrastructure.laws.mapper.LawCategoryMapper;
import com.achobeta.themis.infrastructure.laws.mapper.LawRegulationMapper;
import com.achobeta.themis.infrastructure.laws.mapper.UserFavoriteRegulationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户收藏法律法规仓储实现
 * 
 * @author Cascade AI
 * @date 2025/11/20
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserFavoriteRegulationRepository implements IUserFavoriteRegulationRepository {
     private  final LawCategoryMapper lawCategoryMapper;
    private final UserFavoriteRegulationMapper favoriteMapper;
    private final LawRegulationMapper lawRegulationMapper;

    @Override
    public boolean addFavorite(Long userId, Integer regulationId) {
        try {
            // 检查是否已收藏
            if (isFavorite(userId, regulationId)) {
                log.warn("用户 {} 已收藏法规 {}", userId, regulationId);
                return false;
            }

            UserFavoriteRegulation favorite = UserFavoriteRegulation.builder()
                    .userId(userId)
                    .regulationId(regulationId)
                    .createdTime(LocalDateTime.now())
                    .build();

            int result = favoriteMapper.insert(favorite);
            return result > 0;
        } catch (Exception e) {
            log.error("添加收藏失败: userId={}, regulationId={}", userId, regulationId, e);
            return false;
        }
    }

    @Override
    public boolean removeFavorite(Long userId, Integer regulationId) {
        try {
            LambdaQueryWrapper<UserFavoriteRegulation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserFavoriteRegulation::getUserId, userId)
                   .eq(UserFavoriteRegulation::getRegulationId, regulationId);

            int result = favoriteMapper.delete(wrapper);
            return result > 0;
        } catch (Exception e) {
            log.error("删除收藏失败: userId={}, regulationId={}", userId, regulationId, e);
            return false;
        }
    }

    @Override
    public boolean isFavorite(Long userId, Integer regulationId) {
        LambdaQueryWrapper<UserFavoriteRegulation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavoriteRegulation::getUserId, userId)
               .eq(UserFavoriteRegulation::getRegulationId, regulationId);

        return favoriteMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<UserFavoriteRegulation> getUserFavorites(Long userId) {
        LambdaQueryWrapper<UserFavoriteRegulation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavoriteRegulation::getUserId, userId)
               .orderByDesc(UserFavoriteRegulation::getCreatedTime);

        return favoriteMapper.selectList(wrapper);
    }

    @Override
    public long getUserFavoriteCount(Long userId) {
        LambdaQueryWrapper<UserFavoriteRegulation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFavoriteRegulation::getUserId, userId);

        return favoriteMapper.selectCount(wrapper);
    }

    @Override
    public LawRegulation findRegulationById(Integer regulationId) {
        return lawRegulationMapper.selectById(regulationId);
    }

    @Override
    public List<UserFavoriteRegulationVO> getUserFavoritesWithDetails(Long userId) {
        // 获取用户的收藏列表
        List<UserFavoriteRegulation> favorites = getUserFavorites(userId);
        
        if (favorites == null || favorites.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 关联查询法规详细信息
        return favorites.stream().map(favorite -> {
            LawRegulation regulation = lawRegulationMapper.selectById(favorite.getRegulationId());
            
            if (regulation == null) {
                log.warn("法规 {} 不存在，跳过", favorite.getRegulationId());
                return null;
            }
                    LawCategory lawCategory = lawCategoryMapper.selectById(regulation.getLawCategoryId());

            return UserFavoriteRegulationVO.builder()
                    .id(favorite.getId())
                    .userId(favorite.getUserId())
                    .regulationId(favorite.getRegulationId())
                    .lawCategoryId(regulation.getLawCategoryId())
                    .issueYear(regulation.getIssueYear())
                    .articleNumber(regulation.getArticleNumber())
                    .originalText(regulation.getOriginalText())
                    .createdTime(favorite.getCreatedTime())
                    .regulationName(lawCategory.getLawName())
                    .build();
        }).filter(vo -> vo != null)
          .collect(Collectors.toList());
    }
}

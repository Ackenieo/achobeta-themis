package com.achobeta.themis.domain.laws.service.impl;

import com.achobeta.themis.common.ApiResponse;
import com.achobeta.themis.common.exception.BusinessException;
import com.achobeta.themis.domain.laws.model.LawModel;
import com.achobeta.themis.domain.laws.model.entity.LawCategory;
import com.achobeta.themis.domain.laws.model.entity.LawRegulation;
import com.achobeta.themis.domain.laws.model.vo.LawCategoryWithRegulationsVO;
import com.achobeta.themis.domain.laws.repo.ILawCategoryRepository;
import com.achobeta.themis.domain.laws.repo.ILawRegulationRepository;
import com.achobeta.themis.domain.laws.service.ILawRegulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LawRegulationService implements ILawRegulationService {

    private final ILawRegulationRepository lawRegulationRepository;
    private final ILawCategoryRepository lawCategoryRepository;

    @Override
    public List<LawRegulation> queryLawRegulationList() {
        return lawRegulationRepository.listLawRegulations();
    }

    @Override
    public LawModel queryLawsByCondition(Integer categoryType, Integer page, Integer size) {
        List<LawCategory> categories = lawCategoryRepository.listLawCategoriesConditional(Long.valueOf(categoryType), page, size);
        // 为每个分类查询关联的法律条款
        List<LawCategoryWithRegulationsVO> result = categories.stream().map(category -> {
            List<LawRegulation> regulations = lawRegulationRepository.listLawRegulationsConditional(category.getCategoryType(), page, size);
            LawCategoryWithRegulationsVO categoryWithRegulationsVO = LawCategoryWithRegulationsVO.of(category, regulations);
            return categoryWithRegulationsVO;
        }).toList();
        return LawModel.builder()
                .lawCategoryWithRegulationsVOS(result)
                .page(page)
                .size(size)
                .total(categories.size())
                .build();
    }

    @Override
    public LawCategoryWithRegulationsVO queryLawsByCondition(Integer lawId, Integer articleNumber) {
        LawCategory category = lawCategoryRepository.getById(Long.valueOf(lawId));
        if (category == null) {
            log.debug("未找到法律分类，lawId: {}", lawId);
            throw new BusinessException("未找到指定的法律分类");
        }

        List<LawRegulation> regulations = lawRegulationRepository.listLawRegulationsConditional(category.getCategoryType(), 1, Integer.MAX_VALUE);
        LawCategoryWithRegulationsVO categoryWithRegulationsVO = LawCategoryWithRegulationsVO.of(category, regulations);
        return categoryWithRegulationsVO;
    }
}

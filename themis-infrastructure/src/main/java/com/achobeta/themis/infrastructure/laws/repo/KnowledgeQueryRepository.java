package com.achobeta.themis.infrastructure.laws.repo;

import cn.hutool.core.util.ObjectUtil;
import com.achobeta.themis.common.component.MeiliSearchComponent;
import com.achobeta.themis.common.component.entity.LawDocument;
import com.achobeta.themis.common.exception.BusinessException;
import com.achobeta.themis.common.util.IKPreprocessorUtil;
import com.achobeta.themis.domain.laws.model.entity.LawCategory;
import com.achobeta.themis.domain.laws.model.entity.LawRegulation;
import com.achobeta.themis.domain.laws.repo.IKnowledgeQueryRepository;
import com.achobeta.themis.domain.laws.repo.ILawCategoryRepository;
import com.achobeta.themis.domain.laws.repo.ILawRegulationRepository;
import com.achobeta.themis.infrastructure.laws.mapper.LawCategoryMapper;
import com.achobeta.themis.infrastructure.laws.mapper.LawRegulationMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 知识库查询Repository实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class KnowledgeQueryRepository implements IKnowledgeQueryRepository, ILawCategoryRepository, ILawRegulationRepository {
    
    private final MeiliSearchComponent meiliSearchComponent;
    private final LawCategoryMapper lawCategoryMapper;
    private final LawRegulationMapper lawRegulationMapper;
    
    @Override
    public List<LawDocument> searchLawDocuments(String query, Integer lawCategoryId, Integer limit) throws Exception {
        log.info("搜索法律文档，query={}, lawCategoryId={}, limit={}", query, lawCategoryId, limit);
        
        // 对查询进行分词
        String segmentedQuery = IKPreprocessorUtil.segment(query, true);
        log.info("分词后的查询: {}", segmentedQuery);
        
        // 调用MeiliSearch搜索
        List<LawDocument> documents = meiliSearchComponent.searchLawDocuments(
                segmentedQuery, 
                lawCategoryId, 
                limit != null && limit > 0 ? limit : 5,
                LawDocument.class
        );
        
        log.info("搜索到 {} 条法律文档", documents.size());
        return documents;
    }

    @Override
    public List<LawCategory> listLawCategories() {
        return lawCategoryMapper.selectList(null);
    }

    @Override
    public LawCategory getById(Long id) {
        return lawCategoryMapper.selectById(id);
    }

    @Override
    public void save(LawCategory lawCategory) {
        lawCategoryMapper.insert(lawCategory);
    }

    @Override
    public void delete(Long id) {
        lawCategoryMapper.deleteById(id);
    }

    @Override
    public List<LawRegulation> listLawRegulations() {
        return lawRegulationMapper.selectList(null);
    }

    @Override
    public List<LawRegulation> listLawRegulationsConditional(Integer categoryId, Integer page, Integer size) {
        // 查询该分类下的所有法律条款
        LambdaQueryWrapper<LawRegulation> regulationWrapper = new LambdaQueryWrapper<>();
        regulationWrapper.eq(LawRegulation::getLawCategoryId, categoryId)
                .orderByAsc(LawRegulation::getArticleNumber);
        List<LawRegulation> regulations = lawRegulationMapper.selectList(regulationWrapper);

        return regulations;
    }

    @Override
    public List<LawCategory> listLawCategoriesConditional(Long categoryType, Integer page, Integer size) {
        // 分页查询法律分类
        LambdaQueryWrapper<LawCategory> categoryWrapper = new LambdaQueryWrapper<>();
        categoryWrapper.eq(LawCategory::getCategoryType, categoryType)
                .orderByAsc(LawCategory::getLawId)
                .last("LIMIT " + size + " OFFSET " + ((page - 1) * size));
        List<LawCategory> categories = lawCategoryMapper.selectList(categoryWrapper);

        if (ObjectUtil.isEmpty(categories)) {
            log.debug("未找到category_type={}的法律分类", categoryType);
            throw new BusinessException("未找到category_type=" + categoryType + "的法律分类");
        }

        return categories;
    }
}

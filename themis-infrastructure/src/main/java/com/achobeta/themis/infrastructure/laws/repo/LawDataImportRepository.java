package com.achobeta.themis.infrastructure.laws.repo;

import com.achobeta.themis.common.component.MeiliSearchComponent;
import com.achobeta.themis.common.component.entity.LawDocument;
import com.achobeta.themis.common.util.IKPreprocessorUtil;
import com.achobeta.themis.domain.laws.model.entity.LawCategory;
import com.achobeta.themis.domain.laws.model.entity.LawRegulation;
import com.achobeta.themis.domain.laws.repo.ILawDataImportRepository;
import com.achobeta.themis.infrastructure.laws.mapper.LawCategoryMapper;
import com.achobeta.themis.infrastructure.laws.mapper.LawRegulationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class LawDataImportRepository implements ILawDataImportRepository {
    
    private final LawCategoryMapper lawCategoryMapper;
    private final LawRegulationMapper lawRegulationMapper;
    private final MeiliSearchComponent meiliSearchComponent;
    
    private final String LAW_DOCUMENTS = "law_documents";
    
    /**
     * 从数据库导入法律数据到 Meilisearch
     * @return 导入的文档数量
     */
    public int importLawDataFromDatabase() throws Exception {
        log.info("开始从数据库导入法律数据到 Meilisearch");
        
        // 1. 查询所有法律分类
        List<LawCategory> categories = lawCategoryMapper.selectList(null);
        if (categories == null || categories.isEmpty()) {
            log.warn("数据库中没有法律分类数据");
            return 0;
        }
        
        // 2. 创建分类ID到名称的映射
        Map<Long, LawCategory> categoryMap = new HashMap<>();
        for (LawCategory category : categories) {
            categoryMap.put(category.getLawId(), category);
        }
        
        // 3. 查询所有法律条文
        List<LawRegulation> regulations = lawRegulationMapper.selectList(null);
        if (regulations == null || regulations.isEmpty()) {
            log.warn("数据库中没有法律条文数据");
            return 0;
        }
        
        // 4. 转换为 Meilisearch 文档
        List<LawDocument> lawDocuments = new ArrayList<>();
        for (LawRegulation regulation : regulations) {
            LawCategory category = categoryMap.get(regulation.getLawCategoryId());
            if (category == null) {
                log.warn("法条 {} 的分类 {} 不存在，跳过", 
                        regulation.getRegulationId(), regulation.getLawCategoryId());
                continue;
            }
            
            // 对法条原文进行分词
            String segmentedText = IKPreprocessorUtil.segment(regulation.getOriginalText(), true);
            
            LawDocument document = LawDocument.builder()
                    .id(regulation.getRegulationId())
                    .lawName(category.getLawName())
                    .lawCategoryId(regulation.getLawCategoryId())
                    .articleNumber(regulation.getArticleNumber())
                    .originalText(regulation.getOriginalText())
                    .originalTextSegmented(segmentedText)
                    .issueYear(regulation.getIssueYear())
                    .relatedRegulationIds(category.getRelatedRegulationIds())
                    .createTime(regulation.getCreateTime())
                    .build();
            
            lawDocuments.add(document);
        }
        
        // 5. 批量添加到 Meilisearch
        if (!lawDocuments.isEmpty()) {
            meiliSearchComponent.addDocuments(LAW_DOCUMENTS, lawDocuments);
            log.info("成功导入 {} 条法律数据到 Meilisearch", lawDocuments.size());
        }
        
        return lawDocuments.size();
    }
}

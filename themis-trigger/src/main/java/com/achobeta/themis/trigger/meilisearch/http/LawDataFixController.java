package com.achobeta.themis.trigger.meilisearch.http;

import com.achobeta.themis.common.component.entity.LawDocument;
import com.achobeta.themis.common.util.IKPreprocessorUtil;
import com.achobeta.themis.domain.laws.model.entity.LawCategory;
import com.achobeta.themis.domain.laws.model.entity.LawRegulation;
import com.achobeta.themis.domain.laws.service.ILawCategoryService;
import com.achobeta.themis.domain.laws.service.ILawRegulationService;
import com.alibaba.fastjson.JSON;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.model.Settings;
import com.meilisearch.sdk.model.Task;
import com.meilisearch.sdk.model.TaskInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/law/fix")
@RequiredArgsConstructor
public class LawDataFixController {
    
    private final Client meiliSearchClient;
    private final ILawCategoryService lawCategoryService;
    private final ILawRegulationService lawRegulationService;
    
    /**
     * 检查索引配置
     */
    @GetMapping("/check-settings")
    public Map<String, Object> checkSettings() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Index index = meiliSearchClient.index("law_documents");
            Settings settings = index.getSettings();
            
            // 直接获取主键
            String primaryKey = index.getPrimaryKey();
            
            result.put("主键", primaryKey != null ? primaryKey : "未设置");
            result.put("可搜索字段", settings.getSearchableAttributes());
            result.put("可过滤字段", settings.getFilterableAttributes());
            result.put("可排序字段", settings.getSortableAttributes());
            result.put("状态", "成功");
            
        } catch (Exception e) {
            log.error("检查设置失败", e);
            result.put("状态", "失败");
            result.put("错误", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 重新创建索引（删除旧的，创建新的）
     */
    @PostMapping("/recreate-index")
    public Map<String, Object> recreateIndex() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 删除旧索引
            try {
                meiliSearchClient.deleteIndex("law_documents");
                Thread.sleep(1000); // 等待删除完成
                log.info("已删除旧索引");
            } catch (Exception e) {
                log.info("旧索引不存在或删除失败: {}", e.getMessage());
            }
            
            // 创建新索引，指定主键为 id
            TaskInfo createTask = meiliSearchClient.createIndex("law_documents", "id");
            meiliSearchClient.waitForTask(createTask.getTaskUid());
            log.info("已创建新索引，主键: id");
            
            // 配置索引
            Index index = meiliSearchClient.index("law_documents");
            Settings settings = new Settings();
            
            settings.setSearchableAttributes(new String[]{ "originalTextSegmented", "lawName" });
            settings.setFilterableAttributes(new String[]{ "lawCategoryId", "articleNumber", "issueYear", "categoryType" });
            settings.setSortableAttributes(new String[]{ "articleNumber", "createTime" });
            
            TaskInfo settingsTask = index.updateSettings(settings);
            meiliSearchClient.waitForTask(settingsTask.getTaskUid());
            
            result.put("状态", "成功");
            result.put("消息", "索引已重新创建并配置完成");
            result.put("主键", "id");
            
        } catch (Exception e) {
            log.error("重新创建索引失败", e);
            result.put("状态", "失败");
            result.put("错误", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 使用正确的主键重新导入数据
     */
    @PostMapping("/reimport-with-fix")
    public Map<String, Object> reimportWithFix() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("开始重新导入数据...");
            
            // 1. 查询数据
            List<LawCategory> categories = lawCategoryService.queryLawCategoryList();
            List<LawRegulation> regulations = lawRegulationService.queryLawRegulationList();
            
            if (categories == null || categories.isEmpty() || regulations == null || regulations.isEmpty()) {
                result.put("状态", "失败");
                result.put("错误", "数据库中没有数据");
                return result;
            }
            
            // 2. 创建分类映射
            Map<Long, LawCategory> categoryMap = new HashMap<>();
            for (LawCategory category : categories) {
                categoryMap.put(category.getLawId(), category);
            }
            
            // 3. 转换数据
            List<LawDocument> lawDocuments = new ArrayList<>();
            for (LawRegulation regulation : regulations) {
                LawCategory category = categoryMap.get(regulation.getLawCategoryId());
                if (category == null) {
                    continue;
                }
                
                String segmentedText = IKPreprocessorUtil.segment(regulation.getOriginalText(), true);
                
                LawDocument document = LawDocument.builder()
                        .id(regulation.getRegulationId())
                        .lawName(category.getLawName())
                        .lawCategoryId(regulation.getLawCategoryId())
                        .categoryType(category.getCategoryType())
                        .articleNumber(regulation.getArticleNumber())
                        .originalText(regulation.getOriginalText())
                        .originalTextSegmented(segmentedText)
                        .issueYear(regulation.getIssueYear())
                        .relatedRegulationIds(category.getRelatedRegulationIds())
                        .createTime(regulation.getCreateTime())
                        .updateTime(regulation.getUpdateTime())
                        .build();
                
                lawDocuments.add(document);
            }
            
            // 统计国家法规和地方法规数量
            long nationalCount = lawDocuments.stream().filter(doc -> doc.getCategoryType() == 1).count();
            long localCount = lawDocuments.stream().filter(doc -> doc.getCategoryType() == 0).count();
            log.info("准备导入 {} 条数据（国家法规: {}, 地方法规: {}）", lawDocuments.size(), nationalCount, localCount);
            
            // 4. 批量添加到 Meilisearch
            Index index = meiliSearchClient.index("law_documents");
            String json = JSON.toJSONString(lawDocuments);
            
            TaskInfo taskInfo = index.addDocuments(json, "id"); // 明确指定主键
            log.info("文档已提交，taskUid={}", taskInfo.getTaskUid());
            
            meiliSearchClient.waitForTask(taskInfo.getTaskUid());
            
            Task task = meiliSearchClient.getTask(taskInfo.getTaskUid());
            log.info("任务完成: status={}", task.getStatus());
            
            if ("failed".equals(task.getStatus())) {
                var error = task.getError();
                result.put("状态", "失败");
                result.put("错误", error != null ? error.getMessage() : "未知错误");
                return result;
            }
            
            // 5. 验证结果
            var stats = index.getStats();
            result.put("状态", "成功");
            result.put("导入数量", lawDocuments.size());
            result.put("索引文档总数", stats.getNumberOfDocuments());
            
        } catch (Exception e) {
            log.error("重新导入失败", e);
            result.put("状态", "失败");
            result.put("错误", e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
    
    /**
     * 验证数据完整性
     */
    @GetMapping("/validate-data")
    public Map<String, Object> validateData() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 查询数据库数据
            List<LawCategory> categories = lawCategoryService.queryLawCategoryList();
            List<LawRegulation> regulations = lawRegulationService.queryLawRegulationList();
            
            // 2. 统计数据
            long nationalCategories = categories.stream().filter(c -> c.getCategoryType() == 1).count();
            long localCategories = categories.stream().filter(c -> c.getCategoryType() == 0).count();
            
            // 3. 检查关联关系
            Map<Long, LawCategory> categoryMap = new HashMap<>();
            for (LawCategory category : categories) {
                categoryMap.put(category.getLawId(), category);
            }
            
            long orphanedRegulations = regulations.stream()
                    .filter(reg -> !categoryMap.containsKey(reg.getLawCategoryId()))
                    .count();
            
            // 4. 检查 Meilisearch 索引
            Index index = meiliSearchClient.index("law_documents");
            var stats = index.getStats();
            
            result.put("状态", "成功");
            result.put("数据库统计", Map.of(
                    "法律分类总数", categories.size(),
                    "国家法规分类", nationalCategories,
                    "地方法规分类", localCategories,
                    "法律条文总数", regulations.size(),
                    "孤立条文数", orphanedRegulations
            ));
            result.put("Meilisearch统计", Map.of(
                    "索引文档总数", stats.getNumberOfDocuments(),
                    "是否正在索引", stats.isIndexing()
            ));
            
            // 5. 数据一致性检查
            long expectedDocuments = regulations.size() - orphanedRegulations;
            boolean isConsistent = stats.getNumberOfDocuments() == expectedDocuments;
            result.put("数据一致性", Map.of(
                    "预期文档数", expectedDocuments,
                    "实际文档数", stats.getNumberOfDocuments(),
                    "是否一致", isConsistent
            ));
            
        } catch (Exception e) {
            log.error("验证数据失败", e);
            result.put("状态", "失败");
            result.put("错误", e.getMessage());
        }
        
        return result;
    }
}

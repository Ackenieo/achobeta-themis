package com.achobeta.themis.trigger.meilisearch.http;

import com.achobeta.themis.common.component.entity.LawDocument;
import com.achobeta.themis.domain.laws.model.entity.LawCategory;
import com.achobeta.themis.domain.laws.model.entity.LawRegulation;
import com.achobeta.themis.domain.laws.service.ILawCategoryService;
import com.achobeta.themis.domain.laws.service.ILawRegulationService;
import com.alibaba.fastjson.JSON;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.model.IndexStats;
import com.meilisearch.sdk.model.Task;
import com.meilisearch.sdk.model.TaskInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/law/debug")
@RequiredArgsConstructor
public class LawDataDebugController {
    
    private final Client meiliSearchClient;
    private final ILawCategoryService lawCategoryService;
    private final ILawRegulationService lawRegulationService;
    
    /**
     * 检查 law_documents 索引状态
     */
    @GetMapping("/check-index")
    public Map<String, Object> checkIndex() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Index index = meiliSearchClient.index("law_documents");
            
            // 获取索引统计信息
            IndexStats stats = index.getStats();
            result.put("索引名称", "law_documents");
            result.put("文档数量", stats.getNumberOfDocuments());
            result.put("是否正在索引", stats.isIndexing());
            result.put("字段分布", stats.getFieldDistribution());
            
            log.info("law_documents 索引状态: 文档数={}, 正在索引={}", 
                    stats.getNumberOfDocuments(), stats.isIndexing());
            
            result.put("状态", "成功");
            
        } catch (Exception e) {
            log.error("检查索引失败", e);
            result.put("状态", "失败");
            result.put("错误信息", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 获取所有索引列表
     */
    @GetMapping("/list-indexes")
    public Map<String, Object> listIndexes() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            var indexes = meiliSearchClient.getIndexes();
            result.put("索引列表", indexes.getResults());
            result.put("总数", indexes.getResults().length);
            result.put("状态", "成功");
            
            log.info("找到 {} 个索引", indexes.getResults().length);
            
        } catch (Exception e) {
            log.error("获取索引列表失败", e);
            result.put("状态", "失败");
            result.put("错误信息", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 测试添加单个文档
     */
    @PostMapping("/test-add-one")
    public Map<String, Object> testAddOne() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 创建测试文档
            LawDocument testDoc = LawDocument.builder()
                    .id(99999L)
                    .lawName("测试法律")
                    .lawCategoryId(1L)
                    .articleNumber(1)
                    .originalText("这是一条测试法条")
                    .originalTextSegmented("这是 一条 测试 法条")
                    .issueYear("2025-01-01")
                    .relatedRegulationIds(Arrays.asList(1, 2, 3))
                    .createTime(LocalDateTime.now())
                    .build();
            
            // 序列化为 JSON
            String json = JSON.toJSONString(Collections.singletonList(testDoc));
            log.info("测试文档 JSON: {}", json);
            result.put("JSON", json);
            
            // 添加到 Meilisearch
            Index index = meiliSearchClient.index("law_documents");
            TaskInfo taskInfo = index.addDocuments(json);
            
            log.info("文档已提交，taskUid={}", taskInfo.getTaskUid());
            result.put("taskUid", taskInfo.getTaskUid());
            result.put("taskStatus", taskInfo.getStatus());
            
            // 等待任务完成
            meiliSearchClient.waitForTask(taskInfo.getTaskUid());
            
            // 获取任务详情
            Task task = meiliSearchClient.getTask(taskInfo.getTaskUid());
            log.info("任务完成: status={}, type={}", task.getStatus(), task.getType());
            
            result.put("最终状态", task.getStatus());
            result.put("任务类型", task.getType());
            result.put("任务详情", task.getDetails());
            
            if ("failed".equals(task.getStatus())) {
                var error = task.getError();
                result.put("错误对象", error);
                if (error != null) {
                    result.put("错误消息", error.getMessage());
                    result.put("错误代码", error.getCode());
                    result.put("错误类型", error.getType());
                    result.put("错误链接", error.getLink());
                }
                log.error("任务失败: {}", error);
            }
            
            // 检查文档数量
            IndexStats stats = index.getStats();
            result.put("当前文档总数", stats.getNumberOfDocuments());
            result.put("状态", "成功");
            
        } catch (Exception e) {
            log.error("测试添加文档失败", e);
            result.put("状态", "失败");
            result.put("错误信息", e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
    
    /**
     * 查看数据库中的数据样本
     */
    @GetMapping("/check-db-data")
    public Map<String, Object> checkDbData() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<LawCategory> categories = lawCategoryService.queryLawCategoryList();
            List<LawRegulation> regulations = lawRegulationService.queryLawRegulationList();
            
            result.put("分类数量", categories == null ? 0 : categories.size());
            result.put("条文数量", regulations == null ? 0 : regulations.size());
            
            if (categories != null && !categories.isEmpty()) {
                result.put("分类样本", categories.get(0));
            }
            
            if (regulations != null && !regulations.isEmpty()) {
                result.put("条文样本", regulations.get(0));
            }
            
            result.put("状态", "成功");
            
        } catch (Exception e) {
            log.error("查询数据库失败", e);
            result.put("状态", "失败");
            result.put("错误信息", e.getMessage());
        }
        
        return result;
    }
}

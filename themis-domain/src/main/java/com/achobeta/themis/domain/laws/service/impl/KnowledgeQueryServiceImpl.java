package com.achobeta.themis.domain.laws.service.impl;

import com.achobeta.themis.common.component.entity.LawDocument;
import com.achobeta.themis.domain.laws.model.vo.KnowledgeQueryRequestVO;
import com.achobeta.themis.domain.laws.repo.IKnowledgeQueryRepository;
import com.achobeta.themis.domain.chat.service.IConversationHistoryService;
import com.achobeta.themis.domain.laws.service.IKnowledgeQueryService;
import com.achobeta.themis.domain.laws.service.IKnowledgeSearchHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库查询Service实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeQueryServiceImpl implements IKnowledgeQueryService {
    
    private final IKnowledgeQueryRepository knowledgeQueryRepository;
    private final IConversationHistoryService conversationHistoryService;
    private final IKnowledgeSearchHistoryService knowledgeSearchHistoryService;
    
//    @Autowired
//    @Qualifier("Knowledge")
//    private IAiKnowledgeService aiKnowledgeService;
//
//    @Override
//    public KnowledgeQueryResponseVO queryKnowledge(String userId, KnowledgeQueryRequestVO request) throws Exception {
//        log.info("用户 {} 查询知识库: {}", userId, request.getQuestion());
//
//        // 1. 处理对话ID
//        String conversationId = request.getConversationId();
//        if (conversationId == null || conversationId.isBlank()) {
//            // 创建新对话
//            conversationId = UUID.randomUUID().toString();
//
//            log.info("创建新对话id: {}", conversationId);
//        } else {
//            // 触碰现有对话，续期
//            conversationHistoryService.touch(userId, conversationId);
//            log.info("继续对话: {}", conversationId);
//        }
//
//        // 2. 从MeiliSearch搜索相关法律文档
//        List<LawDocument> lawDocuments = knowledgeQueryRepository.searchLawDocuments(
//                request.getQuestion(),
//                request.getLawCategoryId(),
//                request.getLimit()
//        );
//
//        if (lawDocuments == null || lawDocuments.isEmpty()) {
//            log.warn("未找到相关法律文档");
//            return KnowledgeQueryResponseVO.builder()
//                    .conversationId(conversationId)
//                    .lawDocumentsWithAnalysis(List.of())
//                    .timestamp(System.currentTimeMillis())
//                    .build();
//        }
//
//        // 3. 为每个法律文档生成AI解析
//        List<KnowledgeQueryResponseVO.LawDocumentWithAnalysis> documentsWithAnalysis = new java.util.ArrayList<>();
//
//        for (int i = 0; i < lawDocuments.size(); i++) {
//            LawDocument doc = lawDocuments.get(i);
//            log.info("正在为第{}个法条生成AI解析: {}第{}条", i + 1, doc.getLawName(), doc.getArticleNumber());
//
//            // 构建AI提示词
//            String aiPrompt = buildAiPromptForSingleDocument(request.getQuestion(), doc);
//
//            // 调用AI服务进行解析
//            String aiAnalysis = "";
//            try {
//                // 为每个法条使用同一个conversationId，保持上下文连贯性
//                Flux<String> stream = aiKnowledgeService.chat(conversationId, aiPrompt);
//                List<String> chunks = stream.collectList().block();
//                aiAnalysis = String.join("", chunks);
//                log.info("第{}个法条AI解析完成，长度: {}", i + 1, aiAnalysis.length());
//            } catch (Exception e) {
//                log.error("第{}个法条AI解析失败", i + 1, e);
//              //  aiAnalysis = formatSingleLawDocumentAsText(doc) + "\n\nAI解析服务暂时不可用。";
//            }
//
//            // 添加到结果列表
//            documentsWithAnalysis.add(
//                KnowledgeQueryResponseVO.LawDocumentWithAnalysis.builder()
//                    .lawDocument(doc)
//                    .aiAnalysis(aiAnalysis)
//                    .build()
//            );
//        }
//
//        // 4. 保存搜索历史记录
//        try {
//            KnowledgeSearchRecord searchRecord = KnowledgeSearchRecord.builder()
//                    .recordId(UUID.randomUUID().toString())
//                    .userId(userId)
//                    .question(request.getQuestion())
//                    .conversationId(conversationId)
//                    .lawCategoryId(request.getLawCategoryId())
//                    .resultCount(documentsWithAnalysis.size())
//                    .createTime(System.currentTimeMillis())
//                    .updateTime(System.currentTimeMillis())
//                    .build();
//            knowledgeSearchHistoryService.saveSearchRecord(searchRecord);
//            log.info("知识库搜索历史已保存，记录ID: {}", searchRecord.getRecordId());
//        } catch (Exception e) {
//            log.error("保存知识库搜索历史失败，但不影响查询结果返回", e);
//        }
//
//        // 5. 构建响应
//        return KnowledgeQueryResponseVO.builder()
//                .conversationId(conversationId)
//                .lawDocumentsWithAnalysis(documentsWithAnalysis)
//                .timestamp(System.currentTimeMillis())
//                .build();
//    }

    @Override
    public Map<Long, String> queryKnowledgeId(KnowledgeQueryRequestVO request) throws Exception {
        // 2. 从MeiliSearch搜索相关法律文档
        List<LawDocument> lawDocuments = knowledgeQueryRepository.searchLawDocuments(
                request.getQuestion(),
                request.getLawCategoryId(),
                request.getLimit()
        );

        if (lawDocuments == null || lawDocuments.isEmpty()) {
            log.warn("未找到相关法律文档");
            return Collections.emptyMap();
        }
        Map<Long, String> knowledgeMap = new HashMap<>();
        for (int i = 0; i < lawDocuments.size(); i++) {
            LawDocument doc = lawDocuments.get(i);
            // 添加到结果列表
            knowledgeMap.put((long) doc.getId(), doc.getOriginalText());
        }
        return knowledgeMap;
    }

    /**
     * 为单个法律文档构建AI提示词
     */
    private String buildAiPromptForSingleDocument(String question, LawDocument doc) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("用户问题：").append(question).append("\n\n");
        prompt.append("相关法律条款：\n\n");
        
        prompt.append("法律名称：").append(doc.getLawName()).append("\n");
        prompt.append("条款号：第").append(doc.getArticleNumber()).append("条\n");
        prompt.append("法条原文：").append(doc.getOriginalText()).append("\n");
        
        if (doc.getRelatedRegulationIds() != null && !doc.getRelatedRegulationIds().isEmpty()) {
            prompt.append("关联法条ID：").append(
                    doc.getRelatedRegulationIds().stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", "))
            ).append("\n");
        }
        
        prompt.append("\n请根据上述法条，严格按照系统提示词（prompt-zhishiku.txt）的格式要求，对用户问题进行详细解析。");
        prompt.append("注意：关联法条必须从同一法律文件中选取，并且返回关联法条的条款小写数字作为law_id。");
        
        return prompt.toString();
    }
    /*
    *//**
     * 将单个法律文档格式化为文本（备用方案）
     */
   /* private String formatSingleLawDocumentAsText(LawDocument doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("【标题】\n");
        sb.append("《").append(doc.getLawName()).append("》第").append(doc.getArticleNumber()).append("条\n\n");
        sb.append("【原文】\n");
        sb.append(doc.getOriginalText()).append("\n\n");
        return sb.toString();
    }*/
}

package com.achobeta.themis.common.component;

import com.achobeta.themis.common.component.entity.KnowledgeBaseQuestionDocument;
import com.achobeta.themis.common.component.entity.QuestionTitleDocument;
import com.achobeta.themis.common.util.IKPreprocessorUtil;
import com.alibaba.fastjson.JSON;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.SearchRequest;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Component
@Slf4j
public class MeiliSearchComponent implements CommandLineRunner {

    @Autowired
    private Client meiliSearchClient;

    private static final String QUESTION_TITLE_DOCUMENTS = "question_title_documents";
    private static final String KNOWLEDGE_BASE_QUESTION_DOCUMENTS = "knowledge_base_question_documents";
    private final String LAW_DOCUMENTS = "law_documents";


    /**
     * 向指定索引添加文档列表
     * @param indexName 索引名称
     * @param docs 要添加的文档列表
     * @param <T> 文档类型，必须为 QuestionTitleDocument
     * @throws MeilisearchException 如果添加过程中发生错误
     */
    public <T> void addDocuments(String indexName, List<T> docs) throws MeilisearchException {
        if (docs == null || docs.isEmpty()) {
            log.info("没有要写入索引 {} 的文档", indexName);
            return;
        }
        try {
            Index index = meiliSearchClient.index(indexName);
            String json = JSON.toJSONString(docs);
            
            log.info("准备向索引 {} 添加 {} 条文档", indexName, docs.size());
            TaskInfo taskInfo = index.addDocuments(json);
            
            log.info("文档已提交，taskUid={}, status={}", taskInfo.getTaskUid(), taskInfo.getStatus());
            
            // 等待任务完成
            log.info("正在等待 Meilisearch 索引任务完成...");
            meiliSearchClient.waitForTask(taskInfo.getTaskUid());
            
            // 获取任务详情
            Task task = meiliSearchClient.getTask(taskInfo.getTaskUid());
            log.info("任务完成: taskUid={}, status={}, type={}", 
                    task.getUid(), task.getStatus(), task.getType());
            
            // 检查任务状态
            if ("failed".equals(task.getStatus())) {
                log.error("索引任务失败: {}", task.getError());
                throw new RuntimeException("索引任务失败: " + task.getError());
            }
            
            // 验证文档数量
            IndexStats stats = index.getStats();
            log.info("✅ 成功添加文档到索引 {}，当前索引文档总数: {}", indexName, stats.getNumberOfDocuments());
            
        }
        catch (MeilisearchException e) {
            log.error("向索引 {} 添加文档失败", indexName, e);
            throw e;
        }
    }


    /**
     * 根据文档字段撒选文档，并排序返回符合条件的文档列表
     * @param indexName 索引名称
     * @param filter 筛选条件，例如 "primaryTag = 1"
     * @param sort 排序字段，例如 "count:desc"
     * @param limit 限制返回的文档数量，例如 10
     * @return 符合条件的文档列表
     * @throws MeilisearchException 如果查询过程中发生错误
     */
    public <T> List<T> searchFilteredAndSortedDocuments(String indexName, String[] filter, String[] sort, Integer limit, Class<T> clazz) throws MeilisearchException {
        if (indexName == null || indexName.trim().isEmpty()) {
            throw new IllegalArgumentException("索引名不能为 null 或空字符串");
        }

        try {
            Index index = meiliSearchClient.index(indexName);

            // 使用空查询，仅依赖 filter 与 sort
            SearchRequest req = new SearchRequest("");
            if (filter != null && filter.length > 0) {
                req.setFilter(filter);
            }
            if (sort != null && sort.length > 0) {
                req.setSort(sort);
            }
            // 设置默认 limit 为 100
            int limitInt = 100;
            if (limit != null && limit > 0) {
                limitInt = limit;
            }
            req.setLimit(limitInt);

            SearchResult raw = (SearchResult) index.search(req);
            if (raw == null || raw.getHits() == null || raw.getHits().isEmpty()) {
                return Collections.emptyList();
            }

            // 将 hits 转为 JSON 再反序列化为 List<T>，调用方可自行转换为具体类型
            String json = JSON.toJSONString(raw.getHits());
            return JSON.parseArray(json, clazz);
        }
        catch (MeilisearchException e) {
            log.error("查询索引 {} 失败，filter={}, sort={}, limit={}", indexName, filter, sort, limit, e);
            throw e;
        }
    }

    /**
     * 更新文档的 count 字段
     * @param indexName 索引名称
     * @param id 文档ID
     * @param count 新的 count 值
     * @throws MeilisearchException 如果更新过程中发生错误
     */
    public void updateCount(String indexName, Object id, Number count) throws MeilisearchException {
        if (id == null) {
            throw new IllegalArgumentException("id 不能为 null");
        }
        if (count == null) {
            throw new IllegalArgumentException("count 不能为 null");
        }
        if (indexName == null || indexName.trim().isEmpty()) {
            throw new IllegalArgumentException("索引名不能为 null 或空字符串");
        }

        Map<String, Object> updateDoc = new HashMap<>(2);
        updateDoc.put("id", id);
        updateDoc.put("count", count);
        String updateDocJson = JSON.toJSONString(updateDoc);
        Index index = meiliSearchClient.index(indexName);
        index.updateDocuments(updateDocJson, "id");
    }

    /**
     * 执行语义搜索（混合关键词+向量搜索）
     * @param query 搜索关键词
     * @return 符合条件的文档列表
     */
    public QuestionTitleDocument semanticSearchFromQuestionTitle(String query) throws MeilisearchException {
        Index index = meiliSearchClient.index(QUESTION_TITLE_DOCUMENTS);

        SearchRequest req = new SearchRequest(query);
        /*Hybrid hybrid = Hybrid.builder()
                .semanticRatio(0.9)
                .embedder("default")
                .build();
        req.setHybrid(hybrid);*/

        SearchResult raw = (SearchResult) index.search(req);
        if (raw == null || raw.getHits() == null || raw.getHits().isEmpty()) {
            return null;
        }
        Object first = raw.getHits().get(0);
        String jsonFirst = JSON.toJSONString(first);
        return JSON.parseObject(jsonFirst, QuestionTitleDocument.class);
    }

    /**
     * 执行模糊搜索（关键词搜索）
     * @param indexName 索引名称
     * @param query 搜索关键词
     * @param attributesToSearchOn 要搜索的属性数组
     * @param limit 限制返回的文档数量，例如 10
     * @return 符合条件的文档列表
     */
    public <T> List<T> fuzzySearchFromQuestionTitle(String indexName, String query, String[] attributesToSearchOn, Integer limit, Class<T> clazz) throws MeilisearchException {
        if (indexName == null || indexName.trim().isEmpty()) {
            throw new IllegalArgumentException("索引名不能为 null 或空字符串");
        }
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("查询关键词不能为 null 或空字符串");
        }
        if (attributesToSearchOn == null || attributesToSearchOn.length == 0) {
            throw new IllegalArgumentException("要搜索的属性数组不能为 null 或空数组");
        }
        if (limit == null || limit <= 0) {
            throw new IllegalArgumentException("limit 必须大于0");
        }
        try {
            Index index = meiliSearchClient.index(indexName);
            SearchRequest req = new SearchRequest(query);
            req.setAttributesToSearchOn(attributesToSearchOn);
            req.setLimit(limit);
  //          req.setMatchingStrategy(MatchingStrategy.FREQUENCY);
            SearchResult raw = (SearchResult) index.search(req);
            if (raw == null || raw.getHits() == null || raw.getHits().isEmpty()) {
                return Collections.emptyList();
            }

                String json = JSON.toJSONString(raw.getHits());
                return JSON.parseArray(json, clazz);
        } catch (Exception e) {
            log.error("执行模糊搜索失败，query={}, limit={}, clazz={}", query, limit, clazz, e);
            throw e;
        }
    }

    /**
     * 严格搜索：整词完全匹配 + 匹配词数量差距限制
     * @param query 搜索词（空格分隔的整词）
     * @param limit 返回数量
     * @param maxWordDiff 最大词数量差距（如2：查询5词，文档需3-7词）
     * @return 符合条件的文档
     */
    public List<KnowledgeBaseQuestionDocument> strictSearchForKnowledgeBaseQuestion(String query, int limit, int maxWordDiff) {
        try {
            // 关键：获取指定索引（knowledge_base_question_documents）
            Index index = meiliSearchClient.index("knowledge_base_question_documents");

            // 步骤1：分词并获取查询词列表
            List<String> queryWords = Arrays.asList(query.split(" "));
            int queryWordCount = queryWords.size();

            // 步骤2：使用精确匹配操作符（=）+ 短语匹配（""）
            String exactPhraseQuery = "\"" + String.join(" =", queryWords) + "=\"";

            // 步骤3：执行搜索
            SearchRequest request = new SearchRequest(exactPhraseQuery)
                    .setLimit(limit)
                    .setAttributesToSearchOn(new String[]{ "question_segmented" });

             SearchResult raw = (SearchResult) index.search(request);
            if (raw == null || raw.getHits() == null || raw.getHits().isEmpty()) {
                return Collections.emptyList();
            }
            List<KnowledgeBaseQuestionDocument> hits = JSON.parseArray(JSON.toJSONString(raw.getHits()), KnowledgeBaseQuestionDocument.class);

            // 步骤4：过滤结果：整词完全匹配 + 词数量差距限制
            List<KnowledgeBaseQuestionDocument> filtered = new ArrayList<>();
            for (KnowledgeBaseQuestionDocument doc : hits) {
                String docSegmented = doc.getQuestionSegmented();
                List<String> docWords = Arrays.asList(docSegmented.split(" "));
                int docWordCount = docWords.size();

                // 条件1：查询词必须全部出现在文档词中（整词匹配）
                boolean allWordsMatch = queryWords.stream().allMatch(docWords::contains);

                // 条件2：词数量差距不超过maxWordDiff
                boolean wordCountMatch = Math.abs(docWordCount - queryWordCount) <= maxWordDiff;

                if (allWordsMatch && wordCountMatch) {
                    filtered.add(doc);
                }
            }
            return filtered;
        } catch (Exception e) {
            log.error("严格搜索失败，索引：knowledge_base_question_documents", e);
            return Collections.emptyList();
        }
    }

    public KnowledgeBaseQuestionDocument getKnowledgeBaseQuestionDocumentById(String knowledgeBaseQuestionDocuments, Long questionId) {
        try {
            Index index = meiliSearchClient.index(knowledgeBaseQuestionDocuments);
            SearchRequest req = new SearchRequest(questionId.toString());
            req.setAttributesToSearchOn(new String[]{"questionId"});//设置可搜索字段
            req.setLimit(1);

            SearchResult raw = (SearchResult) index.search(req);
            if (raw == null || raw.getHits() == null || raw.getHits().isEmpty()) {
                return null;
            }

            String json = JSON.toJSONString(raw.getHits().get(0));
            return JSON.parseObject(json, KnowledgeBaseQuestionDocument.class);
        } catch (Exception e) {
            log.error("搜索知识库问题文档失败，questionId={}", questionId, e);
            throw e;
        }
    }


    /**
     * 检查索引是否存在
     * @param indexName 索引名称
     * @return 如果索引存在则返回true，否则返回false
     * @throws MeilisearchException 如果检查过程中发生错误
     */
    public boolean isIndexExists(String indexName) throws MeilisearchException {
        Results<Index> indexes = meiliSearchClient.getIndexes(); // 获取所有索引
        if (indexes == null || indexes.getResults() == null) {
            return false;
        }
        for (Index index : indexes.getResults()) {
            if (index.getUid().equals(indexName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 初始化问题排名索引
     * 配置索引字段为可搜索、可过滤、可排序
     * @throws IOException 如果初始化过程中发生IO错误
     */
    private void initQuestionRankingIndex() throws IOException, InterruptedException {
        try {
            if (isIndexExists(QUESTION_TITLE_DOCUMENTS)) {
                log.info("索引 {} 已存在，无需重复创建", QUESTION_TITLE_DOCUMENTS);
                // 删除索引
                //meiliSearchClient.deleteIndex(QUESTION_TITLE_DOCUMENTS);
                //Thread.sleep(500);
                return;
            }
        }
        catch (MeilisearchException e) {
            throw new RuntimeException(e);
        }
        try {
            meiliSearchClient.createIndex(QUESTION_TITLE_DOCUMENTS);
            Index index = meiliSearchClient.index(QUESTION_TITLE_DOCUMENTS);
            Settings settings = new Settings();

            settings.setSearchableAttributes(new String[]{ "title_segmented" });
            settings.setFilterableAttributes(new String[]{ "primaryTag", "count" });
            settings.setSortableAttributes(new String[]{ "count" });

            TypoTolerance typoTolerance = new TypoTolerance();
            HashMap<String, Integer> minWordSizeTypos =
                    new HashMap<String, Integer>() {
                        {
                            put("oneTypo", 3);
                            put("twoTypos", 6);
                        }
                    };

            typoTolerance.setMinWordSizeForTypos(minWordSizeTypos);

            TaskInfo taskInfo = index.updateSettings(settings);
            meiliSearchClient.waitForTask(taskInfo.getTaskUid());
            Settings currentSettings = index.getSettings();
            System.out.println("当前可搜索属性: " + Arrays.toString(currentSettings.getSearchableAttributes()));

            Map<String, Object> testDoc = new HashMap<>();
            testDoc.put("id", "001");
            testDoc.put("title", "对于企业和劳动者双方来说，《劳动合同法》更侧重保护谁的权益呢？");
            testDoc.put("title_segmented", IKPreprocessorUtil.segment("对于企业和劳动者双方来说，《劳动合同法》更侧重保护谁的权益呢？", true));
            testDoc.put("primaryTag", 4);
            testDoc.put("count", 1);
            testDoc.put("create_time", LocalDateTime.now());
            index.addDocuments(JSON.toJSONString(Collections.singletonList(testDoc)));
            log.info("已插入测试文档，确保 title_segmented 字段可搜索");

            log.info("索引 {} 创建并配置成功", QUESTION_TITLE_DOCUMENTS);
        }
        catch (MeilisearchException e) {
            log.error("创建索引失败", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 初始化知识库问题索引
     * @throws Exception
     */
    private void initKnowledgeBaseQuestionIndex() throws Exception {
        try {
            if (isIndexExists(KNOWLEDGE_BASE_QUESTION_DOCUMENTS)) {
                log.info("索引 {} 已存在，无需重复创建", KNOWLEDGE_BASE_QUESTION_DOCUMENTS);
                // 删除索引
                //meiliSearchClient.deleteIndex(KNOWLEDGE_BASE_QUESTION_DOCUMENTS);
                //Thread.sleep(500);
                return;
            }
            Index index = meiliSearchClient.index(KNOWLEDGE_BASE_QUESTION_DOCUMENTS);
            Thread.sleep(300);
            Settings settings = new Settings();
            settings.setSearchableAttributes(new String[]{ "question_segmented", "questionId" });
            settings.setFilterableAttributes(new String[]{ "count" });
            settings.setSortableAttributes(new String[]{ "count" });

            TypoTolerance typoTolerance = new TypoTolerance();
            typoTolerance.setEnabled(false);
//            HashMap<String, Integer> minWordSizeTypos = new HashMap<String, Integer>() {{
//                put("oneTypo", 5);
//                put("twoTypos", 8);
//            }};
//            typoTolerance.setMinWordSizeForTypos(minWordSizeTypos);
//            typoTolerance.setDisableOnWords(Arrays.asList("劳动", "企业", "劳动者", "劳动法").toArray(new String[0]));
            settings.setTypoTolerance(typoTolerance);

            settings.setRankingRules(new String[]{
                    "exactness",        // 完全匹配（最高权重，只有完全一致的词才高分）
                    "proximity",        // 词序邻近度（词的顺序越接近，权重越高）
                    "words",            // 匹配词数（仅当词序接近时才生效）
                    "attribute",        // 字段权重
                    "sort"             // 自定义排序
            });
            settings.setProximityPrecision("byWord"); // 按单词粒度计算词序邻近度

            TaskInfo taskInfo = index.updateSettings(settings);
            meiliSearchClient.waitForTask(taskInfo.getTaskUid());

            log.info("索引 {} 创建并配置成功", KNOWLEDGE_BASE_QUESTION_DOCUMENTS);

            //创建测试文档
            Map<String, Object> testDoc = new HashMap<>();
            testDoc.put("id", "001");
            testDoc.put("question", "对于企业和劳动者双方来说，《劳动合同法》更侧重保护谁的权益呢？");
            testDoc.put("question_segmented", IKPreprocessorUtil.stopWordSegment("对于企业和劳动者双方来说，《劳动合同法》更侧重保护谁的权益呢？", true));
            testDoc.put("topic", "合同权益");
            testDoc.put("case_background", "企业与劳动者签订劳动合同");
            testDoc.put("count", 1);
       //     testDoc.put("questionId", 0L);
            testDoc.put("create_time", LocalDateTime.now());
            index.addDocuments(JSON.toJSONString(Collections.singletonList(testDoc)));
            log.info("已插入测试文档，确保 question_segmented，questionId 字段可搜索");
        }
        catch (MeilisearchException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 初始化法律法规索引
     * 配置索引字段为可搜索、可过滤、可排序
     */
    public void initLawIndex() throws Exception {
        try {
            if (isIndexExists(LAW_DOCUMENTS)) {
                log.info("索引 {} 已存在，无需重复创建", LAW_DOCUMENTS);
                return;
            }
        } catch (MeilisearchException e) {
            throw new RuntimeException(e);
        }
        
        try {
            meiliSearchClient.createIndex(LAW_DOCUMENTS);
            Index index = meiliSearchClient.index(LAW_DOCUMENTS);
            Settings settings = new Settings();

            // 设置可搜索字段：法条原文分词、法律名称
            settings.setSearchableAttributes(new String[]{ "originalTextSegmented", "lawName" });
            // 设置可过滤字段：法律分类ID、条款号、发布年份
            settings.setFilterableAttributes(new String[]{ "lawCategoryId", "articleNumber", "issueYear" });
            // 设置可排序字段：条款号
            settings.setSortableAttributes(new String[]{ "articleNumber" });

            // 配置容错
            TypoTolerance typoTolerance = new TypoTolerance();
            HashMap<String, Integer> minWordSizeTypos = new HashMap<String, Integer>() {{
                put("oneTypo", 3);
                put("twoTypos", 6);
            }};
            typoTolerance.setMinWordSizeForTypos(minWordSizeTypos);

            TaskInfo taskInfo = index.updateSettings(settings);
            meiliSearchClient.waitForTask(taskInfo.getTaskUid());
            
            log.info("索引 {} 创建并配置成功", LAW_DOCUMENTS);
        } catch (MeilisearchException e) {
            log.error("创建法律索引失败", e);
            throw e;
        }
    }

    /**
     * 搜索法律法规
     * @param query 搜索关键词（已分词）
     * @param lawCategoryId 法律分类ID（可选）
     * @param limit 返回数量
     * @return 法律文档列表
     */
    public <T> List<T> searchLawDocuments(String query, Integer lawCategoryId, Integer limit, Class<T> clazz) throws MeilisearchException {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("查询关键词不能为 null 或空字符串");
        }
        
        try {
            Index index = meiliSearchClient.index(LAW_DOCUMENTS);
            SearchRequest req = new SearchRequest(query);
            req.setAttributesToSearchOn(new String[]{"originalTextSegmented", "lawName"});//设置可搜索字段
            
            // 如果指定了法律分类，添加过滤条件
            if (lawCategoryId != null) {
                req.setFilter(new String[]{"lawCategoryId = " + lawCategoryId});
            }
            
            req.setLimit(limit != null && limit > 0 ? limit : 10);
            
            SearchResult raw = (SearchResult) index.search(req);
            if (raw == null || raw.getHits() == null || raw.getHits().isEmpty()) {
                return Collections.emptyList();
            }

            String json = JSON.toJSONString(raw.getHits());
            return JSON.parseArray(json, clazz);
        } catch (Exception e) {
            log.error("搜索法律文档失败，query={}, lawCategoryId={}, limit={}", query, lawCategoryId, limit, e);
            throw e;
        }
    }

    @Override
    public void run(String... args) throws Exception {
        initQuestionRankingIndex();
        initLawIndex();
        initKnowledgeBaseQuestionIndex();
    }
}
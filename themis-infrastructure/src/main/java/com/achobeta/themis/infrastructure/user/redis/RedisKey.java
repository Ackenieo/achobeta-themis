package com.achobeta.themis.infrastructure.user.redis;

/**
 * @author Aseubel
 * @date 2025/6/28 下午9:22
 */
public class RedisKey {
    public static final String APP = "achobeta-themis:";

    public static final String REDIS_SMS_CODE_PREFIX = "sms:code:";
    
    // 知识库搜索历史记录相关
    public static final String KNOWLEDGE_SEARCH_LIST_PREFIX = "knowledge_search:list:";
    public static final String KNOWLEDGE_SEARCH_DETAIL_PREFIX = "knowledge_search:detail:";
    
    // 法条收藏相关
    public static final String LEGAL_ARTICLE_FAVORITE_LIST_PREFIX = "legal_article_favorite:list:";
    public static final String LEGAL_ARTICLE_FAVORITE_DETAIL_PREFIX = "legal_article_favorite:detail:";

}

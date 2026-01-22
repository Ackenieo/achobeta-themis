package com.achobeta.themis.infrastructure.chat.repo;

import com.achobeta.themis.common.redis.service.IRedisService;
import com.achobeta.themis.domain.chat.model.entity.ConversationMeta;
import com.achobeta.themis.domain.chat.service.IConversationHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConversationHistoryServiceImpl implements IConversationHistoryService {

    private static final long TTL_MS = Duration.ofDays(1).toMillis();

    private final IRedisService redis;

    private String historyMapKey(String userId) {
        // 使用 Redis 哈希表存储历史对话列表
        // 键：chat:user:{userId}:convs
        // 字段（field）：conversationId
        // 值（value）：最后更新时间 ISO 字符串
        return "chat:user:" + userId + ":convs";
    }

    private String metaKey(String conversationId) {
        // 对话元信息键：chat:conv:{conversationId}:meta
        return "chat:conv:" + conversationId + ":meta";
    }

    @Override
    public String startNewConversation(String userId, String currentConversationId,String title) {
        // 若存在当前对话，则先归档到历史
        if (currentConversationId != null && !currentConversationId.isBlank()) {
            archiveIfAbsent(userId, currentConversationId, title);
        }
        // 生成新的会话 ID，并写入基础元信息
        String newId = java.util.UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        String mk = metaKey(newId);
        redis.addToMap(mk, "userId", String.valueOf(userId));
        redis.addToMap(mk, "createdAt", now.toString());
        redis.addToMap(mk, "updatedAt", now.toString());
        redis.setMapExpired(mk, TTL_MS);
        // 新对话不立即加入历史，首次聊天或 touch 时加入
        return newId;
    }

    @Override
    public void archiveIfAbsent(String userId, String conversationId, String title) {
        // 确保元信息存在
        String mk = metaKey(conversationId);
        if (redis.getFromMap(mk, "userId") == null) {
            redis.addToMap(mk, "userId", String.valueOf(userId));
            redis.addToMap(mk, "createdAt", LocalDateTime.now().toString());
        }
        if (title != null && !title.isBlank()) {
            redis.addToMap(mk, "title", title);
        }
        redis.addToMap(mk, "updatedAt", LocalDateTime.now().toString());
        redis.setMapExpired(mk, TTL_MS);

        // 写入用户历史对话列表（哈希结构），并续期
        String hk = historyMapKey(userId);
        redis.addToMap(hk, conversationId, LocalDateTime.now().toString());
        redis.setMapExpired(hk, TTL_MS);
    }

    @Override
    public void touch(String userId, String conversationId) {
        // 触碰：更新元信息时间并续期
        String mk = metaKey(conversationId);
        if (redis.getFromMap(mk, "userId") == null) {
            redis.addToMap(mk, "userId", String.valueOf(userId));
            redis.addToMap(mk, "createdAt", LocalDateTime.now().toString());
        }
        redis.addToMap(mk, "updatedAt", LocalDateTime.now().toString());
        redis.setMapExpired(mk, TTL_MS);

        // 确保出现在用户历史对话列表，并续期
        String hk = historyMapKey(userId);
        redis.addToMap(hk, conversationId, LocalDateTime.now().toString());
        redis.setMapExpired(hk, TTL_MS);
    }

    @Override
    public List<ConversationMeta> listHistories(String userId) {
        // 读取用户历史对话列表（哈希的全部键值对）
        String hk = historyMapKey(userId);
        Map<String, String> map = redis.getMapToJavaMap(hk);
        List<ConversationMeta> list = new ArrayList<>();
        if (map != null) {
            for (Map.Entry<String, String> e : map.entrySet()) {
                ConversationMeta meta = getMeta(e.getKey());
                if (meta != null) {
                    list.add(meta);
                }
            }
        }
        // 按更新时间倒序
        list.sort(Comparator.comparing((ConversationMeta m) -> m.updatedAt).reversed());
        return list;
    }

    @Override
    public ConversationMeta getMeta(String conversationId) {
        // 从元信息哈希表中组装返回对象
        Map<String, String> map = redis.getMapToJavaMap(metaKey(conversationId));
        if (map == null || map.isEmpty()) return null;
        ConversationMeta m = new ConversationMeta();
        m.conversationId = conversationId;
        m.userId = map.get("userId") != null ? String.valueOf(map.get("userId")) : null;
        m.title = map.get("title");
        m.createdAt = map.get("createdAt") != null ? LocalDateTime.parse(map.get("createdAt")) : null;
        m.updatedAt = map.get("updatedAt") != null ? LocalDateTime.parse(map.get("updatedAt")) : null;
        return m;
    }
}

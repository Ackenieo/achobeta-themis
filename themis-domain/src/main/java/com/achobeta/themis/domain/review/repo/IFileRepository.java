package com.achobeta.themis.domain.review.repo;

import com.achobeta.themis.domain.review.model.entity.FileRecord;

public interface IFileRepository {
    FileRecord findByConversationIdAndContentType(String conversationId, String contentType);

    void updateById(FileRecord fileRecord);

    void insert(FileRecord fileRecord);
}

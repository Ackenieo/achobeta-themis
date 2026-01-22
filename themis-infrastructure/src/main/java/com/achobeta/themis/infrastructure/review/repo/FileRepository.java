package com.achobeta.themis.infrastructure.review.repo;

import com.achobeta.themis.domain.review.model.entity.FileRecord;
import com.achobeta.themis.domain.review.repo.IFileRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import com.achobeta.themis.infrastructure.review.mapper.FileMapper;

@Repository
@RequiredArgsConstructor
public class FileRepository implements IFileRepository {
    private final FileMapper fileMapper;

    @Override
    public FileRecord findByConversationIdAndContentType(String conversationId, String contentType) {
        return fileMapper.selectOne(new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getConversationId, conversationId)
                .eq(FileRecord::getFileType, contentType));
    }

    @Override
    public void updateById(FileRecord fileRecord) {
        fileMapper.updateById(fileRecord);
    }

    @Override
    public void insert(FileRecord fileRecord) {
        fileMapper.insert(fileRecord);
    }
}

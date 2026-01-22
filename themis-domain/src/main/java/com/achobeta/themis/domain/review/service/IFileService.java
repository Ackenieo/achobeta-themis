package com.achobeta.themis.domain.review.service;

import org.springframework.core.io.Resource;

public interface IFileService {
    /**
     * 生成文件下载url
     * @param isChange
     * @param conversationId
     * @param contentType
     * @param content
     * @return
     */
     Resource generateDownloadResource(Boolean isChange, String conversationId, String contentType, String content);

    /**
     * 内容转换 (把“\\n”替换为“\n”)
     * @param content
     * @return
     */
    String contentTransform(String content);
}

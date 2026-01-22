package com.achobeta.themis.domain.review.model.entity;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.*;

/**
 * Spring Boot 3 推荐的 MultipartFile 实现
 * 基于 Spring 6 的 Resource 接口
 */
public class ResourceMultipartFile implements MultipartFile {

    private final Resource resource;
    private final String originalFilename;
    private final String contentType;
    private final String name;

    public ResourceMultipartFile(byte[] content, String originalFilename, String contentType) {
        this(new ByteArrayResource(content), originalFilename, contentType, "file");
    }

    public ResourceMultipartFile(Resource resource, String originalFilename, String contentType, String name) {
        this.resource = resource;
        this.originalFilename = StringUtils.hasText(originalFilename) ?
                originalFilename : "unknown";
        this.contentType = StringUtils.hasText(contentType) ?
                contentType : "application/octet-stream";
        this.name = StringUtils.hasText(name) ? name : "file";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        try {
            return resource.contentLength() == 0;
        } catch (IOException e) {
            return true;
        }
    }

    @Override
    public long getSize() {
        try {
            return resource.contentLength();
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public byte[] getBytes() throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return inputStream.readAllBytes();
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return resource.getInputStream();
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        if (dest.exists() && !dest.delete()) {
            throw new IOException("无法删除已存在的文件: " + dest.getAbsolutePath());
        }
        try (InputStream in = resource.getInputStream();
             OutputStream out = new FileOutputStream(dest)) {
            in.transferTo(out);
        }
    }
}
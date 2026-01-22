package com.achobeta.themis.domain.review.model.entity;

import com.achobeta.themis.common.annotation.FieldDesc;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileRecord {
    @FieldDesc(name = "文件ID")
    @TableId(type = IdType.AUTO)
    private Long id;
    @FieldDesc(name = "会话ID")
    private String conversationId;
    @FieldDesc(name = "用户ID")
    private Long userId;
    @FieldDesc(name = "文件名")
    private String fileName;
    @FieldDesc(name = "文件OSS路径")
    private String fileOssPath;
    @FieldDesc(name = "文件类型")
    private String fileType;
    @FieldDesc(name = "创建时间")
    private LocalDateTime createTime;
}
package com.achobeta.themis.domain.chat.model.entity;

import com.achobeta.themis.common.annotation.FieldDesc;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@TableName("question_regulation_relations")
public class QuestionRegulationRelations {
        @FieldDesc(name = "关系ID")
        @TableId(value = "id", type = IdType.AUTO)
        private Long id;
        @FieldDesc(name = "问题ID")
        private Long questionId;
        @FieldDesc(name = "法规ID")
        private Long regulationId;
        @FieldDesc(name = "AI翻译")
        private String aiTranslation;
        @FieldDesc(name = "关联案例")
        private String relevantCases;
        @FieldDesc(name = "关联问题")
        private String relevantQuestions;
        @FieldDesc(name = "关联法条")
        private String relevantRegulations;
        @FieldDesc(name = "创建时间")
        private LocalDateTime createTime;
        @FieldDesc(name = "更新时间")
        private LocalDateTime updateTime;
}

package com.achobeta.themis.domain.user.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuestionTitleResponseVO {
    private String title;
    private String primaryTag;
}

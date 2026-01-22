package com.achobeta.themis.domain.laws.model.vo;

import com.achobeta.themis.domain.laws.model.entity.LawCategory;
import com.achobeta.themis.domain.laws.model.entity.LawRegulation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 法律分类及其关联法条响应VO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LawCategoryWithRegulationsVO {
    /**
     * 法律分类ID
     */
    private Integer lawId;

    /**
     * 法律分类名称
     */
    private String lawName;

    /**
     * 法律类型：1-国家法规，0-地方法规
     */
    private Integer categoryType;

    /**
     * 关联的法律条文列表
     */
    private List<RegulationDetailVO> regulations;

    /**
     * 法律条文总数
     */
    private Integer totalCount;

    /**
     * 法律条文详情
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RegulationDetailVO {
        /**
         * 法律条文ID
         */
        private Integer regulationId;

        /**
         * 发布年月日
         */
        private String issueYear;

        /**
         * 条款号（第n条）
         */
        private Integer articleNumber;

        /**
         * 法条原文
         */
        private String originalText;

        public static RegulationDetailVO of(LawRegulation reg) {
            return LawCategoryWithRegulationsVO.RegulationDetailVO.builder()
                    .regulationId(Math.toIntExact(reg.getRegulationId()))
                    .issueYear(reg.getIssueYear())
                    .articleNumber(reg.getArticleNumber())
                    .originalText(reg.getOriginalText())
                    .build();
        }
    }

    public static LawCategoryWithRegulationsVO of(LawCategory category, List<LawRegulation> regulations) {
        // 转换为VO
        List<LawCategoryWithRegulationsVO.RegulationDetailVO> regulationVOs = new ArrayList<>();
        if (regulations != null && !regulations.isEmpty()) {
            regulationVOs = regulations.stream()
                    .map(LawCategoryWithRegulationsVO.RegulationDetailVO::of)
                    .collect(Collectors.toList());
        }

        return LawCategoryWithRegulationsVO.builder()
                .lawId(Math.toIntExact(category.getLawId()))
                .lawName(category.getLawName())
                .categoryType(category.getCategoryType())
                .regulations(regulationVOs)
                .totalCount(regulationVOs.size())
                .build();
    }

}

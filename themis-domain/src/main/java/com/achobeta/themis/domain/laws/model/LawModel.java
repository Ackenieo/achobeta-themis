package com.achobeta.themis.domain.laws.model;

import com.achobeta.themis.domain.laws.model.vo.LawCategoryWithRegulationsVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Aseubel
 * @date 2025/12/8 下午18:51
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LawModel {
    Long lawId;
    Integer categoryType;
    Integer articleNumber;
    List<LawCategoryWithRegulationsVO> lawCategoryWithRegulationsVOS;
    Integer total;
    Integer page;
    Integer size;
}

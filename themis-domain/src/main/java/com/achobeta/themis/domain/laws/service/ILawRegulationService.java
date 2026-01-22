package com.achobeta.themis.domain.laws.service;

import com.achobeta.themis.domain.laws.model.LawModel;
import com.achobeta.themis.domain.laws.model.entity.LawRegulation;
import com.achobeta.themis.domain.laws.model.vo.LawCategoryWithRegulationsVO;

import java.util.List;

/**
 * 法律条文服务接口
 */
public interface ILawRegulationService {

    /**
     * 查询法律条文列表
     * @return 法律条文列表
     */
    List<LawRegulation> queryLawRegulationList();

    /**
     * 根据条件查询法律条文列表
     * @param categoryType 分类类型
     * @param page 页码
     * @param size 页大小
     * @return 法律条文列表
     */
    LawModel queryLawsByCondition(Integer categoryType, Integer page, Integer size);

    /**
     * 查询具体法条
     * 根据法律分类ID和条款号查询指定法条及其所属法律的所有条款
     *
     * @param lawId 法律分类ID
     * @param articleNumber 条款号（第n条）
     * @return 法律分类及其所有条款信息，当前查询的条款会包含在列表中
     */
    LawCategoryWithRegulationsVO queryLawsByCondition(Integer lawId, Integer articleNumber);
}

package com.achobeta.themis.domain.laws.service;

import com.achobeta.themis.domain.laws.model.entity.LawCategory;

import java.util.List;

/**
 * 法律类别服务接口
 */
public interface ILawCategoryService {

    /**
     * 查询法律类别列表
     * @return 法律类别列表
     */
    List<LawCategory> queryLawCategoryList();

}

package com.achobeta.themis.domain.laws.repo;

import com.achobeta.themis.domain.laws.model.entity.LawCategory;

import java.util.List;

/**
 * 法律类别仓储层接口
 */
public interface ILawCategoryRepository {

    /**
     * 获取所有法律类别列表
     * @return 所有法律类别列表
     */
    List<LawCategory> listLawCategories();

    /**
     * 根据ID获取法律类别
     * @param id 法律类别ID
     * @return 法律类别
     */
    LawCategory getById(Long id);

    /**
     * 保存法律类别
     * @param lawCategory 法律类别
     */
    void save(LawCategory lawCategory);

    /**
     * 根据ID删除法律类别
     * @param id 法律类别ID
     */
    void delete(Long id);

    /**
     * 根据条件查询法律类别列表
     * @param categoryType 法律类别类型
     * @param page 页码
     * @param size 每页数量
     * @return 法律类别列表
     */
    List<LawCategory> listLawCategoriesConditional(Long categoryType, Integer page, Integer size);
}

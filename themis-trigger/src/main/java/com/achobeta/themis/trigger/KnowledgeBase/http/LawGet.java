package com.achobeta.themis.trigger.KnowledgeBase.http;

import com.achobeta.themis.common.ApiResponse;
import com.achobeta.themis.common.PageResponse;
import com.achobeta.themis.common.exception.BusinessException;
import com.achobeta.themis.domain.laws.model.LawModel;
import com.achobeta.themis.domain.laws.model.vo.LawCategoryWithRegulationsVO;
import com.achobeta.themis.domain.laws.service.ILawRegulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 法律分类查询Controller
 * 提供国家法规和地方法规的查询功能
 *
 * @Author: ZGjie20
 * @version: 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/law")
@RequiredArgsConstructor
public class LawGet {

    private final ILawRegulationService lawRegulationService;

    /**
     * 查询所有国家法规及其关联的法律条款（分页）
     * 
     * @param page 页码（从1开始，默认1）
     * @param size 每页大小（默认10）
     * @return 国家法规分页列表（category_type=1）
     */
    @GetMapping("/national-laws")
    public ApiResponse<PageResponse<LawCategoryWithRegulationsVO>> getNationalLaws(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size
    ) {
        try {
            log.info("查询国家法规 - 页码: {}, 每页大小: {}", page, size);
            PageResponse<LawCategoryWithRegulationsVO> result = getLawsByCategoryType(1, page, size);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("查询国家法规失败", e);
            return ApiResponse.error("查询国家法规失败: " + e.getMessage());
        }
    }

    /**
     * 查询所有地方法规及其关联的法律条款（分页）
     * 
     * @param page 页码（从1开始，默认1）
     * @param size 每页大小（默认10）
     * @return 地方法规分页列表（category_type=0）
     */
    @GetMapping("/local-laws")
    public ApiResponse<PageResponse<LawCategoryWithRegulationsVO>> getLocalLaws(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size
    ) {
        try {
            log.info("查询地方法规 - 页码: {}, 每页大小: {}", page, size);
            PageResponse<LawCategoryWithRegulationsVO> result = getLawsByCategoryType(0, page, size);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("查询地方法规失败", e);
            return ApiResponse.error("查询地方法规失败: " + e.getMessage());
        }
    }

    /**
     * 根据category_type值查询法律分类及其关联的法律条款（分页）
     * 
     * @param categoryType 1-国家法规，0-地方法规
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 法律分类分页结果
     */
    private PageResponse<LawCategoryWithRegulationsVO> getLawsByCategoryType(Integer categoryType, Integer page, Integer size) {
        LawModel lawModel = lawRegulationService.queryLawsByCondition(categoryType, page, size);
        List<LawCategoryWithRegulationsVO> result = lawModel.getLawCategoryWithRegulationsVOS();
        Integer total = lawModel.getTotal();
        
        // 返回分页结果
        return PageResponse.of(result, page, size, Long.valueOf(total));
    }
    /**
     * 查询具体法条
     * 根据法律分类ID和条款号查询指定法条及其所属法律的所有条款
     * 
     * @param lawId 法律分类ID
     * @param articleNumber 条款号（第n条）
     * @return 法律分类及其所有条款信息，当前查询的条款会包含在列表中
     */
    @GetMapping("/law-regulation")
    public ApiResponse<LawCategoryWithRegulationsVO> queryLawRegulation(
            @RequestParam(value = "lawId") Integer lawId,
            @RequestParam(value = "articleNumber") Integer articleNumber
    ) {
        try {
            log.info("查询具体法条 - 法律ID: {}, 条款号: {}", lawId, articleNumber);

            LawCategoryWithRegulationsVO result = lawRegulationService.queryLawsByCondition(lawId, articleNumber);
            
            log.info("成功查询法条 - 法律: {}, 条款号: {}, 总条款数: {}",
                result.getLawName(), articleNumber, result.getTotalCount());
            
            return ApiResponse.success(result);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("查询具体法条失败 - 法律ID: {}, 条款号: {}", lawId, articleNumber, e);
            return ApiResponse.error("查询具体法条失败: " + e.getMessage());
        }
    }

}

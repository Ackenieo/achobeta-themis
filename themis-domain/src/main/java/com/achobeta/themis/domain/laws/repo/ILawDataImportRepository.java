package com.achobeta.themis.domain.laws.repo;

/**
 * 法律数据导入仓储接口
 */
public interface ILawDataImportRepository {
    /**
     * 从数据库导入法律数据到 Meilisearch
     * @return 导入的文档数量
     */
    int importLawDataFromDatabase() throws Exception;
}

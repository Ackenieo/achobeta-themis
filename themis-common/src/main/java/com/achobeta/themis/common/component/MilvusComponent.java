package com.achobeta.themis.common.component;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.highlevel.dml.InsertRowsParam;
import io.milvus.param.highlevel.dml.response.InsertResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Milvus向量数据库操作组件
 * 提供向量数据的插入等基础操作
 */
@Slf4j
@Component
public class MilvusComponent {

    @Autowired
    private MilvusServiceClient milvusServiceClient;

    private static final String LAW_REGULATION_COLLECTION = "s_law_lib";
    private final Gson gson = new Gson();

    /**
     * 插入法律法规向量数据到Milvus
     * 用于测试 TODO
     * @param rows 待插入的JsonObject列表
     * @return 插入响应结果
     */
    public InsertResponse insertLawRegulationVector(List<JsonObject> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            throw new IllegalArgumentException("插入数据不能为空");
        }

        try {
            log.info("开始向Milvus插入{}条法律法规向量数据", rows.size());

            InsertRowsParam insertParam = InsertRowsParam.newBuilder()
                    .withCollectionName(LAW_REGULATION_COLLECTION)
                    .withRows(rows)
                    .build();

            InsertResponse response = milvusServiceClient.insert(insertParam).getData();

            log.info("成功插入{}条向量数据到集合: {}", response.getInsertIds(), LAW_REGULATION_COLLECTION);
            return response;

        } catch (Exception e) {
            log.error("Milvus插入向量数据失败，集合名: {}, 数据量: {}", LAW_REGULATION_COLLECTION, rows.size(), e);
            // 替换为你的项目自定义技术异常（全局异常处理器统一捕获）
            throw new RuntimeException("Milvus插入向量数据失败", e);
        }
    }

    /**
     * 从Java Bean列表导入数据到Milvus
     *
     * @param dataList Java Bean对象列表
     * @param <T> Bean类型
     * @return 插入响应结果
     */
    public <T> InsertResponse importFromBean(List<T> dataList) {
        if (CollectionUtils.isEmpty(dataList)) {
            throw new IllegalArgumentException("待导入的数据列表为空");
        }

        try {
            // 1. 将Java Bean转换为JsonObject列表
            List<JsonObject> milvusRows = convertToJsonObjectList(dataList);

            // 2. 判空：避免转换后数据为空
            if (CollectionUtils.isEmpty(milvusRows)) {
                throw new IllegalArgumentException("转换后的有效数据为空");
            }

            log.info("成功将{}个Bean对象转换为Milvus数据格式", milvusRows.size());

            // 3. 调用底层方法完成插入
            return this.insertLawRegulationVector(milvusRows);

        } catch (Exception e) {
            log.error("从Bean导入Milvus数据失败，数据量: {}", dataList.size(), e);
            throw new RuntimeException("从Bean导入Milvus数据失败", e);
        }
    }

    /**
     * 将Java Bean列表转换为JsonObject列表
     * 使用Gson进行序列化和反序列化
     *
     * @param dataList Bean对象列表
     * @param <T> Bean类型
     * @return JsonObject列表
     */
    private <T> List<JsonObject> convertToJsonObjectList(List<T> dataList) {
        List<JsonObject> result = new ArrayList<>(dataList.size());

        for (T data : dataList) {
            try {
                // 使用Gson将Bean转为JsonObject
                String json = gson.toJson(data);
                JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                result.add(jsonObject);
            } catch (Exception e) {
                log.warn("转换Bean到JsonObject失败，跳过该数据: {}", data, e);
                // 可以选择跳过错误数据或抛出异常，根据业务需求决定
            }
        }

        return result;
    }

    /**
     * 通用插入方法 - 支持指定集合名称
     *
     * @param collectionName 集合名称
     * @param rows 待插入的JsonObject列表
     * @return 插入响应结果
     */
    public InsertResponse insertVector(String collectionName, List<JsonObject> rows) {
        if (CollectionUtils.isEmpty(rows)) {
            throw new IllegalArgumentException("插入数据不能为空");
        }

        try {
            log.info("开始向Milvus插入{}条向量数据到集合: {}", rows.size(), collectionName);

            InsertRowsParam insertParam = InsertRowsParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withRows(rows)
                    .build();

            InsertResponse response = milvusServiceClient.insert(insertParam).getData();

            log.info("成功插入{}条向量数据到集合: {}", response.getInsertIds(), collectionName);
            return response;

        } catch (Exception e) {
            log.error("Milvus插入向量数据失败，集合名: {}, 数据量: {}", collectionName, rows.size(), e);
            throw new RuntimeException("Milvus插入向量数据失败到集合: " + collectionName, e);
        }
    }
}
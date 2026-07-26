package io.github.onedream921.alphavue.common.api;

import java.util.List;

/**
 * 分页响应体
 *
 * @param records 当前页数据
 * @param total 匹配记录总数
 * @param page 从 1 开始的页码
 * @param size 请求的每页数量
 */
public record PageResponse<T>(List<T> records, long total, int page, int size) {

    public PageResponse {
        records = List.copyOf(records);
    }
}

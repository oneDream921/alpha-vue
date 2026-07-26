package io.github.onedream921.alphavue.modules.log.dto;

/**
 * 操作日志查询条件
 */
public record OperationLogQuery(String keyword, Integer status, Integer handlingStatus) {
}

package io.github.onedream921.alphavue.modules.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.onedream921.alphavue.modules.log.dto.OperationLogQuery;
import io.github.onedream921.alphavue.modules.log.entity.SysOperLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志数据访问 Mapper
 */
@Mapper
public interface SysOperLogMapper extends BaseMapper<SysOperLog> {
    /**
     * 按条件分页查询操作日志
     */
    Page<SysOperLog> selectPageByQuery(Page<SysOperLog> page, @Param("query") OperationLogQuery query);
    /**
     * 更新失败日志的处理状态
     */
    int updateHandlingStatus(@Param("id") long id, @Param("handlingStatus") int handlingStatus,
                             @Param("handledBy") long handledBy);

    SysOperLog selectByIdForDetail(@Param("id") long id);

    /**
     * Selects a bounded batch of operation log ids that are safe to delete.
     */
    List<Long> selectExpiredHandledIds(@Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);
}

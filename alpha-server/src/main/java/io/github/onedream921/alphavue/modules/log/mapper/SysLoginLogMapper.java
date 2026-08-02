package io.github.onedream921.alphavue.modules.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.onedream921.alphavue.modules.log.entity.SysLoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录日志数据访问 Mapper
 */
@Mapper
public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {

    /**
     * Selects a bounded batch of login log ids older than the retention cutoff.
     */
    List<Long> selectExpiredIds(@Param("cutoff") LocalDateTime cutoff, @Param("limit") int limit);
}

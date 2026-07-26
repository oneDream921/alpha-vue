package io.github.onedream921.alphavue.modules.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.onedream921.alphavue.modules.log.entity.SysLoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录日志数据访问 Mapper
 */
@Mapper
public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {
}

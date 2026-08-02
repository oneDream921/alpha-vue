package io.github.onedream921.alphavue.modules.log.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.modules.log.entity.SysLoginLog;
import io.github.onedream921.alphavue.modules.log.entity.SysOperLog;
import io.github.onedream921.alphavue.modules.log.dto.OperationLogQuery;
import io.github.onedream921.alphavue.modules.log.mapper.SysLoginLogMapper;
import io.github.onedream921.alphavue.modules.log.mapper.SysOperLogMapper;
import io.github.onedream921.alphavue.modules.log.vo.LoginLogVo;
import io.github.onedream921.alphavue.modules.log.vo.OperationLogVo;
import io.github.onedream921.alphavue.modules.log.vo.OperationLogDetailVo;
import org.springframework.stereotype.Service;

/**
 * 日志查询服务
 */
@Service
public class LogQueryService {

    private final SysOperLogMapper operLogMapper;
    private final SysLoginLogMapper loginLogMapper;

    public LogQueryService(SysOperLogMapper operLogMapper, SysLoginLogMapper loginLogMapper) {
        this.operLogMapper = operLogMapper;
        this.loginLogMapper = loginLogMapper;
    }

    /**
     * 分页查询操作日志
     */
    public PageResponse<OperationLogVo> operations(int page, int size, OperationLogQuery query) {
        Page<SysOperLog> result = operLogMapper.selectPageByQuery(new Page<>(page, size), query);
        return new PageResponse<>(result.getRecords().stream().map(OperationLogVo::from).toList(),
                result.getTotal(), page, size);
    }

    /**
     * 分页查询登录日志
     */
    public PageResponse<LoginLogVo> logins(int page, int size) {
        Page<SysLoginLog> result = loginLogMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<SysLoginLog>().orderByDesc(SysLoginLog::getId));
        return new PageResponse<>(result.getRecords().stream().map(LoginLogVo::from).toList(),
                result.getTotal(), page, size);
    }

    /**
     * 标记失败操作日志是否已处理
     */
    public boolean updateHandlingStatus(long id, int handlingStatus, long handledBy) {
        return operLogMapper.updateHandlingStatus(id, handlingStatus, handledBy) > 0;
    }

    public OperationLogDetailVo operationDetail(long id) {
        SysOperLog log = operLogMapper.selectByIdForDetail(id);
        return log == null ? null : OperationLogDetailVo.from(log);
    }
}

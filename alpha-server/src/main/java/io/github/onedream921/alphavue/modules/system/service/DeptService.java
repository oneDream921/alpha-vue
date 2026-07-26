package io.github.onedream921.alphavue.modules.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.onedream921.alphavue.common.api.PageResponse;
import io.github.onedream921.alphavue.common.exception.BusinessException;
import io.github.onedream921.alphavue.common.exception.PublicErrorMessage;
import io.github.onedream921.alphavue.modules.system.dto.DeptRequests;
import io.github.onedream921.alphavue.modules.system.entity.SysDept;
import io.github.onedream921.alphavue.modules.system.mapper.SysDeptMapper;
import io.github.onedream921.alphavue.modules.system.vo.DeptVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 部门业务服务
 */
@Service
public class DeptService extends ServiceImpl<SysDeptMapper, SysDept> {

    /**
     * 按父级和排序号分页查询部门
     */
    public PageResponse<DeptVo> page(int pageNumber, int pageSize) {
        Page<SysDept> page = baseMapper.selectPage(new Page<>(pageNumber, pageSize),
                new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getParentId)
                        .orderByAsc(SysDept::getSortOrder).orderByAsc(SysDept::getId));
        return new PageResponse<>(page.getRecords().stream().map(DeptVo::from).toList(),
                page.getTotal(), pageNumber, pageSize);
    }

    /**
     * 查询部门详情，不存在时返回统一请求错误
     */
    public DeptVo get(long id) {
        return DeptVo.from(requireDept(id));
    }

    /**
     * 创建部门并校验父部门有效性
     */
    @Transactional
    public DeptVo create(DeptRequests.Save request) {
        validateParent(request.parentId(), null);
        SysDept dept = new SysDept();
        copy(request, dept);
        save(dept);
        return DeptVo.from(dept);
    }

    /**
     * 更新部门并阻止将自身设为父级
     */
    @Transactional
    public DeptVo update(long id, DeptRequests.Save request) {
        SysDept dept = requireDept(id);
        validateParent(request.parentId(), id);
        copy(request, dept);
        updateById(dept);
        return DeptVo.from(dept);
    }

    /**
     * 删除部门，存在子部门时拒绝删除
     */
    @Transactional
    public void delete(long id) {
        requireDept(id);
        if (baseMapper.selectCount(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id)) > 0) {
            throw invalidRequest();
        }
        removeById(id);
    }

    private void validateParent(Long parentId, Long selfId) {
        if (parentId == null || parentId == 0) {
            return;
        }
        if (parentId.equals(selfId) || getById(parentId) == null) {
            throw invalidRequest();
        }
    }

    private static void copy(DeptRequests.Save request, SysDept dept) {
        dept.setParentId(request.parentId() == null ? 0 : request.parentId());
        dept.setName(request.name());
        dept.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        dept.setStatus(request.status() == null ? 1 : request.status());
    }

    private SysDept requireDept(long id) {
        SysDept dept = getById(id);
        if (dept == null) {
            throw invalidRequest();
        }
        return dept;
    }

    private static BusinessException invalidRequest() {
        return new BusinessException(400, PublicErrorMessage.INVALID_REQUEST);
    }
}

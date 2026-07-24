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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeptService extends ServiceImpl<SysDeptMapper, SysDept> {

    public PageResponse<SysDept> page(int pageNumber, int pageSize) {
        Page<SysDept> page = baseMapper.selectPage(new Page<>(pageNumber, pageSize),
                new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getParentId)
                        .orderByAsc(SysDept::getSortOrder).orderByAsc(SysDept::getId));
        return new PageResponse<>(page.getRecords(), page.getTotal(), pageNumber, pageSize);
    }

    public SysDept get(long id) {
        return requireDept(id);
    }

    @Transactional
    public SysDept create(DeptRequests.Save request) {
        validateParent(request.parentId(), null);
        SysDept dept = new SysDept();
        copy(request, dept);
        save(dept);
        return dept;
    }

    @Transactional
    public SysDept update(long id, DeptRequests.Save request) {
        SysDept dept = requireDept(id);
        validateParent(request.parentId(), id);
        copy(request, dept);
        updateById(dept);
        return dept;
    }

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

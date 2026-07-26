package io.github.onedream921.alphavue.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.onedream921.alphavue.modules.system.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜单基础数据访问 Mapper
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {
}

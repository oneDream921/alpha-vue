package io.github.onedream921.alphavue.modules.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.onedream921.alphavue.modules.file.entity.SysFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysFileMapper extends BaseMapper<SysFile> {

    @Update("UPDATE sys_file SET deleted = 1 WHERE id = #{id} AND deleted = 0")
    int markDeletedIfActive(@Param("id") long id);

    @Select("SELECT COUNT(*) FROM sys_file WHERE id = #{id} AND deleted = 0")
    int countActiveById(@Param("id") long id);
}

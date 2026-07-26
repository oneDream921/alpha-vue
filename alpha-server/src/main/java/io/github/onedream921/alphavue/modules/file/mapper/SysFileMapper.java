package io.github.onedream921.alphavue.modules.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.onedream921.alphavue.modules.file.entity.SysFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 文件元数据数据访问 Mapper
 */
@Mapper
public interface SysFileMapper extends BaseMapper<SysFile> {

    /**
     * 在文件仍未删除时标记为已删除
     */
    int markDeletedIfActive(@Param("id") long id);

    /**
     * 统计指定文件元数据是否仍为未删除状态
     */
    int countActiveById(@Param("id") long id);
}

package io.github.onedream921.alphavue.modules.file.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.onedream921.alphavue.modules.system.entity.SystemEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 文件信息实体
 */
@Getter
@Setter
@TableName("sys_file")
public class SysFile extends SystemEntity {
    private String storageProvider;
    private String objectKey;
    private String originalName;
    private String contentType;
    private Long sizeBytes;
    private String publicUrl;
    private Long uploaderId;
}

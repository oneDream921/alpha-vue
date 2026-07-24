package io.github.onedream921.alphavue.modules.file.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.onedream921.alphavue.modules.system.entity.SystemEntity;
import lombok.Getter;
import lombok.Setter;

/** Persisted metadata for an object held by a configured storage provider. */
@Getter
@Setter
@TableName("sys_file")
public class SysFile extends SystemEntity {
    @TableField("storage_provider")
    private String storageProvider;
    @TableField("object_key")
    private String objectKey;
    @TableField("original_name")
    private String originalName;
    @TableField("content_type")
    private String contentType;
    @TableField("size_bytes")
    private Long sizeBytes;
    @TableField("public_url")
    private String publicUrl;
    @TableField("uploader_id")
    private Long uploaderId;
}

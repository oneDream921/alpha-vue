package io.github.onedream921.alphavue.modules.file.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件存储接口
 */
public interface StorageProvider {

    /**
     * 返回稳定的配置和元数据标识。
     */
    String name();

    /**
     * 保存对象内容
     */
    void store(String key, InputStream input, String contentType) throws IOException;

    /**
     * 删除对象内容
     */
    void delete(String key) throws IOException;

    /**
     * 打开对象内容用于受控下载。
     */
    InputStream open(String key) throws IOException;

    /**
     * 获取对象公开访问地址
     */
    String publicUrl(String key);
}

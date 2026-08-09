package io.github.onedream921.alphavue.modules.file.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import io.github.onedream921.alphavue.modules.system.settings.SettingGroup;
import io.github.onedream921.alphavue.modules.system.settings.service.SystemSettingService;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.InputStream;
import java.io.FilterInputStream;
import java.util.Map;

@Component
public class OssStorageProvider implements StorageProvider {
    private final SystemSettingService settings;
    public OssStorageProvider(SystemSettingService settings) { this.settings = settings; }
    public String name() { return "oss"; }
    public void store(String key, InputStream input, String contentType) throws IOException { OSS client=client(); try { client.putObject(bucket(), key, input); } catch (Exception exception) { throw new IOException("OSS upload failed", exception); } finally { client.shutdown(); } }
    public void delete(String key) throws IOException { OSS client=client(); try { client.deleteObject(bucket(), key); } catch (Exception exception) { throw new IOException("OSS delete failed", exception); } finally { client.shutdown(); } }
    public InputStream open(String key) throws IOException { OSS client=client(); try { OSSObject object = client.getObject(bucket(), key); return new FilterInputStream(object.getObjectContent()) { @Override public void close() throws IOException { try { super.close(); } finally { client.shutdown(); } } }; } catch (Exception exception) { client.shutdown(); throw new IOException("OSS read failed", exception); } }
    public String publicUrl(String key) { return baseUrl() + "/" + key; }
    private OSS client() { Map<String,Object> value = settings.runtimeValues(SettingGroup.FILE); return new OSSClientBuilder().build(required(value,"endpoint"), required(value,"accessKey"), required(value,"secretKey")); }
    private String bucket() { return required(settings.runtimeValues(SettingGroup.FILE), "bucket"); }
    private String baseUrl() { Map<String,Object> value = settings.runtimeValues(SettingGroup.FILE); String domain = value.get("accessDomain") instanceof String text && !text.isBlank() ? text : "https://" + bucket() + "." + required(value,"endpoint").replaceFirst("https?://", ""); return domain.replaceAll("/$", ""); }
    private static String required(Map<String,Object> value, String key) { Object result=value.get(key); if (!(result instanceof String text) || text.isBlank()) throw new IllegalStateException("Missing FILE setting: " + key); return text.trim(); }
}

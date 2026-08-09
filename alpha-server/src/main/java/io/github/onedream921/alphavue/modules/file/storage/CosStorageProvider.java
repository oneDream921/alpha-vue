package io.github.onedream921.alphavue.modules.file.storage;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.region.Region;
import io.github.onedream921.alphavue.modules.system.settings.SettingGroup;
import io.github.onedream921.alphavue.modules.system.settings.service.SystemSettingService;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.InputStream;
import java.io.FilterInputStream;
import java.util.Map;

@Component
public class CosStorageProvider implements StorageProvider {
    private final SystemSettingService settings;
    public CosStorageProvider(SystemSettingService settings) { this.settings = settings; }
    public String name() { return "cos"; }
    public void store(String key, InputStream input, String contentType) throws IOException { try { COSClient client=client(); client.putObject(bucket(), key, input, null); client.shutdown(); } catch(Exception exception){ throw new IOException("COS upload failed",exception); } }
    public void delete(String key) throws IOException { try { COSClient client=client(); client.deleteObject(bucket(),key); client.shutdown(); } catch(Exception exception){ throw new IOException("COS delete failed",exception); } }
    public InputStream open(String key) throws IOException { COSClient client=client(); try { COSObject object=client.getObject(bucket(),key); return new FilterInputStream(object.getObjectContent()) { @Override public void close() throws IOException { try { super.close(); } finally { client.shutdown(); } } }; } catch(Exception exception){ client.shutdown(); throw new IOException("COS read failed",exception); } }
    public String publicUrl(String key) { Map<String,Object> value=settings.runtimeValues(SettingGroup.FILE); Object domain=value.get("accessDomain"); return ((domain instanceof String text && !text.isBlank()) ? text : "https://"+bucket()+".cos."+required(value,"region")+".myqcloud.com").replaceAll("/$","")+"/"+key; }
    private COSClient client(){ Map<String,Object> value=settings.runtimeValues(SettingGroup.FILE); return new COSClient(new BasicCOSCredentials(required(value,"accessKey"),required(value,"secretKey")),new ClientConfig(new Region(required(value,"region")))); }
    private String bucket(){ return required(settings.runtimeValues(SettingGroup.FILE),"bucket"); }
    private static String required(Map<String,Object> value,String key){ Object result=value.get(key); if(!(result instanceof String text)||text.isBlank()) throw new IllegalStateException("Missing FILE setting: "+key); return text.trim(); }
}

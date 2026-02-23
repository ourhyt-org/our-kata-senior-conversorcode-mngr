package art.ourhyt.legacy2modern.conversions.domain.ports.outputs;

import java.util.Map;

public interface ObjectStorePort {
    void putJson(String key, Map<String, Object> payload);

    void putText(String key, String payload);

    byte[] getObjectBytes(String bucket, String key);
}

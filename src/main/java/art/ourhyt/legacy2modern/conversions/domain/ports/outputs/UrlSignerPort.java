package art.ourhyt.legacy2modern.conversions.domain.ports.outputs;

import java.time.Duration;

public interface UrlSignerPort {
    String presignGet(String key, Duration ttl);
}

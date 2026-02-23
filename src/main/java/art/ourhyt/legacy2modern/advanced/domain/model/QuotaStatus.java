package art.ourhyt.legacy2modern.advanced.domain.model;

public record QuotaStatus(int limit, int used, int remaining, String resetAt) {
}

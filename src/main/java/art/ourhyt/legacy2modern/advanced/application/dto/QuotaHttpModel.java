package art.ourhyt.legacy2modern.advanced.application.dto;

public record QuotaHttpModel(int limit, int used, int remaining, String resetAt) {
}

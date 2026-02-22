package art.ourhyt.legacy2modern.conversions.domain.model;

public enum JobStatus {
    PENDING,
    RUNNING,
    FINISHED,
    FAILED;

    public static JobStatus from(String value) {
        if (value == null) {
            return FAILED;
        }
        try {
            return JobStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return FAILED;
        }
    }
}

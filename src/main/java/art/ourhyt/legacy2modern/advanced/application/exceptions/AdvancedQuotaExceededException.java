package art.ourhyt.legacy2modern.advanced.application.exceptions;

import java.util.List;

public class AdvancedQuotaExceededException extends RuntimeException {
    private final String code;
    private final List<String> details;

    public AdvancedQuotaExceededException(String code, String message, List<String> details) {
        super(message);
        this.code = code;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public String code() {
        return code;
    }

    public List<String> details() {
        return details;
    }
}

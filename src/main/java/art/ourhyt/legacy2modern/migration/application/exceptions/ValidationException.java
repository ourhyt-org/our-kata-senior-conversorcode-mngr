package art.ourhyt.legacy2modern.migration.application.exceptions;

import java.util.List;

public class ValidationException extends RuntimeException {
    private final String code;
    private final List<String> details;

    public ValidationException(String code, String message, List<String> details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public String code() {
        return code;
    }

    public List<String> details() {
        return details;
    }
}

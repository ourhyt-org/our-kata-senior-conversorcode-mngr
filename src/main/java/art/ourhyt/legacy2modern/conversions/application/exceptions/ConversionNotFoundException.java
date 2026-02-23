package art.ourhyt.legacy2modern.conversions.application.exceptions;

import java.util.List;

public class ConversionNotFoundException extends RuntimeException {
    private final String code;
    private final List<String> details;

    public ConversionNotFoundException(String code, String message, List<String> details) {
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

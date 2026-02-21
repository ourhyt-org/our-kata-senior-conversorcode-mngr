package art.ourhyt.legacy2modern.migration.infrastructure.adapters.in.rest;

import java.util.List;

public record ErrorResponse(ErrorBody error) {
    public record ErrorBody(String code, String message, List<String> details) {
    }
}

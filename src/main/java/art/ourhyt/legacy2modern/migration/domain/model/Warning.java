package art.ourhyt.legacy2modern.migration.domain.model;

import java.util.List;

public record Warning(String code, String message, List<Integer> lineNumbers) {
}

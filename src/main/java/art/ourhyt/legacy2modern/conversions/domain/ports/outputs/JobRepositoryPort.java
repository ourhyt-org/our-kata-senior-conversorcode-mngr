package art.ourhyt.legacy2modern.conversions.domain.ports.outputs;

import art.ourhyt.legacy2modern.conversions.domain.model.ConversionJob;

import java.util.Optional;

public interface JobRepositoryPort {
    void save(ConversionJob job);

    Optional<ConversionJob> findByJobId(String jobId);
}

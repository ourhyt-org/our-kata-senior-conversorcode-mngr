package art.ourhyt.legacy2modern.conversions.application.services;

import art.ourhyt.legacy2modern.conversions.application.dto.GetConversionStatusResponseModel;
import art.ourhyt.legacy2modern.conversions.application.exceptions.ConversionNotFoundException;
import art.ourhyt.legacy2modern.conversions.domain.model.ConversionJob;
import art.ourhyt.legacy2modern.conversions.domain.model.JobStatus;
import art.ourhyt.legacy2modern.conversions.domain.ports.in.GetConversionStatusInputPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.ConversionConfigPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.JobRepositoryPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.outputs.UrlSignerPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.util.List;

@ApplicationScoped
public class GetConversionStatusService implements GetConversionStatusInputPort {
    private final JobRepositoryPort jobRepository;
    private final UrlSignerPort urlSigner;
    private final ConversionConfigPort config;

    @Inject
    public GetConversionStatusService(JobRepositoryPort jobRepository, UrlSignerPort urlSigner, ConversionConfigPort config) {
        this.jobRepository = jobRepository;
        this.urlSigner = urlSigner;
        this.config = config;
    }

    @Override
    public GetConversionStatusResponseModel execute(String jobId) {
        final ConversionJob job = jobRepository.findByJobId(jobId)
            .orElseThrow(() -> new ConversionNotFoundException("NOT_FOUND", "Conversion job not found", List.of("jobId=" + jobId)));

        final JobStatus status = job.status() == null ? JobStatus.FAILED : job.status();
        if (status == JobStatus.FINISHED) {
            final Duration ttl = Duration.ofMinutes(clampTtl(config.presignTtlMinutes()));
            final String downloadUrl = job.outputS3Key() == null ? null : urlSigner.presignGet(job.outputS3Key(), ttl);
            final String reportUrl = job.reportS3Key() == null ? null : urlSigner.presignGet(job.reportS3Key(), ttl);

            return new GetConversionStatusResponseModel(
                job.jobId(),
                status.name(),
                job.createdAt(),
                job.startedAt(),
                job.finishedAt(),
                job.outputS3Key(),
                job.reportS3Key(),
                downloadUrl,
                reportUrl,
                null
            );
        }

        if (status == JobStatus.FAILED) {
            return new GetConversionStatusResponseModel(
                job.jobId(),
                status.name(),
                job.createdAt(),
                job.startedAt(),
                job.finishedAt(),
                null,
                null,
                null,
                null,
                job.errorMessage() == null ? "Job failed" : job.errorMessage()
            );
        }

        return new GetConversionStatusResponseModel(
            job.jobId(),
            status.name(),
            job.createdAt(),
            job.startedAt(),
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    private int clampTtl(int ttl) {
        return Math.max(10, Math.min(20, ttl));
    }
}

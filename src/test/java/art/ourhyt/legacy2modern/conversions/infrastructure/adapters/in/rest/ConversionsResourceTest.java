package art.ourhyt.legacy2modern.conversions.infrastructure.adapters.in.rest;

import art.ourhyt.legacy2modern.conversions.application.dto.CreateConversionResponseModel;
import art.ourhyt.legacy2modern.conversions.application.dto.FileContentModel;
import art.ourhyt.legacy2modern.conversions.application.dto.GetConversionFilesResponseModel;
import art.ourhyt.legacy2modern.conversions.application.dto.ManifestEntryModel;
import art.ourhyt.legacy2modern.conversions.application.dto.SkippedFileModel;
import art.ourhyt.legacy2modern.conversions.application.dto.GetConversionStatusResponseModel;
import art.ourhyt.legacy2modern.conversions.application.exceptions.ConversionNotFoundException;
import art.ourhyt.legacy2modern.conversions.application.exceptions.ConversionNotReadyException;
import art.ourhyt.legacy2modern.conversions.domain.ports.in.CreateConversionJobInputPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.in.GetConversionFilesInputPort;
import art.ourhyt.legacy2modern.conversions.domain.ports.in.GetConversionStatusInputPort;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class ConversionsResourceTest {
    @InjectMock
    CreateConversionJobInputPort createPort;

    @InjectMock
    GetConversionStatusInputPort getPort;

    @InjectMock
    GetConversionFilesInputPort filesPort;

    @Test
    void returns401WhenApiKeyMissing() {
        final String payload = """
            {
              "languageSelected":"cobol",
              "languageTarget":"java",
              "version":"21",
              "typeArchitected":"hexagonal",
              "code":"DISPLAY 'HI'"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(payload)
            .when()
            .post("/conversions")
            .then()
            .statusCode(401)
            .body("error.code", equalTo("UNAUTHORIZED"));
    }

    @Test
    void returns202WhenRequestIsValid() {
        Mockito.when(createPort.execute(Mockito.any()))
            .thenReturn(new CreateConversionResponseModel("job-1", "PENDING", "/conversions/job-1"));

        final String payload = """
            {
              "languageSelected":"cobol",
              "languageTarget":"java",
              "version":"21",
              "typeArchitected":"hexagonal",
              "code":"DISPLAY 'HI'"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .header("X-API-KEY", "test-key")
            .body(payload)
            .when()
            .post("/conversions")
            .then()
            .statusCode(202)
            .body("jobId", equalTo("job-1"))
            .body("status", equalTo("PENDING"))
            .body("pollUrl", equalTo("/conversions/job-1"));
    }

    @Test
    void returns404WhenJobNotFound() {
        Mockito.when(getPort.execute("missing"))
            .thenThrow(new ConversionNotFoundException("NOT_FOUND", "Conversion job not found", List.of("jobId=missing")));

        given()
            .header("X-API-KEY", "test-key")
            .when()
            .get("/conversions/missing")
            .then()
            .statusCode(404)
            .body("error.code", equalTo("NOT_FOUND"));
    }

    @Test
    void returns200ForFinishedJob() {
        Mockito.when(getPort.execute("job-2"))
            .thenReturn(new GetConversionStatusResponseModel(
                "job-2",
                "FINISHED",
                "2026-02-22T10:00:00Z",
                "2026-02-22T10:01:00Z",
                "2026-02-22T10:02:00Z",
                "conversions/job-2/output.zip",
                "conversions/job-2/report.json",
                "https://signed.example.com/output",
                "https://signed.example.com/report",
                null
            ));

        given()
            .header("X-API-KEY", "test-key")
            .when()
            .get("/conversions/job-2")
            .then()
            .statusCode(200)
            .body("jobId", equalTo("job-2"))
            .body("status", equalTo("FINISHED"))
            .body("downloadUrl", equalTo("https://signed.example.com/output"));
    }

    @Test
    void filesReturns409WhenNotReady() {
        Mockito.when(filesPort.execute(Mockito.any()))
            .thenThrow(new ConversionNotReadyException("NOT_READY", "Conversion output is not ready", List.of("status=PENDING")));

        given()
            .header("X-API-KEY", "test-key")
            .when()
            .get("/conversions/job-3/files")
            .then()
            .statusCode(409)
            .body("error.code", equalTo("NOT_READY"));
    }

    @Test
    void filesReturns200ManifestOnly() {
        Mockito.when(filesPort.execute(Mockito.any()))
            .thenReturn(new GetConversionFilesResponseModel(
                "job-4",
                "FINISHED",
                "conversions/job-4/output.zip",
                2048,
                "cmd/app/main.go",
                List.of("cmd/app/main.go", "README.md", "src/main.ts"),
                List.of(
                    new ManifestEntryModel("cmd/app/main.go", 1200, true),
                    new ManifestEntryModel("README.md", 200, true)
                ),
                List.of(),
                List.of(new SkippedFileModel("assets/logo.png", "unsupported_extension"))
            ));

        given()
            .header("X-API-KEY", "test-key")
            .when()
            .get("/conversions/job-4/files")
            .then()
            .statusCode(200)
            .body("jobId", equalTo("job-4"))
            .body("zipSizeBytes", equalTo(2048))
            .body("defaultFile", equalTo("cmd/app/main.go"))
            .body("recommendedFiles[0]", equalTo("cmd/app/main.go"));
    }

    @Test
    void filesReturns200WithRequestedContent() {
        Mockito.when(filesPort.execute(Mockito.any()))
            .thenReturn(new GetConversionFilesResponseModel(
                "job-5",
                "FINISHED",
                "conversions/job-5/output.zip",
                2048,
                "cmd/app/main.go",
                List.of("cmd/app/main.go"),
                List.of(new ManifestEntryModel("cmd/app/main.go", 1200, true)),
                List.of(new FileContentModel("cmd/app/main.go", "package main")),
                List.of()
            ));

        given()
            .header("X-API-KEY", "test-key")
            .when()
            .get("/conversions/job-5/files?includeContent=true&paths=cmd/app/main.go")
            .then()
            .statusCode(200)
            .body("files[0].path", equalTo("cmd/app/main.go"))
            .body("files[0].content", equalTo("package main"));
    }
}

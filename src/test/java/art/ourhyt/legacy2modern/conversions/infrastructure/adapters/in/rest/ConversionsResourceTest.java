package art.ourhyt.legacy2modern.conversions.infrastructure.adapters.in.rest;

import art.ourhyt.legacy2modern.conversions.application.dto.CreateConversionResponseModel;
import art.ourhyt.legacy2modern.conversions.application.dto.GetConversionStatusResponseModel;
import art.ourhyt.legacy2modern.conversions.application.exceptions.ConversionNotFoundException;
import art.ourhyt.legacy2modern.conversions.domain.ports.in.CreateConversionJobInputPort;
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
}

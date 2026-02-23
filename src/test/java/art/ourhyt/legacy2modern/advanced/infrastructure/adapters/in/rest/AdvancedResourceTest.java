package art.ourhyt.legacy2modern.advanced.infrastructure.adapters.in.rest;

import art.ourhyt.legacy2modern.advanced.application.dto.AdvancedCreateConversionResponseModel;
import art.ourhyt.legacy2modern.advanced.application.dto.AdvancedQuotaResponseModel;
import art.ourhyt.legacy2modern.advanced.application.dto.QuotaHttpModel;
import art.ourhyt.legacy2modern.advanced.application.exceptions.AdvancedQuotaExceededException;
import art.ourhyt.legacy2modern.advanced.application.exceptions.AdvancedUnauthorizedException;
import art.ourhyt.legacy2modern.advanced.domain.ports.in.CreateAdvancedConversionInputPort;
import art.ourhyt.legacy2modern.advanced.domain.ports.in.GetAdvancedQuotaInputPort;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
class AdvancedResourceTest {
    @InjectMock
    CreateAdvancedConversionInputPort createPort;

    @InjectMock
    GetAdvancedQuotaInputPort quotaPort;

    @Test
    void returns401WhenMissingAuthorization() {
        Mockito.when(createPort.execute(ArgumentMatchers.isNull(), ArgumentMatchers.any()))
            .thenThrow(new AdvancedUnauthorizedException("UNAUTHORIZED", "Invalid or missing bearer token", List.of("Authorization header is required")));

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
            .post("/advanced/conversions")
            .then()
            .statusCode(401)
            .body("error.code", equalTo("UNAUTHORIZED"));
    }

    @Test
    void returns429WhenQuotaExceeded() {
        Mockito.when(createPort.execute(ArgumentMatchers.anyString(), ArgumentMatchers.any()))
            .thenThrow(new AdvancedQuotaExceededException("QUOTA_EXCEEDED", "Daily limit reached", List.of("limit=10", "used=10")));

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
            .header("Authorization", "Bearer token")
            .body(payload)
            .when()
            .post("/advanced/conversions")
            .then()
            .statusCode(429)
            .body("error.code", equalTo("QUOTA_EXCEEDED"));
    }

    @Test
    void returns202WhenAllowed() {
        Mockito.when(createPort.execute(ArgumentMatchers.anyString(), ArgumentMatchers.any()))
            .thenReturn(new AdvancedCreateConversionResponseModel(
                "job-adv-1",
                "PENDING",
                "/conversions/job-adv-1",
                new QuotaHttpModel(10, 1, 9, "2026-02-23T00:00:00Z")
            ));

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
            .header("Authorization", "Bearer token")
            .body(payload)
            .when()
            .post("/advanced/conversions")
            .then()
            .statusCode(202)
            .body("jobId", equalTo("job-adv-1"))
            .body("quota.remaining", equalTo(9));
    }

    @Test
    void returns200ForQuotaEndpoint() {
        Mockito.when(quotaPort.execute(ArgumentMatchers.anyString()))
            .thenReturn(new AdvancedQuotaResponseModel(new QuotaHttpModel(10, 2, 8, "2026-02-23T00:00:00Z")));

        given()
            .header("Authorization", "Bearer token")
            .when()
            .get("/advanced/quota")
            .then()
            .statusCode(200)
            .body("quota.limit", equalTo(10))
            .body("quota.used", equalTo(2))
            .body("quota.remaining", equalTo(8));
    }
}

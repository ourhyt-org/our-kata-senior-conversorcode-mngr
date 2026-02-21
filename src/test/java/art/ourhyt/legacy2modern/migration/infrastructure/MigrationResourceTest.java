package art.ourhyt.legacy2modern.migration.infrastructure;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;

@QuarkusTest
class MigrationResourceTest {
    @Test
    void returns401WhenApiKeyMissing() {
        final String payload = """
            {
              "sourceLanguage":"COBOL",
              "targetLanguage":"JAVA",
              "code":"DISPLAY 'HI'"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .body(payload)
            .when()
            .post("/migrate")
            .then()
            .statusCode(401)
            .body("error.code", equalTo("UNAUTHORIZED"));
    }

    @Test
    void returns200WhenApiKeyValid() {
        final String payload = """
            {
              "sourceLanguage":"COBOL",
              "targetLanguage":"JAVA",
              "code":"IF A = B THEN\\nDISPLAY 'HI'\\nEND-IF"
            }
            """;

        given()
            .contentType(ContentType.JSON)
            .header("X-API-KEY", "test-key")
            .body(payload)
            .when()
            .post("/migrate")
            .then()
            .statusCode(200)
            .body("outputCode", equalTo("if (A = B) {\nSystem.out.println(\"HI\");\n}"))
            .body("report", hasKey("appliedRules"))
            .body("report", hasKey("warnings"))
            .body("report.appliedRules.size()", greaterThan(0));
    }
}

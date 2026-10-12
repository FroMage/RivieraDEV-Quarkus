package rest;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import testdata.Login;
import testdata.TestData;

/**
 * The admin and backoffice pages must require a login, and work once logged in.
 */
@QuarkusTest
public class AccessControlTest {

    static final List<String> ADMIN_PAGES = List.of(
            "/Admin/index", "/Admin/uploadProgramForm", "/Admin/speakerEmails", "/Admin/speakerEmailCompany",
            "/Admin/speakerBadges", "/Admin/organiserBadges", "/Admin/sponsorBadges", "/Admin/badgesForm",
            "/Admin/bufferScheduler", "/Admin/sponsorScheduler", "/Admin/sponsorTwitter", "/Admin/slidesSecrets",
            "/Admin/speakerPhotosZip",
            // the schedule of the room duty, linked from the admin index
            "/excel");

    static final List<String> BACKOFFICE_PAGES = List.of(
            "/_renarde/backoffice/index", "/_renarde/backoffice/Talk/index", "/_renarde/backoffice/Speaker/index",
            "/_renarde/backoffice/Sponsor/index", "/_renarde/backoffice/User/index",
            "/_renarde/backoffice/Configuration/index");

    private static Map<String, String> admin;

    // not in a @BeforeAll, as the HTTP port of the test is not configured yet there
    private static synchronized Map<String, String> admin() {
        if (admin == null) {
            admin = Login.asAdmin();
        }
        return admin;
    }

    @Test
    public void adminPagesRequireLogin() {
        List<String> open = new ArrayList<>();
        for (String page : ADMIN_PAGES) {
            int status = given().redirects().follow(false).get(page).statusCode();
            // redirected to the login page, or refused
            if (status < 300 || status == 404 || status >= 500) {
                open.add(page + " -> " + status);
            }
        }
        assertTrue(open.isEmpty(), "Admin pages that do not require a login: " + open);
    }

    @Test
    public void backofficeRequiresLogin() {
        List<String> open = new ArrayList<>();
        for (String page : BACKOFFICE_PAGES) {
            int status = given().redirects().follow(false).get(page).statusCode();
            if (status < 300 || status == 404 || status >= 500) {
                open.add(page + " -> " + status);
            }
        }
        assertTrue(open.isEmpty(), "Backoffice pages that do not require a login: " + open);
    }

    @Test
    public void anonymousRedirectIsToTheLoginPage() {
        Response response = given().redirects().follow(false).get("/Admin/index");
        assertTrue(response.statusCode() == 302 || response.statusCode() == 303, "status " + response.statusCode());
        assertTrue(response.header("Location").contains("login"), response.header("Location"));
    }

    @Test
    public void adminPagesWorkWhenLoggedIn() {
        List<String> failures = new ArrayList<>();
        for (String page : ADMIN_PAGES) {
            int status = given().redirects().follow(false).cookies(admin()).get(page).statusCode();
            if (status != 200) {
                failures.add(page + " -> " + status);
            }
        }
        assertTrue(failures.isEmpty(), "Admin pages that fail when logged in: " + failures);
    }

    @Test
    public void backofficeWorksWhenLoggedIn() {
        List<String> failures = new ArrayList<>();
        for (String page : BACKOFFICE_PAGES) {
            int status = given().redirects().follow(false).cookies(admin()).get(page).statusCode();
            if (status != 200) {
                failures.add(page + " -> " + status);
            }
        }
        assertTrue(failures.isEmpty(), "Backoffice pages that fail when logged in: " + failures);
    }

    @Test
    public void anonymousCannotPost() {
        // with the CSRF token so that we test authentication, and not the CSRF protection
        String csrf = given().get("/_renarde/security/login").then().extract().cookie("csrf-token");
        for (String action : List.of("/Admin/startBufferScheduler", "/Admin/stopBufferScheduler",
                "/Admin/startSponsorScheduler", "/Admin/sendSlidesEmails", "/Admin/uploadProgram")) {
            int status = given().redirects().follow(false).cookie("csrf-token", csrf).formParam("csrf-token", csrf)
                    .post(action).statusCode();
            assertTrue(status >= 300 && status != 404 && status < 500 || status == 401 || status == 403,
                    action + " -> " + status);
        }
    }

    @Test
    public void wrongPasswordDoesNotLogIn() {
        String csrf = given().get("/_renarde/security/login").then().extract().cookie("csrf-token");
        Response response = given().redirects().follow(false).cookie("csrf-token", csrf).formParam("csrf-token", csrf)
                .formParam("username", TestData.adminUser()).formParam("password", "wrong")
                .post("/_renarde/security/login");
        // sent back to the login form...
        assertTrue(response.header("Location") != null && response.header("Location").contains("login"),
                "status " + response.statusCode() + " location " + response.header("Location"));
        // ...and whatever cookies we got do not give access
        Response admin = given().redirects().follow(false).cookies(response.cookies()).get("/Admin/index");
        assertTrue(admin.statusCode() == 302 || admin.statusCode() == 303, "status " + admin.statusCode());
        assertTrue(admin.header("Location").contains("login"), admin.header("Location"));
    }
}

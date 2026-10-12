package testdata;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import io.restassured.response.Response;

/**
 * Logs in through the real login form, so that we test authentication the way the browser does it.
 */
public class Login {

    /** Cookies of an admin user, to be sent with requests. */
    public static Map<String, String> asAdmin() {
        TestData.adminUser();
        // renarde protects forms with a CSRF token, as a cookie and as a form field
        String csrf = given().get("/_renarde/security/login").then().statusCode(200).extract().cookie("csrf-token");
        Response login = given().redirects().follow(false)
                .cookie("csrf-token", csrf)
                .formParam("csrf-token", csrf)
                .formParam("username", TestData.ADMIN_USER)
                .formParam("password", TestData.ADMIN_PASSWORD)
                .post("/_renarde/security/login");
        assertEquals(303, login.statusCode(), "Login should redirect, not show the form again: " + login.asString());
        // keep the CSRF token as well, as an admin who posts forms needs it
        Map<String, String> cookies = new HashMap<>(login.cookies());
        cookies.put("csrf-token", csrf);
        return cookies;
    }

    /** The CSRF token to send, as a cookie and as a form field, with a POST to a public page */
    public static String csrfToken() {
        return given().get("/_renarde/security/login").then().statusCode(200).extract().cookie("csrf-token");
    }
}

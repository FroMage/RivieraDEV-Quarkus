package rest;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import model.Talk;
import testdata.Login;
import testdata.TestData;

/**
 * The like button of the talks: anyone can like, but only the admin gets to see the count.
 */
@QuarkusTest
public class LikesTest {

    private static Map<String, String> admin;

    private static synchronized Map<String, String> admin() {
        if (admin == null) {
            admin = Login.asAdmin();
        }
        return admin;
    }

    private static Response anonymous(String action, Long id) {
        String csrf = Login.csrfToken();
        return given().cookie("csrf-token", csrf).formParam("csrf-token", csrf).post("/" + action + "/" + id);
    }

    private static Response asAdmin(String action, Long id) {
        String csrf = admin().get("csrf-token");
        return given().cookies(admin()).cookie("csrf-token", csrf).formParam("csrf-token", csrf)
                .post("/" + action + "/" + id);
    }

    private static int likes(Talk talk) {
        return QuarkusTransaction.requiringNew().call(() -> ((Talk) Talk.findById(talk.id)).nbLikes);
    }

    @Test
    public void likesAreCountedButNotShownToVisitors() {
        Talk talk = TestData.talk();
        assertEquals(0, likes(talk));

        anonymous("like-talk", talk.id).then().statusCode(204);
        anonymous("like-talk", talk.id).then().statusCode(204);
        assertEquals(2, likes(talk));

        anonymous("unlike-talk", talk.id).then().statusCode(204);
        assertEquals(1, likes(talk));
    }

    @Test
    public void adminSeesTheCount() {
        Talk talk = TestData.talk();
        assertEquals("1", asAdmin("like-talk", talk.id).then().statusCode(200).extract().asString());
        assertEquals("2", asAdmin("like-talk", talk.id).then().statusCode(200).extract().asString());
        assertEquals("1", asAdmin("unlike-talk", talk.id).then().statusCode(200).extract().asString());
    }

    @Test
    public void likesCannotGoBelowZero() {
        Talk talk = TestData.talk();
        anonymous("unlike-talk", talk.id);
        anonymous("unlike-talk", talk.id);
        assertEquals(0, likes(talk));
    }

    @Test
    public void likingAnUnknownTalkIsIgnored() {
        anonymous("like-talk", 0L).then().statusCode(204);
        anonymous("unlike-talk", 0L).then().statusCode(204);
    }

    @Test
    public void talkWithoutACounterStartsAtZero() {
        // talks imported before the counter existed
        Talk talk = TestData.talk(t -> t.nbLikes = null);
        anonymous("like-talk", talk.id);
        assertEquals(1, likes(talk));
    }
}

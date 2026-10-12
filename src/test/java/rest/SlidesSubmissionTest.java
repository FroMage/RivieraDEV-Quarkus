package rest;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import model.Speaker;
import model.Talk;
import testdata.Login;
import testdata.TestData;

/**
 * Speakers give us the link to their slides with the secret URL of their talk, which we email them.
 */
@QuarkusTest
public class SlidesSubmissionTest {

    @Inject
    MockMailbox mailbox;

    private static Map<String, String> admin;

    private static synchronized Map<String, String> admin() {
        if (admin == null) {
            admin = Login.asAdmin();
        }
        return admin;
    }

    @BeforeEach
    void clearMailbox() {
        mailbox.clear();
    }

    private static String slidesUrl(Talk talk) {
        return QuarkusTransaction.requiringNew().call(() -> ((Talk) Talk.findById(talk.id)).slidesUrl);
    }

    private static Response submit(String secret, String url) {
        String csrf = Login.csrfToken();
        return given().redirects().follow(false).cookie("csrf-token", csrf).formParam("csrf-token", csrf)
                .formParam("slidesUrl", url).post("/talk/" + secret + "/submit-slides");
    }

    private static Response adminPost(String path) {
        String csrf = admin().get("csrf-token");
        return given().redirects().follow(false).cookies(admin()).cookie("csrf-token", csrf)
                .formParam("csrf-token", csrf).post(path);
    }

    private static Talk talkWithSpeakerEmail(String email) {
        Talk talk = TestData.talk();
        TestData.addSpeaker(talk, TestData.speaker(s -> s.email = email));
        return talk;
    }

    @Test
    public void formShowsTheTalk() {
        Talk talk = TestData.talk();
        String html = given().get("/talk/" + talk.slidesSecret + "/submit-slides").then().statusCode(200)
                .extract().asString();
        assertTrue(html.contains(talk.titleEN), html);
    }

    @Test
    public void speakerCanSubmitTheSlidesAndTheyShowOnTheTalkPage() {
        Talk talk = TestData.talk();
        assertEquals(303, submit(talk.slidesSecret, "https://speakerdeck.com/me/my-talk").statusCode());

        assertEquals("https://speakerdeck.com/me/my-talk", slidesUrl(talk));
        String page = given().get("/session/" + talk.id).then().statusCode(200).extract().asString();
        assertTrue(page.contains("href=\"https://speakerdeck.com/me/my-talk\""), "Slides link missing from the talk page");
        // and it can be replaced
        assertEquals(303, submit(talk.slidesSecret, "https://example.com/newer").statusCode());
        assertEquals("https://example.com/newer", slidesUrl(talk));
    }

    @Test
    public void wrongSecretCannotSubmit() {
        Talk talk = TestData.talk();
        assertEquals(404, submit("not-the-secret", "https://example.com/slides").statusCode());
        assertNull(slidesUrl(talk));
        given().get("/talk/not-the-secret/submit-slides").then().statusCode(404);
    }

    @Test
    public void onlyWebLinksAreAccepted() {
        Talk talk = TestData.talk();
        // the talk page links to what we store: it must not be a script or something that is not a link
        for (String invalid : List.of("javascript:alert(document.cookie)", "data:text/html,<script>alert(1)</script>",
                "ftp://example.com/slides", "not a url", "", "   ", "https://")) {
            assertEquals(303, submit(talk.slidesSecret, invalid).statusCode(), invalid);
            assertNull(slidesUrl(talk), "Accepted '" + invalid + "' as slides link");
        }
        String page = given().get("/session/" + talk.id).then().statusCode(200).extract().asString();
        assertFalse(page.contains("javascript:"), page);
    }

    @Test
    public void slidesLinkIsTrimmed() {
        Talk talk = TestData.talk();
        submit(talk.slidesSecret, "  https://example.com/slides \n");
        assertEquals("https://example.com/slides", slidesUrl(talk));
    }

    @Test
    public void emailToASpeakerHasTheLinkToSubmitTheSlides() {
        Talk talk = talkWithSpeakerEmail("speaker-one@example.com");

        assertEquals(303, adminPost("/Admin/sendSlidesEmailForTalk/" + talk.id).statusCode());

        List<Mail> mails = mailbox.getMailsSentTo("speaker-one@example.com");
        assertEquals(1, mails.size());
        Mail mail = mails.get(0);
        assertEquals("[Riviera DEV] Please share your slides", mail.getSubject());
        assertTrue(mail.getFrom().contains("info@rivieradev.fr"), mail.getFrom());
        assertTrue(mail.getText().contains(talk.titleEN), mail.getText());
        // an absolute URL, as it is read outside of the site
        String link = "/talk/" + talk.slidesSecret + "/submit-slides";
        assertTrue(mail.getText().contains("http") && mail.getText().contains(link), mail.getText());
        assertTrue(mail.getHtml().contains(link), mail.getHtml());
    }

    @Test
    public void emailIsSentToEverySpeakerOfTheTalk() {
        Talk talk = talkWithSpeakerEmail("first-speaker@example.com");
        TestData.addSpeaker(talk, TestData.speaker(s -> s.email = "second-speaker@example.com"));

        adminPost("/Admin/sendSlidesEmailForTalk/" + talk.id);

        assertEquals(1, mailbox.getMailsSentTo("first-speaker@example.com").size());
        assertEquals(1, mailbox.getMailsSentTo("second-speaker@example.com").size());
    }

    @Test
    public void noEmailWhenTheSpeakersHaveNoAddress() {
        Talk talk = TestData.talk();
        TestData.addSpeaker(talk, TestData.speaker(s -> s.email = " "));

        assertEquals(303, adminPost("/Admin/sendSlidesEmailForTalk/" + talk.id).statusCode());

        assertEquals(0, mailbox.getTotalMessagesSent());
    }

    @Test
    public void testEmailGoesToTheOrganisersNotTheSpeaker() {
        Talk talk = talkWithSpeakerEmail("not-this-one@example.com");

        adminPost("/Admin/sendTestSlidesEmail/" + talk.id);

        assertEquals(0, mailbox.getMailsSentTo("not-this-one@example.com").size());
        assertEquals(1, mailbox.getMailsSentTo("info@rivieradev.fr").size());
    }

    @Test
    public void bulkEmailSkipsTalksThatAlreadyHaveTheirSlides() {
        Talk waiting = talkWithSpeakerEmail("still-waiting@example.com");
        Talk done = talkWithSpeakerEmail("already-done@example.com");
        TestData.update(done, t -> t.slidesUrl = "https://example.com/done");

        assertEquals(303, adminPost("/Admin/sendSlidesEmails").statusCode());

        assertEquals(1, mailbox.getMailsSentTo("still-waiting@example.com").size());
        assertEquals(0, mailbox.getMailsSentTo("already-done@example.com").size());
    }

    @Test
    public void emailsRequireALogin() {
        Talk talk = talkWithSpeakerEmail("anonymous-test@example.com");
        String csrf = Login.csrfToken();
        given().redirects().follow(false).cookie("csrf-token", csrf).formParam("csrf-token", csrf)
                .post("/Admin/sendSlidesEmailForTalk/" + talk.id);
        assertEquals(0, mailbox.getTotalMessagesSent());
    }

    @Test
    public void adminOverviewListsTheSecrets() {
        Talk talk = TestData.talk();
        String html = given().cookies(admin()).get("/Admin/slidesSecrets").then().statusCode(200).extract().asString();
        assertTrue(html.contains(talk.slidesSecret), "The admin needs the secret URLs");
    }
}

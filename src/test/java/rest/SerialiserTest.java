package rest;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import model.Speaker;
import model.Talk;
import testdata.TestData;

/**
 * /Serialiser/json is public: it is what the development setup imports the program from.
 */
@QuarkusTest
public class SerialiserTest {

    @Test
    public void exportContainsThePublicData() {
        Speaker speaker = TestData.speaker();
        Talk talk = TestData.talk();
        TestData.addSpeaker(talk, speaker);

        String json = given().get("/Serialiser/json").then().statusCode(200).extract().asString();

        assertTrue(json.contains(speaker.lastName), "Speaker missing from the export");
        assertTrue(json.contains(talk.titleEN), "Talk missing from the export");
    }

    @Test
    public void exportDoesNotLeakSpeakerContactDetails() {
        Speaker speaker = TestData.speaker(s -> {
            s.email = "private-email-of-speaker@example.com";
            s.phone = "+33 6 12 34 56 78";
        });

        String json = given().get("/Serialiser/json").then().statusCode(200).extract().asString();

        // the speaker is in there, so that this does not pass just because it is missing
        assertTrue(json.contains(speaker.lastName), "Speaker missing from the export");
        assertFalse(json.contains(speaker.email), "Speaker email leaked in the public export");
        assertFalse(json.contains(speaker.phone), "Speaker phone leaked in the public export");
    }

    @Test
    public void exportDoesNotLeakSlidesSecrets() {
        // anyone who knows the secret can submit the slides of a talk
        Talk talk = TestData.talk();

        String json = given().get("/Serialiser/json").then().statusCode(200).extract().asString();

        assertTrue(json.contains(talk.titleEN), "Talk missing from the export");
        assertFalse(json.contains(talk.slidesSecret), "Slides secret leaked in the public export");
    }

    @Test
    public void exportDoesNotContainUsers() {
        String admin = TestData.adminUser();
        String json = given().get("/Serialiser/json").then().statusCode(200).extract().asString();
        assertFalse(json.contains(admin), "Users must not be exported");
        assertFalse(json.contains("$2a$"), "Password hashes must not be exported");
    }
}

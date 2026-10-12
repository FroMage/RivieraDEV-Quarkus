package rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import model.Organiser;
import model.PreviousSpeaker;
import model.Speaker;
import model.Sponsor;
import model.SponsorShip;
import testdata.TestData;

/**
 * Photos and logos are served from the database, with Last-Modified so that browsers do not download them again.
 */
@QuarkusTest
public class MediaEndpointsTest {

    private static final byte[] IMAGE = TestData.png(60, 40);

    /** Checks that an image is served, that it is not sent again to a client that has it, and is when it changes */
    private static <T extends PanacheEntityBase> void checkCaching(String url, Class<T> type, Object id,
            Consumer<T> change) throws InterruptedException {
        Response first = given().get(url);
        first.then().statusCode(200).header("Content-Type", containsString("image/png"));
        assertArrayEquals(IMAGE, first.asByteArray());
        String lastModified = first.header("Last-Modified");
        assertNotNull(lastModified, "Last-Modified missing");

        // the client has it
        given().header("If-Modified-Since", lastModified).get(url).then().statusCode(304);

        // it changes, and is sent again to the client that has the old one
        Thread.sleep(1100);
        QuarkusTransaction.requiringNew().run(() -> change.accept(Panache.getEntityManager().find(type, id)));
        Response changed = given().header("If-Modified-Since", lastModified).get(url);
        changed.then().statusCode(200);
        assertNotEquals(lastModified, changed.header("Last-Modified"));
        // and the new one is cached too
        given().header("If-Modified-Since", changed.header("Last-Modified")).get(url).then().statusCode(304);
    }

    @Test
    public void speakerPhoto() throws Exception {
        Speaker speaker = TestData.speaker(s -> s.photo = TestData.blob(IMAGE));
        checkCaching("/Application/speakerPhoto/" + speaker.id, Speaker.class, speaker.id, s -> s.company = "Changed");
    }

    @Test
    public void sponsorLogo() throws Exception {
        Sponsor sponsor = TestData.sponsor(SponsorShip.Gold, s -> s.logo = TestData.blob(IMAGE));
        checkCaching("/Application/sponsorLogo/" + sponsor.id, Sponsor.class, sponsor.id, s -> s.company = "Changed");
    }

    @Test
    public void previousSpeakerPhoto() throws Exception {
        PreviousSpeaker speaker = TestData.previousSpeaker(s -> s.photo = TestData.blob(IMAGE));
        checkCaching("/Application/previousSpeakerPhoto/" + speaker.id, PreviousSpeaker.class, speaker.id,
                s -> s.company = "Changed");
    }

    @Test
    public void organiserPhoto() throws Exception {
        Organiser organiser = TestData.organiser(o -> o.photo = TestData.blob(IMAGE));
        checkCaching("/Application/orgaPhoto/" + organiser.id, Organiser.class, organiser.id,
                o -> o.company = "Changed");
    }

    @Test
    public void missingPhotosUseTheDefaultOne() {
        Speaker speaker = TestData.speaker();
        PreviousSpeaker previous = TestData.previousSpeaker(p -> {
        });
        Organiser organiser = TestData.organiser(o -> {
        });
        String speakerRedirect = given().redirects().follow(false).get("/Application/speakerPhoto/" + speaker.id)
                .then().statusCode(303).extract().header("Location");
        String previousRedirect = given().redirects().follow(false)
                .get("/Application/previousSpeakerPhoto/" + previous.id).then().statusCode(303).extract().header("Location");
        String organiserRedirect = given().redirects().follow(false).get("/Application/orgaPhoto/" + organiser.id)
                .then().statusCode(303).extract().header("Location");
        for (String location : new String[] { speakerRedirect, previousRedirect, organiserRedirect }) {
            assertTrue(location.contains("/static/images/mascotte/"), location);
            // and that default photo does exist
            given().get(location).then().statusCode(200);
        }
    }

    @Test
    public void unknownEntitiesAreNotFound() {
        for (String endpoint : new String[] { "speakerPhoto", "sponsorLogo", "previousSpeakerPhoto", "orgaPhoto" }) {
            given().get("/Application/" + endpoint + "/0").then().statusCode(404);
        }
    }
}

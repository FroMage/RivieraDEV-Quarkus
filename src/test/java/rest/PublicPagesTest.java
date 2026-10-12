package rest;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import model.Organiser;
import model.PreviousSpeaker;
import model.Speaker;
import model.Sponsor;
import model.SponsorShip;
import model.Talk;
import model.TalkTheme;
import model.TalkType;
import model.Track;
import testdata.TestData;

/**
 * Renders every public page, in both languages, with some data: this catches template errors, which is
 * what is most likely to break when an entity changes.
 */
@QuarkusTest
public class PublicPagesTest {

    static Talk talk;
    static Speaker speaker;
    static Sponsor sponsor;
    static Track track;

    @BeforeAll
    static void createData() {
        track = TestData.track();
        TalkTheme theme = TestData.theme();
        TalkType type = TestData.type();
        speaker = TestData.speaker();
        long hour = 60 * 60 * 1000;
        long start = Date.valueOf("2026-07-06").getTime() + 9 * hour;
        var slot = TestData.slot(new java.util.Date(start), new java.util.Date(start + hour));
        talk = TestData.talk(t -> {
            t.track = track;
            t.theme = theme;
            t.type = type;
            t.slot = slot;
        });
        TestData.addSpeaker(talk, speaker);
        // a sponsor of every level, as the home and sponsors pages group them
        for (SponsorShip level : SponsorShip.values()) {
            Sponsor created = TestData.sponsor(level);
            if (level == SponsorShip.Gold) {
                sponsor = created;
            }
        }
        QuarkusTransaction.requiringNew().run(() -> {
            PreviousSpeaker previous = new PreviousSpeaker();
            previous.firstName = "Previous";
            previous.lastName = "Speaker";
            previous.company = "Old company";
            previous.year = 2025;
            previous.persist();
            Organiser organiser = new Organiser();
            organiser.firstName = "Orga";
            organiser.lastName = "Niser";
            organiser.biography = "Organises things";
            organiser.orga = true;
            organiser.persist();
        });
    }

    private static List<String> publicPages() {
        List<String> pages = new ArrayList<>(List.of(
                "/", "/photos", "/access", "/coc", "/subscribe", "/schedule", "/schedule-supersecret",
                "/talks", "/speakers", "/team", "/schools", "/live", "/live/" + track.title, "/live-twitter/" + track.title,
                "/fishmarket/2026-07-06",
                "/speaker/" + speaker.id, "/sponsor/" + sponsor.id, "/session/" + talk.id,
                "/talk/" + talk.slidesSecret + "/submit-slides",
                // the HTML the banners are rendered from
                "/talk-banner/" + talk.id + "/en", "/speaker-banner/" + speaker.id + "/en",
                "/sponsor-banner/" + sponsor.id + "/en"));
        return pages;
    }

    @Test
    public void allPublicPagesRender() {
        List<String> failures = new ArrayList<>();
        for (String language : List.of("en", "fr")) {
            for (String page : publicPages()) {
                Response response = given().redirects().follow(false).header("Accept-Language", language).get(page);
                int status = response.statusCode();
                // some pages redirect (to a configured url for instance), which is fine
                if (status >= 400) {
                    failures.add(language + " " + page + " -> " + status);
                }
            }
        }
        assertTrue(failures.isEmpty(), "Pages that fail: " + failures);
    }

    @Test
    public void talkWithoutALevelRenders() {
        // the level is optional in the backoffice, and the schedule pages must cope with it
        Talk withoutLevel = TestData.talk(t -> {
            t.level = null;
            t.track = track;
            t.slot = talk.slot;
        });
        for (String page : List.of("/schedule", "/schedule-supersecret", "/session/" + withoutLevel.id, "/talks")) {
            given().get(page).then().statusCode(200);
        }
        String schedule = given().get("/schedule-supersecret").then().statusCode(200).extract().asString();
        assertTrue(schedule.contains(withoutLevel.titleEN), "Talk without a level missing from the schedule");
    }

    @Test
    public void unknownEntitiesAreNotFound() {
        for (String page : List.of("/speaker/0", "/sponsor/0", "/session/0", "/talk/no-such-secret/submit-slides")) {
            given().get(page).then().statusCode(404);
        }
    }

    @Test
    public void talkPageShowsTheTalkAndItsSpeaker() {
        String html = given().get("/session/" + talk.id).then().statusCode(200).extract().asString();
        assertTrue(html.contains(talk.titleEN), html);
        assertTrue(html.contains(speaker.lastName), html);
    }

    @Test
    public void speakerPhotoRedirectsWhenThereIsNone() {
        // the default photo is used for speakers without one
        given().redirects().follow(false).get("/Application/speakerPhoto/" + speaker.id).then().statusCode(303);
    }
}

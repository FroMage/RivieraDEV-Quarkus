package rest;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import model.Language;
import model.Level;
import model.Speaker;
import model.Talk;
import model.TalkTheme;
import model.TalkType;
import testdata.Login;
import testdata.TestData;

/**
 * Importing the program exported by the CFP app.
 */
@QuarkusTest
public class ProgramImportTest {

    @TempDir
    static Path temp;

    private static Map<String, String> admin;

    private static synchronized Map<String, String> admin() {
        if (admin == null) {
            admin = Login.asAdmin();
        }
        return admin;
    }

    /** What the CFP app exports, with names that are unique to this test so that they do not clash with other data */
    static class Program {
        final String uid = UUID.randomUUID().toString().substring(0, 8);
        final String janeEmail = "jane-" + uid + "@example.com";
        final String cherEmail = "cher-" + uid + "@example.com";
        final String themeA = "Cloud " + uid;
        final String themeB = "Security " + uid;
        final String conference = "Conference " + uid;
        final String workshop = "Workshop " + uid;
        final String titleA = "Talk A " + uid;
        final String titleB = "Talk B " + uid;

        String json(String pictureUrl) throws JsonProcessingException {
            Map<String, Object> jane = Map.of("id", "s1", "name", "Jane van Doe", "bio", "Jane's bio",
                    "company", "Acme " + uid, "picture", pictureUrl, "email", janeEmail,
                    "socialLinks", List.of("https://twitter.com/jane_tweets", "https://www.linkedin.com/in/jane-li",
                            "https://github.com/jane-gh"));
            // only a first name, no bio and no picture
            Map<String, Object> cher = Map.of("id", "s2", "name", "Cher", "bio", " ", "email", cherEmail,
                    "socialLinks", List.of());
            Map<String, Object> a = Map.of("id", "p1-" + uid, "title", titleA, "level", "advanced",
                    "abstract", "Abstract of A", "categories", List.of(themeA, themeB), "formats", List.of(conference),
                    "languages", List.of("fr"), "speakers", List.of(jane, cher));
            // the same speaker again, and no level
            java.util.HashMap<String, Object> b = new java.util.HashMap<>(Map.of("id", "p2-" + uid, "title", titleB,
                    "abstract", "Abstract of B", "categories", List.of(themeA), "formats", List.of(workshop),
                    "languages", List.of("en"), "speakers", List.of(jane)));
            b.put("level", null);
            return new ObjectMapper().writeValueAsString(Map.of("name", "Program", "proposals", List.of(a, b)));
        }
    }

    private static Path picture() throws IOException {
        Path picture = temp.resolve("jane.png");
        // larger than what we keep
        Files.write(picture, TestData.png(800, 600));
        return picture;
    }

    private static Response upload(String json) {
        String csrf = admin().get("csrf-token");
        return given().redirects().follow(false).cookies(admin()).cookie("csrf-token", csrf)
                .multiPart("csrf-token", csrf)
                .multiPart("program", "program.json", json.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        "application/octet-stream")
                .post("/Admin/uploadProgram");
    }

    private static Program importProgram() throws IOException {
        Program program = new Program();
        Response response = upload(program.json(picture().toUri().toString()));
        assertEquals(303, response.statusCode(), response.asString());
        return program;
    }

    private static Talk talk(String title) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Talk talk = Talk.find("titleEN", title).firstResult();
            if (talk != null) {
                // load what we assert on
                talk.speakers.size();
                if (talk.theme != null) {
                    talk.theme.theme.length();
                }
                if (talk.type != null) {
                    talk.type.typeEN.length();
                }
            }
            return talk;
        });
    }

    private static long count(String entity, String field, String value) {
        return QuarkusTransaction.requiringNew().call(() -> (long) Talk.getEntityManager()
                .createQuery("select count(*) from " + entity + " where " + field + " = ?1").setParameter(1, value)
                .getSingleResult());
    }

    @Test
    public void importCreatesTheTalksWithTheirSpeakersThemesAndTypes() throws IOException {
        Program program = importProgram();

        Talk a = talk(program.titleA);
        assertNotNull(a, "Talk A was not imported");
        assertEquals("Abstract of A", a.descriptionEN);
        assertEquals(Level.Advanced, a.level);
        assertEquals(Language.FR, a.language);
        // the first category and format are used
        assertEquals(program.themeA, a.theme.theme);
        assertEquals(program.conference, a.type.typeEN);
        assertEquals(2, a.speakers.size());
        // so that the speakers can be asked for their slides
        assertNotNull(a.slidesSecret);
        // what we recognise the talk with when importing again
        assertEquals("p1-" + program.uid, a.importId);

        Talk b = talk(program.titleB);
        assertNotNull(b, "Talk B was not imported");
        // no level means the lowest
        assertEquals(Level.Beginner, b.level);
        assertEquals(Language.EN, b.language);
        assertEquals(program.workshop, b.type.typeEN);

        // categories and formats are created once
        assertEquals(1, count("TalkTheme", "theme", program.themeA));
        assertEquals(1, count("TalkTheme", "theme", program.themeB));
        assertEquals(1, count("TalkType", "typeEN", program.conference));
        assertEquals(1, count("TalkType", "typeEN", program.workshop));
        TalkTheme themeA = QuarkusTransaction.requiringNew().call(() -> TalkTheme.find("theme", program.themeA).firstResult());
        TalkTheme themeB = QuarkusTransaction.requiringNew().call(() -> TalkTheme.find("theme", program.themeB).firstResult());
        assertNotEquals(themeA.color, themeB.color, "New themes get different colours");
    }

    @Test
    public void importCreatesSpeakersOnceEvenWhenTheySpeakTwice() throws IOException {
        Program program = importProgram();

        // Jane speaks in both talks, and is only created once
        assertEquals(1, count("Speaker", "email", program.janeEmail));
        Speaker jane = QuarkusTransaction.requiringNew().call(() -> Speaker.find("email", program.janeEmail).firstResult());
        assertEquals("Jane", jane.firstName);
        // everything after the first space is the last name
        assertEquals("van Doe", jane.lastName);
        assertEquals("Jane's bio", jane.biography);
        assertEquals("Acme " + program.uid, jane.company);
        assertEquals("jane_tweets", jane.twitterAccount);
        assertEquals("jane-li", jane.linkedInAccount);
        assertEquals("jane-gh", jane.githubAccount);

        Talk a = talk(program.titleA);
        Talk b = talk(program.titleB);
        assertTrue(a.speakers.stream().anyMatch(s -> s.id.equals(jane.id)));
        assertTrue(b.speakers.stream().anyMatch(s -> s.id.equals(jane.id)));

        // a single name is the last name, and a missing bio is a placeholder
        Speaker cher = QuarkusTransaction.requiringNew().call(() -> Speaker.find("email", program.cherEmail).firstResult());
        assertNull(cher.firstName);
        assertEquals("Cher", cher.lastName);
        assertEquals("To be added", cher.biography);
        assertNull(cher.twitterAccount);
    }

    @Test
    public void importScalesThePictureOfTheSpeakers() throws IOException {
        Program program = importProgram();
        Speaker jane = QuarkusTransaction.requiringNew().call(() -> Speaker.find("email", program.janeEmail).firstResult());

        byte[] photo = given().get("/Application/speakerPhoto/" + jane.id).then().statusCode(200)
                .header("Content-Type", org.hamcrest.Matchers.containsString("image/jpeg"))
                .extract().asByteArray();
        var image = ImageIO.read(new ByteArrayInputStream(photo));
        assertEquals(400, image.getWidth());
        assertEquals(300, image.getHeight());

        // there was no picture for the other one: it gets the default one
        Speaker cher = QuarkusTransaction.requiringNew().call(() -> Speaker.find("email", program.cherEmail).firstResult());
        given().redirects().follow(false).get("/Application/speakerPhoto/" + cher.id).then().statusCode(303);
    }

    @Test
    public void importIgnoresPicturesThatCannotBeLoaded() throws IOException {
        Program program = new Program();
        Response response = upload(program.json(temp.resolve("does-not-exist.png").toUri().toString()));
        assertEquals(303, response.statusCode());
        // the rest is imported
        assertNotNull(talk(program.titleA));
        Speaker jane = QuarkusTransaction.requiringNew().call(() -> Speaker.find("email", program.janeEmail).firstResult());
        assertNotNull(jane);
        assertNull(jane.photo);
    }

    @Test
    public void importingTwiceDoesNotDuplicateAnything() throws IOException {
        Program program = new Program();
        String json = program.json(picture().toUri().toString());
        assertEquals(303, upload(json).statusCode());
        assertEquals(303, upload(json).statusCode());

        assertEquals(1, count("Talk", "titleEN", program.titleA), "Talk A duplicated by a second import");
        assertEquals(1, count("Talk", "titleEN", program.titleB), "Talk B duplicated by a second import");
        assertEquals(1, count("Speaker", "email", program.janeEmail));
        assertEquals(1, count("Speaker", "email", program.cherEmail));
        assertEquals(1, count("TalkTheme", "theme", program.themeA));
        assertEquals(1, count("TalkType", "typeEN", program.conference));
    }

    @Test
    public void importingAgainKeepsTheTalksAsTheyWere() throws Exception {
        Program program = new Program();
        String json = program.json(picture().toUri().toString());
        upload(json);
        Talk before = talk(program.titleA);

        // the organisers edited the talk after the import, a second import must not undo that
        Thread.sleep(1100);
        TestData.update(before, t -> t.titleFR = "Titre français");
        Talk edited = talk(program.titleA);
        upload(json);
        Talk after = talk(program.titleA);

        assertEquals(before.id, after.id);
        assertEquals("Titre français", after.titleFR);
        assertEquals(edited.lastUpdated, after.lastUpdated, "A second import should not touch existing talks");
    }

    @Test
    public void invalidProgramImportsNothing() throws IOException {
        long talks = QuarkusTransaction.requiringNew().call(() -> Talk.count());
        Response response = upload("this is not json");
        assertEquals(303, response.statusCode());
        assertEquals(talks, (long) QuarkusTransaction.requiringNew().call(() -> Talk.count()));
    }
}

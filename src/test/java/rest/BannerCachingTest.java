package rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;
import model.Banner;
import model.BreakType;
import model.Language;
import model.Speaker;
import model.Sponsor;
import model.SponsorShip;
import model.Talk;

@QuarkusTest
public class BannerCachingTest {

    private static final byte[] PNG_SIGNATURE = { (byte) 0x89, 'P', 'N', 'G' };

    @Inject
    UserTransaction tx;

    private Talk createTalk() throws Exception {
        tx.begin();
        Talk talk = new Talk();
        talk.titleEN = "Caching banners";
        talk.isBreak = BreakType.NotABreak;
        talk.language = Language.EN;
        talk.persist();
        tx.commit();
        return talk;
    }

    private static void assertIsPng(byte[] bytes) {
        assertThat(java.util.Arrays.copyOf(bytes, 4), equalTo(PNG_SIGNATURE));
    }

    /** Reads what is stored for a talk, or null if nothing is */
    private String[] storedBanner(Talk talk, String language) throws Exception {
        tx.begin();
        try {
            Banner banner = Banner.find(talk, language);
            if (banner == null) {
                return null;
            }
            Blob png = banner.png;
            return new String[] { banner.id.toString(), banner.etag,
                    new String(png.getBytes(1, (int) png.length()), StandardCharsets.ISO_8859_1) };
        } finally {
            tx.commit();
        }
    }

    private void replaceStoredImage(Talk talk, String language, String content) throws Exception {
        tx.begin();
        Banner banner = Banner.find(talk, language);
        banner.png = Panache.getSession().getLobHelper().createBlob(content.getBytes(StandardCharsets.UTF_8));
        tx.commit();
    }

    private long largeObjects() throws Exception {
        tx.begin();
        try {
            return ((Number) Panache.getEntityManager()
                    .createNativeQuery("select count(*) from pg_largeobject_metadata").getSingleResult()).longValue();
        } finally {
            tx.commit();
        }
    }

    @Test
    public void talkBannerIsCacheable() throws Exception {
        Talk talk = createTalk();
        String url = "/talk-banner/" + talk.id + "/en.png";

        Response first = given().get(url);
        first.then().statusCode(200)
                .header("Content-Type", containsString("image/png"))
                .header("Cache-Control", containsString("max-age=86400"))
                .header("ETag", not(emptyOrNullString()))
                .header("Last-Modified", not(emptyOrNullString()));
        assertIsPng(first.asByteArray());
        String etag = first.header("ETag");
        String lastModified = first.header("Last-Modified");

        // revalidation does not render anything
        given().header("If-None-Match", etag).get(url)
                .then().statusCode(304)
                .header("ETag", etag)
                .header("Cache-Control", containsString("max-age=86400"));
        given().header("If-Modified-Since", lastModified).get(url)
                .then().statusCode(304);

        // other language is a different image
        given().get("/talk-banner/" + talk.id + "/fr.png")
                .then().statusCode(200)
                .header("ETag", not(etag));

        // editing the talk invalidates the cache
        Thread.sleep(1100);
        tx.begin();
        Talk.<Talk> findById(talk.id).titleEN = "Changed title";
        tx.commit();
        Response changed = given().header("If-None-Match", etag).get(url);
        changed.then().statusCode(200).header("ETag", not(etag));

        given().get("/talk-banner/0/en.png").then().statusCode(404);
    }

    @Test
    public void renderedBannerIsMemoized() throws Exception {
        Talk talk = createTalk();
        String url = "/talk-banner/" + talk.id + "/en.png";
        assertThat(storedBanner(talk, "en"), equalTo(null));

        Response first = given().get(url);
        first.then().statusCode(200);
        byte[] rendered = first.asByteArray();
        assertIsPng(rendered);

        // it was stored, with the etag of what we sent
        String[] stored = storedBanner(talk, "en");
        assertThat(stored[1], equalTo(first.header("ETag").replace("\"", "")));
        assertThat(stored[2].getBytes(StandardCharsets.ISO_8859_1), equalTo(rendered));

        // another client without anything cached gets what is stored: replace it to prove we do not render
        replaceStoredImage(talk, "en", "STORED");
        Response second = given().get(url);
        second.then().statusCode(200).header("ETag", first.header("ETag"))
                .header("Content-Type", containsString("image/png"));
        assertThat(new String(second.asByteArray(), StandardCharsets.UTF_8), equalTo("STORED"));

        // when what the banner shows changes, we render again and replace what is stored, in the same row,
        // without leaving a large object behind
        long largeObjects = largeObjects();
        Thread.sleep(1100);
        tx.begin();
        Talk.<Talk> findById(talk.id).titleEN = "Changed title";
        tx.commit();
        Response third = given().get(url);
        third.then().statusCode(200).header("ETag", not(first.header("ETag")));
        assertIsPng(third.asByteArray());
        String[] restored = storedBanner(talk, "en");
        assertThat(restored[0], equalTo(stored[0]));
        assertThat(restored[1], equalTo(third.header("ETag").replace("\"", "")));
        assertThat(largeObjects(), equalTo(largeObjects));

        // each language has its own
        given().get("/talk-banner/" + talk.id + "/fr.png").then().statusCode(200);
        assertThat(storedBanner(talk, "fr")[0], not(equalTo(stored[0])));

        // we only render the languages we have
        given().get("/talk-banner/" + talk.id + "/de.png").then().statusCode(404);
        assertThat(storedBanner(talk, "de"), equalTo(null));

        // the banner goes with the talk
        tx.begin();
        Talk.deleteById(talk.id);
        tx.commit();
        assertThat(storedBanner(talk, "en"), equalTo(null));
        assertThat(storedBanner(talk, "fr"), equalTo(null));
    }

    @Test
    public void sponsorBannerIsCacheable() throws Exception {
        tx.begin();
        Sponsor sponsor = new Sponsor();
        sponsor.company = "ACME";
        sponsor.about = "About";
        sponsor.aboutEN = "About";
        BufferedImage logo = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(logo, "png", png);
        sponsor.logo = Panache.getSession().getLobHelper().createBlob(png.toByteArray());
        sponsor.level = SponsorShip.Gold;
        sponsor.persist();
        tx.commit();
        String url = "/sponsor-banner/" + sponsor.id + "/en.png";
        Response first = given().get(url);
        first.then().statusCode(200).header("Cache-Control", containsString("max-age=86400"));
        assertIsPng(first.asByteArray());
        given().header("If-None-Match", first.header("ETag")).get(url).then().statusCode(304);
        // and a client with nothing cached gets the same image
        assertThat(given().get(url).asByteArray(), equalTo(first.asByteArray()));
    }

    @Test
    public void speakerBannerFollowsItsTalks() throws Exception {
        Talk talk = createTalk();
        tx.begin();
        Speaker speaker = new Speaker();
        speaker.firstName = "Jane";
        speaker.lastName = "Doe";
        speaker.biography = "Bio";
        speaker.persist();
        Talk.<Talk> findById(talk.id).speakers.add(speaker);
        speaker.talks.add(Talk.findById(talk.id));
        tx.commit();
        String url = "/speaker-banner/" + speaker.id + "/en.png";
        Response first = given().get(url);
        first.then().statusCode(200).header("Cache-Control", containsString("max-age=86400"));
        assertIsPng(first.asByteArray());
        String etag = first.header("ETag");
        given().header("If-None-Match", etag).get(url).then().statusCode(304);

        // the banner shows the talk titles
        Thread.sleep(1100);
        tx.begin();
        Talk.<Talk> findById(talk.id).titleEN = "Another title";
        tx.commit();
        given().header("If-None-Match", etag).get(url).then().statusCode(200).header("ETag", not(etag));
    }
}

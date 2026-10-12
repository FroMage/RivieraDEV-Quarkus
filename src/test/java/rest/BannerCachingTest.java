package rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;
import model.BreakType;
import model.Language;
import model.Speaker;
import model.Sponsor;
import model.SponsorShip;
import model.Talk;

@QuarkusTest
public class BannerCachingTest {

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
        given().header("If-None-Match", first.header("ETag")).get(url).then().statusCode(304);
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

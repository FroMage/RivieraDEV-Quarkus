package rest;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import testdata.GoldenFixture;
import testdata.Images;

/**
 * Renders the banners for a fixed talk, speaker and sponsor and compares them with images we checked by eye, so that
 * we notice when a template, a font, a background or the renderer changes how they look.
 *
 * When it changes on purpose, record the new ones with -Dgolden.update=true (and look at them).
 */
@QuarkusTest
public class BannerGoldenTest {

    /** The same machine renders the same pixels, but allow for a different JDK or fonts to move a few of them */
    static final double TOLERANCE = 2.0;

    static GoldenFixture.Ids ids;

    @BeforeAll
    static void createData() {
        ids = GoldenFixture.create();
    }

    private static byte[] banner(String path) {
        return given().get(path).then().statusCode(200).header("Content-Type", "image/png").extract().asByteArray();
    }

    @Test
    public void talkBannerInEnglish() {
        Images.assertLooksLike("talk-banner-en", banner("/talk-banner/" + ids.talk() + "/en.png"), TOLERANCE);
    }

    @Test
    public void talkBannerInFrench() {
        Images.assertLooksLike("talk-banner-fr", banner("/talk-banner/" + ids.talk() + "/fr.png"), TOLERANCE);
    }

    @Test
    public void speakerBanner() {
        Images.assertLooksLike("speaker-banner-en", banner("/speaker-banner/" + ids.speaker() + "/en.png"), TOLERANCE);
    }

    @Test
    public void sponsorBanner() {
        Images.assertLooksLike("sponsor-banner-en", banner("/sponsor-banner/" + ids.sponsor() + "/en.png"), TOLERANCE);
    }
}

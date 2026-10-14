package rest;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import testdata.GoldenFixture;
import testdata.Images;

/**
 * Renders the banners for a fixed talk, speaker and sponsor and compares them with images we checked by eye, so that
 * we notice when a template, a font, a background or the renderer changes how they look. The comparison ignores how
 * machines draw the strokes of the glyphs, see {@link Images}.
 *
 * When it changes on purpose, record the new ones with -Dgolden.update=true (and look at them).
 */
@QuarkusTest
public class BannerGoldenTest {

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
        Images.assertLooksLike("talk-banner-en", banner("/talk-banner/" + ids.talk() + "/en.png"));
    }

    @Test
    public void talkBannerInFrench() {
        Images.assertLooksLike("talk-banner-fr", banner("/talk-banner/" + ids.talk() + "/fr.png"));
    }

    @Test
    public void speakerBanner() {
        Images.assertLooksLike("speaker-banner-en", banner("/speaker-banner/" + ids.speaker() + "/en.png"));
    }

    @Test
    public void sponsorBanner() {
        Images.assertLooksLike("sponsor-banner-en", banner("/sponsor-banner/" + ids.sponsor() + "/en.png"));
    }
}

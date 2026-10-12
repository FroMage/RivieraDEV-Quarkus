package nativetest;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.response.Response;
import testdata.GoldenFixture;
import testdata.Images;

/**
 * Tests of the application as it is deployed: they run against the native executable (when built with -Dnative), as
 * the problems that we had, with the memory and with Java2D, only happen there. They only talk to the application over
 * HTTP, and create their data in its database.
 *
 * Run with: mvn verify -Dnative -Dquarkus.native.container-build=true
 */
@QuarkusIntegrationTest
public class NativeAppIT implements DevServicesContext.ContextAware {

    /**
     * The golden images were recorded on the JVM, the native executable renders with the same code but not exactly
     * the same fonts rendering.
     */
    static final double TOLERANCE = 4.0;

    private DevServicesContext context;

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        this.context = context;
    }

    private Seeder seeder() {
        Map<String, String> properties = context.devServicesProperties();
        return new Seeder(properties.get("quarkus.datasource.jdbc.url"), properties.get("quarkus.datasource.username"),
                properties.get("quarkus.datasource.password"));
    }

    record Fixture(long talk, long speaker, long sponsor) {
    }

    private Fixture goldenFixture(Seeder seeder) {
        long theme = seeder.theme(GoldenFixture.THEME);
        long type = seeder.type(GoldenFixture.TYPE_EN, GoldenFixture.TYPE_FR);
        long[] speakers = GoldenFixture.SPEAKERS.stream()
                .mapToLong(s -> seeder.speaker(s.firstName(), s.lastName(), s.company(), s.photoSeed())).toArray();
        long talk = seeder.talk(GoldenFixture.TALK_TITLE_EN, GoldenFixture.TALK_TITLE_FR, theme, type, speakers);
        long sponsor = seeder.sponsor(GoldenFixture.SPONSOR_COMPANY, GoldenFixture.SPONSOR_LEVEL.name(),
                GoldenFixture.SPONSOR_LOGO_SEED);
        return new Fixture(talk, speakers[0], sponsor);
    }

    private static byte[] png(String path) {
        Response response = given().get(path);
        response.then().statusCode(200).header("Content-Type", "image/png");
        byte[] bytes = response.asByteArray();
        Images.assertIsPng(bytes);
        return bytes;
    }

    @Test
    public void pagesRender() {
        try (Seeder seeder = seeder()) {
            Fixture fixture = goldenFixture(seeder);
            for (String page : List.of("/", "/talks", "/speakers", "/schedule", "/team", "/live",
                    "/talk-banner/" + fixture.talk() + "/en", "/session/" + fixture.talk(),
                    "/speaker/" + fixture.speaker(), "/sponsor/" + fixture.sponsor())) {
                for (String language : List.of("en", "fr")) {
                    given().header("Accept-Language", language).get(page).then().statusCode(200);
                }
            }
            // the photo and the logo come from the database
            png("/Application/speakerPhoto/" + fixture.speaker());
            png("/Application/sponsorLogo/" + fixture.sponsor());
        }
    }

    @Test
    public void bannersLookLikeTheGoldenOnes() {
        try (Seeder seeder = seeder()) {
            Fixture fixture = goldenFixture(seeder);
            Images.assertLooksLike("talk-banner-en", png("/talk-banner/" + fixture.talk() + "/en.png"), TOLERANCE);
            Images.assertLooksLike("talk-banner-fr", png("/talk-banner/" + fixture.talk() + "/fr.png"), TOLERANCE);
            Images.assertLooksLike("speaker-banner-en", png("/speaker-banner/" + fixture.speaker() + "/en.png"),
                    TOLERANCE);
            Images.assertLooksLike("sponsor-banner-en", png("/sponsor-banner/" + fixture.sponsor() + "/en.png"),
                    TOLERANCE);
        }
    }

    @Test
    public void bannersAreStoredAndRenderedAgainOnlyWhenTheyChange() {
        try (Seeder seeder = seeder()) {
            Fixture fixture = goldenFixture(seeder);
            String url = "/talk-banner/" + fixture.talk() + "/en.png";
            assertEquals(0, seeder.count("select count(*) from Banner where talk_id = ?", fixture.talk()));

            Response first = given().get(url);
            first.then().statusCode(200);
            String etag = first.header("ETag");

            // stored with the ETag that we sent
            assertEquals(1, seeder.count("select count(*) from Banner where talk_id = ? and png is not null",
                    fixture.talk()));
            assertEquals(etag.replace("\"", ""),
                    seeder.string("select etag from Banner where talk_id = ? and language = 'en'", fixture.talk()));

            // a client that has nothing gets the same image, and one that has it gets nothing
            assertTrue(java.util.Arrays.equals(first.asByteArray(), given().get(url).asByteArray()),
                    "A second request should get the stored image");
            given().header("If-None-Match", etag).get(url).then().statusCode(304);

            // when the talk changes, the stored image is replaced
            seeder.update("update Talk set titleEN = 'A different title', lastUpdated = now() + interval '1 second'"
                    + " where id = ?", fixture.talk());
            Response changed = given().header("If-None-Match", etag).get(url);
            changed.then().statusCode(200);
            assertNotEquals(etag, changed.header("ETag"));
            assertTrue(!java.util.Arrays.equals(first.asByteArray(), changed.asByteArray()),
                    "The new title should be in the new image");
            assertEquals(changed.header("ETag").replace("\"", ""),
                    seeder.string("select etag from Banner where talk_id = ? and language = 'en'", fixture.talk()));
            assertEquals(1, seeder.count("select count(*) from Banner where talk_id = ?", fixture.talk()));
        }
    }

    /**
     * What crashed production: crawlers fetch the banners of many talks at the same time, and every one that is not
     * stored yet is rendered at once. The application runs with the memory limit of production, see the pom.
     */
    @Test
    public void manyBannersAtOnceDoNotExhaustTheMemory() throws Exception {
        int waves = Integer.getInteger("banners.waves", 5);
        // how many banners are requested at once: change it to find how much the memory limit allows
        int bannersPerWave = Integer.getInteger("banners.per.wave", 40);
        List<List<Long>> talksPerWave = new ArrayList<>();
        try (Seeder seeder = seeder()) {
            long theme = seeder.theme("Many banners");
            long type = seeder.type("Many banners", "Beaucoup de bannières");
            long[] speakers = new long[10];
            for (int i = 0; i < speakers.length; i++) {
                speakers[i] = seeder.speaker("Speaker", "Number" + i, "Company " + i, i);
            }
            for (int wave = 0; wave < waves; wave++) {
                List<Long> talks = new ArrayList<>();
                for (int i = 0; i < bannersPerWave; i++) {
                    int n = wave * bannersPerWave + i;
                    // two speakers each, so that there are photos to load and draw
                    talks.add(seeder.talk("Talk number " + n, "Conférence numéro " + n, theme, type,
                            speakers[n % speakers.length], speakers[(n + 3) % speakers.length]));
                }
                talksPerWave.add(talks);
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(bannersPerWave);
        try {
            for (int wave = 0; wave < waves; wave++) {
                // all at once: nothing is stored for any of them yet
                CountDownLatch start = new CountDownLatch(1);
                List<Future<Integer>> results = new ArrayList<>();
                for (long talk : talksPerWave.get(wave)) {
                    Callable<Integer> request = () -> {
                        start.await();
                        Response response = given().get("/talk-banner/" + talk + "/en.png");
                        if (response.statusCode() == 200) {
                            Images.assertIsPng(response.asByteArray());
                        }
                        return response.statusCode();
                    };
                    results.add(executor.submit(request));
                }
                start.countDown();
                for (Future<Integer> result : results) {
                    assertEquals(200, result.get(120, TimeUnit.SECONDS), "Wave " + wave + " of banners failed");
                }
            }
        } finally {
            executor.shutdownNow();
        }
        // and the application is still there
        given().get("/").then().statusCode(200);
    }
}

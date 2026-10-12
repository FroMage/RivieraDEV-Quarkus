package testdata;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.narayana.jta.QuarkusTransaction;
import model.BreakType;
import model.Language;
import model.Level;
import model.Slot;
import model.Speaker;
import model.Sponsor;
import model.SponsorShip;
import model.Talk;
import model.TalkTheme;
import model.TalkThemeColor;
import model.TalkType;
import model.Track;
import model.User;

/**
 * Creates valid entities for tests, each in its own transaction, and returns them (detached).
 *
 * The database is shared by all the tests, so everything we create is unique and tests must not rely on
 * the database being empty.
 */
public class TestData {

    public static final String ADMIN_USER = "test-admin";
    public static final String ADMIN_PASSWORD = "test-password";

    private static final AtomicInteger counter = new AtomicInteger();

    private static int next() {
        return counter.incrementAndGet();
    }

    private static <T> T inTransaction(Supplier<T> work) {
        return QuarkusTransaction.requiringNew().call(work::get);
    }

    public static Talk talk() {
        return talk(t -> {
        });
    }

    public static Talk talk(Consumer<Talk> customiser) {
        int n = next();
        return inTransaction(() -> {
            Talk talk = new Talk();
            talk.titleEN = "Talk " + n;
            talk.titleFR = "Conférence " + n;
            talk.descriptionEN = "Description of talk " + n;
            talk.descriptionFR = "Description de la conférence " + n;
            talk.isBreak = BreakType.NotABreak;
            talk.language = Language.EN;
            // the program import always sets one, and some templates rely on it
            talk.level = Level.Beginner;
            customiser.accept(talk);
            talk.persist();
            return talk;
        });
    }

    public static Speaker speaker() {
        return speaker(s -> {
        });
    }

    public static Speaker speaker(Consumer<Speaker> customiser) {
        int n = next();
        return inTransaction(() -> {
            Speaker speaker = new Speaker();
            speaker.firstName = "First" + n;
            speaker.lastName = "Last" + n;
            speaker.biography = "Biography of speaker " + n;
            speaker.company = "Company " + n;
            customiser.accept(speaker);
            speaker.persist();
            return speaker;
        });
    }

    public static Sponsor sponsor(SponsorShip level) {
        return sponsor(level, s -> {
        });
    }

    /** A sponsor with a logo, which the sponsor pages and banners need */
    public static Sponsor sponsor(SponsorShip level, Consumer<Sponsor> customiser) {
        int n = next();
        return inTransaction(() -> {
            Sponsor sponsor = new Sponsor();
            sponsor.company = "Sponsor " + n;
            sponsor.about = "About sponsor " + n;
            sponsor.aboutEN = "About sponsor " + n;
            sponsor.level = level;
            sponsor.logo = Panache.getSession().getLobHelper().createBlob(png(100, 50));
            customiser.accept(sponsor);
            sponsor.persist();
            return sponsor;
        });
    }

    public static Track track() {
        int n = next();
        return inTransaction(() -> {
            Track track = new Track();
            track.title = "Track " + n;
            track.position = n;
            track.persist();
            return track;
        });
    }

    public static TalkTheme theme() {
        int n = next();
        return inTransaction(() -> {
            TalkTheme theme = new TalkTheme();
            theme.theme = "Theme " + n;
            theme.color = TalkThemeColor.Blue;
            theme.persist();
            return theme;
        });
    }

    public static TalkType type() {
        int n = next();
        return inTransaction(() -> {
            TalkType type = new TalkType();
            type.typeEN = "Type " + n;
            type.typeFR = "Type " + n;
            type.persist();
            return type;
        });
    }

    public static Slot slot(Date start, Date end) {
        return inTransaction(() -> {
            Slot slot = new Slot();
            slot.startDate = start;
            slot.endDate = end;
            slot.persist();
            return slot;
        });
    }

    /** Makes sure the admin user exists, and returns its name */
    public static String adminUser() {
        return inTransaction(() -> {
            if (User.findByUsername(ADMIN_USER) == null) {
                User user = new User();
                user.firstName = "Test";
                user.lastName = "Admin";
                user.userName = ADMIN_USER;
                user.password = ADMIN_PASSWORD;
                user.persist();
            }
            return ADMIN_USER;
        });
    }

    public static void addSpeaker(Talk talk, Speaker speaker) {
        QuarkusTransaction.requiringNew().run(() -> {
            Talk managedTalk = Talk.findById(talk.id);
            Speaker managedSpeaker = Speaker.findById(speaker.id);
            managedTalk.speakers.add(managedSpeaker);
            managedSpeaker.talks.add(managedTalk);
        });
    }

    /** Applies a change to a talk, in its own transaction (and so triggers the lifecycle callbacks) */
    public static void update(Talk talk, Consumer<Talk> change) {
        QuarkusTransaction.requiringNew().run(() -> change.accept(Talk.findById(talk.id)));
    }

    public static byte[] png(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }
}

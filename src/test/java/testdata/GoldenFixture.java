package testdata;

import java.util.List;

import model.Sponsor;
import model.SponsorShip;
import model.Talk;
import model.TalkTheme;
import model.TalkType;
import model.Speaker;

/**
 * The talk, speakers and sponsor that the golden banners are rendered from: they must always be the same.
 *
 * The integration tests against the native executable cannot use TestData, so they create the same things
 * with SQL, from these values.
 */
public class GoldenFixture {

    public static final String TALK_TITLE_EN = "Building resilient systems with Quarkus";
    public static final String TALK_TITLE_FR = "Construire des systèmes résilients avec Quarkus";
    public static final String THEME = "Cloud & Containers";
    public static final String TYPE_EN = "Conference";
    public static final String TYPE_FR = "Conférence";

    public record SpeakerData(String firstName, String lastName, String company, int photoSeed) {
    }

    public static final List<SpeakerData> SPEAKERS = List.of(
            new SpeakerData("Jane", "Doe", "Acme Corp", 1),
            new SpeakerData("John", "Smith", "Globex", 2));

    public static final String SPONSOR_COMPANY = "Initech";
    public static final SponsorShip SPONSOR_LEVEL = SponsorShip.Gold;
    public static final int SPONSOR_LOGO_SEED = 3;

    public record Ids(long talk, long speaker, long sponsor) {
    }

    public static Ids create() {
        TalkTheme theme = TestData.theme(t -> t.theme = THEME);
        TalkType type = TestData.type(t -> {
            t.typeEN = TYPE_EN;
            t.typeFR = TYPE_FR;
        });
        Talk talk = TestData.talk(t -> {
            t.titleEN = TALK_TITLE_EN;
            t.titleFR = TALK_TITLE_FR;
            t.theme = theme;
            t.type = type;
        });
        Speaker first = null;
        for (SpeakerData data : SPEAKERS) {
            Speaker speaker = TestData.speaker(s -> {
                s.firstName = data.firstName();
                s.lastName = data.lastName();
                s.company = data.company();
                s.photo = TestData.blob(Pictures.photo(data.photoSeed()));
            });
            TestData.addSpeaker(talk, speaker);
            if (first == null) {
                first = speaker;
            }
        }
        Sponsor sponsor = TestData.sponsor(SPONSOR_LEVEL, s -> {
            s.company = SPONSOR_COMPANY;
            s.logo = TestData.blob(Pictures.logo(SPONSOR_LOGO_SEED));
        });
        return new Ids(talk.id, first.id, sponsor.id);
    }
}

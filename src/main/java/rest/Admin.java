package rest;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.pdf.Pdf;
import io.quarkiverse.renarde.util.FileUtils;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Sort;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateData;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.smallrye.common.annotation.Blocking;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import email.Emails;
import model.BreakType;
import model.BufferPost;
import model.Language;
import model.Level;
import model.Organiser;
import model.Slot;
import model.Speaker;
import model.Sponsor;
import model.SponsorShip;
import model.Talk;
import model.TalkTheme;
import model.TalkThemeColor;
import model.TalkType;
import model.TemporarySlot;
import util.ImageUtil;
import util.JavaExtensions;
import services.SponsorBufferSchedulerService;
import services.TalkBufferSchedulerService;
import jakarta.inject.Inject;

@Blocking
@Authenticated
public class Admin extends Controller {

    @Inject
    TalkBufferSchedulerService bufferSchedulerService;

    @Inject
    SponsorBufferSchedulerService sponsorBufferSchedulerService;

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JsonProgram {
        public String name;
        public List<JsonTalk> proposals;
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JsonTalk {
        public String id;
        public String title;
        public String level;
        @JsonProperty("abstract")
        public String abstractField;
        public List<String> categories;
        public List<String> formats;
        public List<String> languages;
        public List<JsonSpeaker> speakers;
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JsonSpeaker {
        public String id;
        public String name;
        public String bio;
        public String company;
        public String picture;
        public List<String> socialLinks;
        public String email;
        public String references;
        public String location;
    }

    @CheckedTemplate
    public static class Templates {
    	public static native TemplateInstance uploadProgramForm();

		public static native TemplateInstance speakerEmails(List<Speaker> speakers);

		public static native TemplateInstance index(List<Date> days);

		public static native TemplateInstance speakerEmailsCompany(List<Speaker> speakers);

		public static native TemplateInstance badges(List<Badge> badges);

    	public static native TemplateInstance badgesForm();

    	public static native TemplateInstance bufferScheduler(TalkBufferSchedulerService.SchedulerStatus status, List<BufferPost> posts);

    	public static native TemplateInstance sponsorScheduler(SponsorBufferSchedulerService.SchedulerStatus status, List<BufferPost> posts);

    	public static native TemplateInstance sponsorTwitter(Map<SponsorShip, List<Sponsor>> sponsors);

    	public static native TemplateInstance slidesSecrets(List<Talk> talks);
    }
    
    public TemplateInstance uploadProgramForm() {
    	return Templates.uploadProgramForm();
    }
    
    @POST
    public void uploadProgram(@RestForm("program") @PartType(MediaType.APPLICATION_OCTET_STREAM) InputStream programInputStream) throws FileNotFoundException, IOException {
        flash("message", "Uploaded stuff");
        Log.info("upload");

        // Workaround a Quarkus bug that I can't declare the parameter as List<JsonTalk>, to be filed
        String content = new BufferedReader(new InputStreamReader(programInputStream, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));
        Log.info("upload 2");
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonProgram program = mapper.readValue(content, new TypeReference<JsonProgram>() {
            });
            Log.info("upload 3");

            Map<String, Speaker> jsonSpeakerIds = new HashMap<>();
            Map<String, TalkTheme> jsonCategoryByName = new HashMap<>();
            Map<String, TalkType> jsonFormatByName = new HashMap<>();

            int color = 0;
            TalkThemeColor[] colors = TalkThemeColor.values();

            Log.info("upload 4");
            for (JsonTalk jsonTalk : program.proposals) {
                Log.infof("Importing %s categories", jsonTalk.categories.size());
                for (String jsonCategory : jsonTalk.categories) {
                    Log.infof(" Category %s", jsonCategory);

                    TalkTheme talkTheme = TalkTheme.find("theme", jsonCategory).firstResult();
                    if (talkTheme == null) {
                        talkTheme = new TalkTheme();
                        talkTheme.theme = jsonCategory;
                        talkTheme.color = colors[color % colors.length];
                        talkTheme.persist();
                        color++;
                    }

                    jsonCategoryByName.put(jsonCategory, talkTheme);
                }

                Log.infof("Importing %s formats", jsonTalk.formats.size());
                for (String jsonFormat : jsonTalk.formats) {
                    Log.infof(" Format %s", jsonFormat);
                    TalkType talkType = TalkType.find("typeEN", jsonFormat).firstResult();
                    if (talkType == null) {
                        talkType = new TalkType();
                        talkType.typeEN = jsonFormat;
                        talkType.persist();
                    }

                    jsonFormatByName.put(jsonFormat, talkType);
                }

                Log.infof("Importing %s speakers", jsonTalk.speakers.size());
                for (JsonSpeaker jsonSpeaker : jsonTalk.speakers) {
                    Log.infof(" Speaker %s, bio: %s", jsonSpeaker.name, jsonSpeaker.bio);
                    Speaker speaker = Speaker.find("email", jsonSpeaker.email).firstResult();
                    if (speaker == null) {

                        speaker = new Speaker();
                        speaker.biography = jsonSpeaker.bio;
                        if (speaker.biography == null || speaker.biography.isBlank()) {
                            speaker.biography = "To be added";
                        }
                        speaker.company = jsonSpeaker.company;
                        speaker.email = jsonSpeaker.email;
                        if (jsonSpeaker.name == null) {
                            speaker.firstName = "Anonymous";
                            speaker.lastName = "Coward";
                        } else {
                            int firstSpace = jsonSpeaker.name.indexOf(' ');
                            if (firstSpace != -1) {
                                speaker.firstName = jsonSpeaker.name.substring(0, firstSpace);
                                speaker.lastName = jsonSpeaker.name.substring(firstSpace + 1);
                            } else {
                                speaker.lastName = jsonSpeaker.name;
                            }
                        }
                        speaker.twitterAccount = jsonSpeaker.socialLinks.stream()
                                .map(s -> s.toLowerCase())
                                .filter(s -> s.contains("twitter.com") || s.contains("x.com"))
                                .map(s -> s.split("/")[s.split("/").length - 1]).findAny().orElse(null);


                        speaker.linkedInAccount = jsonSpeaker.socialLinks.stream()
                                .map(s -> s.toLowerCase())
                                .filter(s -> s.contains("linkedin.com"))
                                .map(s -> s.split("/")[s.split("/").length - 1]).findAny().orElse(null);


                        speaker.githubAccount = jsonSpeaker.socialLinks.stream()
                                .map(s -> s.toLowerCase())
                                .filter(s -> s.contains("github.com"))
                                .map(s -> s.split("/")[s.split("/").length - 1]).findAny().orElse(null);

                        // FIXME: add language, github?
                        if (jsonSpeaker.picture != null) {
                            try (InputStream is = new URL(jsonSpeaker.picture).openStream()) {
                                BufferedImage image = ImageIO.read(is);
                                // this can be null if the stream is empty, I guess
                                if (image != null) {
                                    BufferedImage scaledImage = ImageUtil.scaleImage(image, 400);
                                    image.flush();
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    ImageUtil.writeImage(scaledImage, baos);
                                    scaledImage.flush();
                                    speaker.photo = Panache.getSession().getLobHelper().createBlob(baos.toByteArray());
                                }
                            } catch (FileNotFoundException x) {
                                // ignore, this is a 404
                                Log.infof("Failed to load image from %s: %s", jsonSpeaker.picture, x.getMessage());
                            } catch (IOException x) {
                                // happens for 403 too
                                Log.infof("Failed to load image from %s: %s", jsonSpeaker.picture, x.getMessage());
                            }
                        }
                        speaker.persist();
                    }
                    jsonSpeakerIds.put(jsonSpeaker.email, speaker);
                }
            }

            Log.infof("Importing %s talks", program.proposals.size());
            for (JsonTalk jsonTalk : program.proposals) {
                Log.infof(" Talk %s", jsonTalk.id);
                Talk talk = Talk.find("importId", jsonTalk.id).firstResult();
                if (talk == null) {
                    talk = new Talk();
                    talk.descriptionEN = jsonTalk.abstractField;
                    // bullshit
                    talk.importId = jsonTalk.title;
                    if (jsonTalk.level == null)
                        talk.level = Level.Beginner;
                    else
                        talk.level = Level.valueOf(io.quarkiverse.renarde.util.JavaExtensions.capitalised(jsonTalk.level.toLowerCase()));
                    talk.titleEN = jsonTalk.title;
                    for (JsonSpeaker speaker : jsonTalk.speakers) {
                        talk.speakers.add(jsonSpeakerIds.get(speaker.email));
                    }
                    talk.isBreak = BreakType.NotABreak;
                    talk.type = jsonFormatByName.get(jsonTalk.formats.get(0));
                    if (jsonTalk.categories != null) {
                        talk.theme = jsonCategoryByName.get(jsonTalk.categories.get(0));
                    }
                    talk.language = jsonTalk.languages.contains("en") ? Language.EN : Language.FR;
                    talk.persist();
                } else {
                    Log.infof(" Talk %s already exists", jsonTalk.id);
                }
            }
        }catch(Exception x) {
            x.printStackTrace();
        }
        uploadProgramForm();
    }
    
    @Transactional
    public Response speakerPhotosZip() throws IOException, SQLException {
        List<Speaker> speakers = Speaker.listAll();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        Map<String,Integer> dupeCount = new HashMap<>();
        try(ZipOutputStream zos = new ZipOutputStream(baos)){
            for (Speaker speaker : speakers) {
                if(speaker.photo == null)
                    continue;
                byte[] photo = speaker.photo.getBytes(1, (int) speaker.photo.length());
                String type = FileUtils.getMimeType(null, photo);
                int slash = type.lastIndexOf('/');
                String ext = slash != -1 ? type.substring(slash+1) : type;
                String name = speaker.firstName+"-"+speaker.lastName;
                name = JavaExtensions.removeAccents(name);
                Integer count = dupeCount.get(name);
                if(count != null) {
                    count++;
                    name += "-"+count;
                    dupeCount.put(name, count);
                } else {
                    dupeCount.put(name, 1);
                }
                zos.putNextEntry(new ZipEntry(name+"."+ext));
                zos.write(photo);
            }
        }
        byte[] bytes = baos.toByteArray();
        return Response.ok(bytes, "application/zip").header("Content-Disposition", "attachment; filename=\"speaker-photos.zip\"").build();
    }
    
    public TemplateInstance speakerEmails() {
        List<Speaker> speakers = Speaker.list("ORDER BY firstName,lastName");
        return Templates.speakerEmails(speakers);
    }

    public TemplateInstance speakerEmailCompany() {
        List<Speaker> speakers = Speaker.list("ORDER BY firstName,lastName");
        return Templates.speakerEmailsCompany(speakers);
    }

    @TemplateData
    public static record Badge(String firstName, String lastName, String company, String level, String email) {
    	public String getVcard() {
    		StringBuilder sb = new StringBuilder();
    		sb.append("BEGIN:VCARD\n");
    		sb.append("VERSION:3.0\n");
    		sb.append("N:").append(lastName).append(";").append(firstName).append(";;;\n");
    		sb.append("FN:").append(firstName).append(" ").append(lastName).append("\n");
    		sb.append("ORG:").append(company).append(";\n");
    		if(email != null) {
    			sb.append("EMAIL;type=INTERNET:").append(email).append("\n");
    		}
    		sb.append("END:VCARD\n");
    		return sb.toString();
    	}
    }
    
    public TemplateInstance speakerBadges() {
    	List<Speaker> speakers = Speaker.listAll(Sort.by("lastName").and("firstName"));
    	List<Badge> badges = new ArrayList<Badge>();
    	for (Speaker speaker : speakers) {
			badges.add(new Badge(speaker.firstName, speaker.lastName, speaker.company, "SPEAKER", speaker.email));
		}
    	return Templates.badges(badges);
    }

    public TemplateInstance organiserBadges() {
    	List<Organiser> organisers = Organiser.listAll(Sort.by("lastName").and("firstName"));
    	List<Badge> badges = new ArrayList<Badge>();
    	for (Organiser organiser : organisers) {
			badges.add(new Badge(organiser.firstName, organiser.lastName, organiser.company, "STAFF", null));
		}
    	return Templates.badges(badges);
    }

    public TemplateInstance sponsorBadges() {
    	List<Sponsor> sponsors = Sponsor.listAll(Sort.by("company"));
    	List<Badge> badges = new ArrayList<Badge>();
    	for (Sponsor sponsor : sponsors) {
    		if(sponsor.level != SponsorShip.PreviousYears) {
    			badges.add(new Badge(sponsor.company, "", sponsor.company, "SPONSOR", null));
    		}
		}
    	return Templates.badges(badges);
    }

    @POST
    public TemplateInstance badges(@RestForm File csv) throws IOException {
    	CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setSkipHeaderRecord(true)
                .build();
    	List<Badge> badges = new ArrayList<Badge>();
        try (Reader reader = new FileReader(csv)) {
            // Generate one document per row, using the specified syntax.
            Iterable<CSVRecord> records = csvFormat.parse(reader);
            int i = 1;
            for (CSVRecord record : records) {
            	badges.add(new Badge(record.get(0), record.get(1), record.get(2), record.get(3), record.get(4)));
            }
        }
        return Templates.badges(badges);
    }

    public TemplateInstance badgesForm() {
    	return Templates.badgesForm();
    }

    public TemplateInstance index() {
        List<Date> days = (List)Slot.list(
                    "select distinct date_trunc('day', startDate) from Slot ORDER BY date_trunc('day', startDate)");

        return Templates.index(days);
    }

    public TemplateInstance bufferScheduler() {
        TalkBufferSchedulerService.SchedulerStatus status = bufferSchedulerService.getStatus();
        List<BufferPost> posts = BufferPost.list("talk IS NOT NULL");
        return Templates.bufferScheduler(status, posts);
    }

    @POST
    public void startBufferScheduler() {
        try {
            bufferSchedulerService.startScheduler();
            flash("message", "Buffer scheduler started successfully");
        } catch (Exception e) {
            Log.errorf(e, "Failed to start Buffer scheduler");
            flash("error", "Failed to start scheduler: " + e.getMessage());
        }
        bufferScheduler();
    }

    @POST
    public void stopBufferScheduler() {
        bufferSchedulerService.stopScheduler();
        flash("message", "Buffer scheduler stopped");
        bufferScheduler();
    }

    @POST
    public void deleteBufferPost(@RestPath Long id, @RestForm String returnTo) {
        try {
            bufferSchedulerService.deleteBufferPost(id);
            flash("message", "Buffer post deleted successfully");
        } catch (Exception e) {
            Log.errorf(e, "Failed to delete Buffer post #%d", id);
            flash("error", "Failed to delete Buffer post: " + e.getMessage());
        }
        if ("sponsor".equals(returnTo)) {
            sponsorScheduler();
        } else {
            bufferScheduler();
        }
    }

    public TemplateInstance sponsorScheduler() {
        SponsorBufferSchedulerService.SchedulerStatus status = sponsorBufferSchedulerService.getStatus();
        List<BufferPost> posts = BufferPost.list("sponsor IS NOT NULL");
        return Templates.sponsorScheduler(status, posts);
    }

    @POST
    public void startSponsorScheduler() {
        try {
            sponsorBufferSchedulerService.startScheduler();
            flash("message", "Sponsor scheduler started successfully");
        } catch (Exception e) {
            Log.errorf(e, "Failed to start Sponsor scheduler");
            flash("error", "Failed to start scheduler: " + e.getMessage());
        }
        sponsorScheduler();
    }

    @POST
    public void stopSponsorScheduler() {
        sponsorBufferSchedulerService.stopScheduler();
        flash("message", "Sponsor scheduler stopped");
        sponsorScheduler();
    }

    public TemplateInstance sponsorTwitter() {
        Map<SponsorShip, List<Sponsor>> sponsors = Sponsor.getSponsorsToDisplay().getSponsors();
        return Templates.sponsorTwitter(sponsors);
    }

    public TemplateInstance slidesSecrets() {
        List<Talk> talks = Talk.list("isBreak = ?1", BreakType.NotABreak);
        return Templates.slidesSecrets(talks);
    }

    private String[] speakerEmails(Talk talk) {
        return talk.speakers.stream()
            .map(s -> s.email)
            .filter(e -> e != null && !e.isBlank())
            .toArray(String[]::new);
    }

    @POST
    public void sendSlidesEmails() {
        List<Talk> talks = Talk.list("isBreak = ?1 AND slidesUrl IS NULL", BreakType.NotABreak);
        int sent = 0;
        List<String> errors = new ArrayList<>();
        for (Talk talk : talks) {
            String[] emails = speakerEmails(talk);
            if (emails.length == 0) {
                continue;
            }
            try {
                Emails.slidesRequest(talk, emails);
                sent++;
            } catch (Exception e) {
                Log.errorf(e, "Failed to send slides email for talk %s", talk.getTitle());
                errors.add(talk.getTitle() + " <" + String.join(", ", emails) + ">: " + e.getMessage());
            }
        }
        flash("message", "Sent " + sent + " email(s)");
        if (!errors.isEmpty()) {
            flash("error", "Failed to send: " + String.join("; ", errors));
        }
        slidesSecrets();
    }

    @POST
    public void sendTestSlidesEmail(@RestPath Long id) {
        Talk talk = Talk.findById(id);
        notFoundIfNull(talk);
        try {
            Emails.slidesRequest(talk, "info@rivieradev.fr");
            flash("message", "Test email sent to info@rivieradev.fr for: " + talk.getTitle());
        } catch (Exception e) {
            Log.errorf(e, "Failed to send test slides email for talk %s", talk.getTitle());
            flash("error", "Failed to send test email: " + e.getMessage());
        }
        slidesSecrets();
    }

    @POST
    public void sendSlidesEmailForTalk(@RestPath Long id) {
        Talk talk = Talk.findById(id);
        notFoundIfNull(talk);
        String[] emails = speakerEmails(talk);
        if (emails.length == 0) {
            flash("error", "No speaker emails for: " + talk.getTitle());
            slidesSecrets();
            return;
        }
        try {
            Emails.slidesRequest(talk, emails);
            flash("message", "Email sent to " + String.join(", ", emails) + " for: " + talk.getTitle());
        } catch (Exception e) {
            Log.errorf(e, "Failed to send slides email for talk %s", talk.getTitle());
            flash("error", "Failed to send email: " + e.getMessage());
        }
        slidesSecrets();
    }
}

package rest;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.hibernate.engine.jdbc.BlobProxy;
import org.jboss.resteasy.reactive.PartType;
import org.jboss.resteasy.reactive.RestForm;

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
import model.BreakType;
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

@Blocking
@Authenticated
public class Admin extends Controller {
    
    public static class ProgramUpload {
        public String name;
        public List<JsonCategory> categories;
        public List<JsonFormat> formats;
        public List<JsonTalk> talks;
        public List<JsonSpeaker> speakers;
    }
    
    public static class JsonCategory {
        public String id;
        public String name;
    }

    public static class JsonFormat {
        public String id;
        public String name;
        public String description;
    }

    public static class JsonTalk {
        public String title;
        public String state;
        public String level;
        @JsonProperty("abstract")
        public String abstractField;
        public String categories;
        public String formats;
        public String[] speakers;
        public String comments;
        public String references;
        public double rating;
        public int loves;
        public int hates;
        public String language;
    }

    public static class JsonSpeaker {
        public String uid;
        public String displayName;
        public String bio;
        public String company;
        public String photoURL;
        public String twitter;
        public String github;
        public String language;
        public String email;
        public String phone;
    }

//    public static class StringOrListDeserializer implements JsonDeserializer<String[]> {
//
//        @Override
//        public String[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
//            if (json instanceof JsonArray) {
//                return new Gson().fromJson(json, String[].class);
//            }
//            String child = context.deserialize(json, String.class);
//            return new String[] { child };
//        }
//    }

    @CheckedTemplate
    public static class Templates {
    	public static native TemplateInstance uploadProgramForm();

		public static native TemplateInstance speakerEmails(List<Speaker> speakers);

		public static native TemplateInstance index(List<Date> days);

		public static native TemplateInstance speakerEmailsCompany(List<Speaker> speakers);

		public static native TemplateInstance badges(List<Badge> badges);
		
    	public static native TemplateInstance badgesForm();
    }
    
    public TemplateInstance uploadProgramForm() {
    	return Templates.uploadProgramForm();
    }
    
    @POST
    public void uploadProgram(@RestForm @PartType(MediaType.APPLICATION_JSON) ProgramUpload program) throws FileNotFoundException, IOException {
        flash("message", "Uploaded stuff");
        
        Map<String,Speaker> jsonSpeakerIds = new HashMap<>();
        Map<String,TalkTheme> jsonCategoryIds = new HashMap<>();
        Map<String,TalkType> jsonFormatIds = new HashMap<>();

        int color = 0;
        TalkThemeColor[] colors = TalkThemeColor.values();

        if(program.categories != null) {
        	Log.infof("Importing %s categories", program.categories.size());
        	for (JsonCategory jsonCategory : program.categories) {
        		Log.infof(" Category %s", jsonCategory.name);

        		TalkTheme talkTheme = TalkTheme.find("importId", jsonCategory.id).firstResult();
        		if(talkTheme == null) {
        			talkTheme = new TalkTheme();
        			talkTheme.theme = jsonCategory.name;
        			talkTheme.color = colors[color % colors.length];
        			talkTheme.importId = jsonCategory.id;
        			talkTheme.persist();
        		}

        		jsonCategoryIds.put(jsonCategory.id, talkTheme);
        		color++;
        	}
        }

        Log.infof("Importing %s formats", program.formats.size());
        for (JsonFormat jsonFormat : program.formats) {
        	Log.infof(" Format %s", jsonFormat.name);
        	TalkType talkType = TalkType.find("importId", jsonFormat.id).firstResult();
        	if(talkType == null) {
        		talkType = new TalkType();
        		talkType.typeEN = jsonFormat.name;
        		talkType.importId = jsonFormat.id;
        		talkType.persist();
        	}

        	jsonFormatIds.put(jsonFormat.id, talkType);
        }

        Log.infof("Importing %s speakers", program.speakers.size());
        for (JsonSpeaker jsonSpeaker : program.speakers) {
        	Log.infof(" Speaker %s, bio: %s", jsonSpeaker.displayName, jsonSpeaker.bio);
        	Speaker speaker = Speaker.find("importId", jsonSpeaker.uid).firstResult();
        	if(speaker == null) {

        		speaker = new Speaker();
        		speaker.biography = jsonSpeaker.bio;
        		if(speaker.biography == null || speaker.biography.isBlank()) {
        			speaker.biography = "To be added";
        		}
        		speaker.company = jsonSpeaker.company;
        		speaker.email = jsonSpeaker.email;
        		speaker.importId = jsonSpeaker.uid;
        		if(jsonSpeaker.displayName == null) {
        			speaker.firstName = "Anonymous";
        			speaker.lastName = "Coward";
        		} else {
        			int firstSpace = jsonSpeaker.displayName.indexOf(' ');
        			if(firstSpace != -1) {
        				speaker.firstName = jsonSpeaker.displayName.substring(0, firstSpace);
        				speaker.lastName = jsonSpeaker.displayName.substring(firstSpace+1);
        			} else {
        				speaker.lastName = jsonSpeaker.displayName;
        			}
        		}
        		if(jsonSpeaker.twitter != null) {
        			if(jsonSpeaker.twitter.startsWith("@"))
        				speaker.twitterAccount = jsonSpeaker.twitter.substring(1);
        			else if(jsonSpeaker.twitter.startsWith("https://twitter.com"))
        				speaker.twitterAccount = jsonSpeaker.twitter.substring(19);
        			else
        				speaker.twitterAccount = jsonSpeaker.twitter;
        		}
        		speaker.phone = jsonSpeaker.phone;

        		// FIXME: add language, github?
        		if(jsonSpeaker.photoURL != null) {
        			try(InputStream is = new URL(jsonSpeaker.photoURL).openStream()){
        				BufferedImage image = ImageIO.read(is);
        				// this can be null if the stream is empty, I guess
        				if(image != null) {
        					BufferedImage scaledImage = ImageUtil.scaleImage(image, 400);
        					ByteArrayOutputStream baos = new ByteArrayOutputStream();
        					ImageUtil.writeImage(scaledImage, baos);
        					speaker.photo = BlobProxy.generateProxy(baos.toByteArray());
        				}
        			} catch (FileNotFoundException x) {
        				// ignore, this is a 404
        				Log.infof("Failed to load image from %s: %s", jsonSpeaker.photoURL, x.getMessage());
        			} catch (IOException x) {
        				// happens for 403 too
        				Log.infof("Failed to load image from %s: %s", jsonSpeaker.photoURL, x.getMessage());
        			}
        		}
        		speaker.persist();
        	}                
        	jsonSpeakerIds.put(jsonSpeaker.uid, speaker);
        }

        Log.infof("Importing %s talks", program.talks.size());
        for (JsonTalk jsonTalk : program.talks) {
        	Log.infof(" Talk %s", jsonTalk.title);
        	Talk talk = Talk.find("importId", jsonTalk.title).firstResult();
        	if(talk == null) {
        		talk = new Talk();
        		talk.descriptionEN = jsonTalk.abstractField;
        		// bullshit
        		talk.importId = jsonTalk.title;
        		if(jsonTalk.level == null)
        			talk.level = Level.Beginner;
        		else
        			talk.level = Level.valueOf(io.quarkiverse.renarde.util.JavaExtensions.capitalised(jsonTalk.level));
        		talk.titleEN = jsonTalk.title;
        		for (String jsonSpeakerId : jsonTalk.speakers) {
        			talk.speakers.add(jsonSpeakerIds.get(jsonSpeakerId));
        		}
        		talk.isBreak = BreakType.NotABreak;
        		// FIXME: state?
        		talk.type = jsonFormatIds.get(jsonTalk.formats);
        		if(jsonTalk.categories != null) {
        			talk.theme = jsonCategoryIds.get(jsonTalk.categories);
        		}
        		// FIXME:
        		talk.language = Language.FR;
        	}

        	talk.persist();
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
}

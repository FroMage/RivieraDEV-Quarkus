package rest;

import java.sql.Blob;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.server.SimpleResourceInfo;
import org.jboss.resteasy.reactive.server.core.CurrentRequestManager;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

import io.quarkiverse.renarde.security.ControllerWithUser;
import io.quarkiverse.renarde.util.FileUtils;
import io.quarkus.arc.Arc;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateGlobal;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import model.Configuration;
import model.Language;
import model.Level;
import model.Organiser;
import model.PreviousSpeaker;
import model.PricePack;
import model.PricePackCurrentState;
import model.PricePackDate;
import model.PricePackPeriod;
import model.Slot;
import model.Speaker;
import model.Sponsor;
import model.SponsorShip;
import model.Talk;
import model.TalkTheme;
import model.TalkType;
import model.TemporarySlot;
import model.Track;
import model.User;
import util.DateUtils;

@Blocking
public class Application extends ControllerWithUser<User> {

	@CheckedTemplate
	public static class Templates {
		public static native TemplateInstance index(String promotedPage2, boolean displayCountdown, String eventStartDateStr, 
				String eventEndDateStr, String googleMapApiKey, boolean displayPreviousSpeakers, 
				Map<SponsorShip, List<Sponsor>> sponsors, boolean lunchesAndPartySoldOut, 
				List<Sponsor> sponsorsPreviousYears, List<Speaker> speakersPreviousYears, 
				List<Speaker> speakersStar, String sponsoringLeafletUrl, String cancelledUrl);

		public static native TemplateInstance photos();

		public static native TemplateInstance access();

		public static native TemplateInstance coc();

		public static native TemplateInstance subscribe(String ticketingUrl, boolean ticketingIsOpen,
				String ticketingTrainingUrl, boolean ticketingTrainingIsOpen,
				List<PricePackCurrentState> pricePackCurrentStateList);

		public static native TemplateInstance schedule(boolean displayFullSchedule, boolean displayNewSpeakers,
				boolean displayTalks, List<Date> days, List<Track> tracks, Language[] languages,
				Map<Date, List<Track>> tracksPerDays, List<TalkTheme> themes, List<TalkType> types, Level[] levels);

		public static native TemplateInstance scheduleSuperSecret(List<Date> days, List<Track> tracks,
				Map<Date, List<Track>> tracksPerDays, List<TalkTheme> themes, List<TalkType> types, Level[] levels,
				Language[] languages);

		public static native TemplateInstance talks(List<TalkTheme> themes, Level[] levels, List<Talk> talks,
				List<TalkType> types, Language[] languages);

		public static native TemplateInstance speakers(List<Speaker> speakers, List<Speaker> speakersPreviousYears,
				boolean displayPreviousSpeakers);

		public static native TemplateInstance speaker(Speaker speaker);

		public static native TemplateInstance sponsor(Sponsor sponsor);

		public static native TemplateInstance talk(Talk talk, boolean displayFullSchedule);

		public static native TemplateInstance organisers(List<Organiser> orgas);

		public static native TemplateInstance organiser(Organiser orga);

		public static native TemplateInstance fishMarket(List<Track> tracksForDay, Date day);

		public static native TemplateInstance live(List<Track> tracks);

		public static native TemplateInstance liveTrack(List<Track> tracks, String track, List<Talk> keynotes);

		public static native TemplateInstance schools(SponsorShip sponsorShip, List<Sponsor> sponsors);
	}
	
	public static class RenardeRequest {
		public static final RenardeRequest INSTANCE = new RenardeRequest();
		
		public String getUrl() {
	        ResteasyReactiveRequestContext otherHttpContextObject = CurrentRequestManager.get();
	        return otherHttpContextObject.getAbsoluteURI();
		}
		
		public String getAction() {
	        ResteasyReactiveRequestContext otherHttpContextObject = CurrentRequestManager.get();
	        SimpleResourceInfo info = otherHttpContextObject.getTarget().getSimplifiedResourceInfo();
			return info.getResourceClass().getSimpleName() + "." + info.getMethodName();
		}
	}
	
    @TemplateGlobal
    public static class ApplicationGlobals {
    	
    	// FIXME: move to Renarde
    	public static RenardeRequest request() {
    		return RenardeRequest.INSTANCE;
    	}
    	
    	public static String promotedPage() {
    		return Configuration.getPromotedPage();
    	}
    	public static boolean displayTalks() {
    		return Configuration.displayTalks();
    	}
    	public static boolean ticketingIsOpen() {
    		return Configuration.ticketingIsOpen();
    	}
    	public static boolean displayNewSpeakers() {
    		return Configuration.displayNewSpeakers();
    	}
    	public static boolean cfpIsOpened() {
    		return Configuration.cfpIsOpened();
    	}
    	public static String cfpUrl() {
    		return Configuration.getCfpUrl();
    	}
    }

    public void fr(@RestQuery String url) {
        i18n.set("fr");
        seeOther(url);
    }

    public void en(@RestQuery String url) {
        i18n.set("en");
        seeOther(url);
    }

    @Path("/")
    public TemplateInstance index() {
        String promotedPage2 = Configuration.getPromotedPage2();

        String eventStartDateStr = Configuration.getEventStartDate();
        String eventEndDateStr = Configuration.getEventEndDate();
        boolean displayCountdown = false;
        if (eventStartDateStr != null && eventEndDateStr != null) {
            SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            Date now = new Date();
            try {
                Date eventStartDate = isoDateFormat.parse(eventStartDateStr);
                Date eventEndDate = isoDateFormat.parse(eventEndDateStr); // Just test that the format is correct
                displayCountdown = now.before(eventStartDate);
            } catch (ParseException e) {
                e.printStackTrace();
                // Do nothing more, displayCountdown is already set to false
            }
        }

        String googleMapApiKey = Configuration.getGoogleMapApiKey();

        Sponsor.SponsorsToDisplay sponsorsToDisplay = Sponsor.getSponsorsToDisplay();
        Map<SponsorShip, List<Sponsor>> sponsors = sponsorsToDisplay.getSponsors();
        List<Sponsor> sponsorsPreviousYears = sponsorsToDisplay.getSponsorsPreviousYears();
        List<Speaker> speakersPreviousYears = PreviousSpeaker.list("ORDER BY lastName, firstName");
        List<Speaker> speakersStar = Speaker.list("star = true ORDER BY lastName, firstName");

        boolean lunchesAndPartySoldOut = sponsors.get(SponsorShip.Lunches) != null
                && sponsors.get(SponsorShip.Lunches).size() > 0 && sponsors.get(SponsorShip.Party) != null
                && sponsors.get(SponsorShip.Party).size() > 0;

        boolean displayPreviousSpeakers = !Configuration.displayNewSpeakers();

        String sponsoringLeafletUrl = Configuration.getSponsoringLeafletUrl();
        
        String cancelledUrl = Configuration.getCancelledUrl();

        return Templates.index(promotedPage2, displayCountdown, eventStartDateStr, eventEndDateStr, googleMapApiKey,
                displayPreviousSpeakers, sponsors, lunchesAndPartySoldOut, sponsorsPreviousYears, speakersPreviousYears,
                speakersStar, sponsoringLeafletUrl, cancelledUrl);
    }

    @Path("/photos")
    public TemplateInstance photos() {
        return Templates.photos();
    }

    @Path("/about")
    public void about() {
        organisers();
    }

    @Path("/access")
    public TemplateInstance access() {
        return Templates.access();
    }

    @Path("/coc")
    public TemplateInstance coc() {
        return Templates.coc();
    }

    @Path("/subscribe")
    public TemplateInstance subscribe() {
        String ticketingUrl = Configuration.getTicketingUrl();
        boolean ticketingIsOpen = Configuration.ticketingIsOpen();
        String ticketingTrainingUrl = Configuration.getTicketingTrainingUrl();
        boolean ticketingTrainingIsOpen = Configuration.ticketingTrainingIsOpen();

        List<PricePack> pricePacks = PricePack.listAll();
        List<PricePackDate> pricePackDatesList = PricePackDate.listAll();
        PricePackDate pricePackDates = null;
        if (pricePackDatesList != null && pricePackDatesList.size() >= 1) {
            pricePackDates = pricePackDatesList.get(0);
        }
        List<PricePackCurrentState> pricePackCurrentStateList = new ArrayList<PricePackCurrentState>();
        Date now = new Date();
        for (PricePack pricePack : pricePacks) {
            Integer currentPrice = null;
            Integer maxPrice = null;
            PricePackPeriod currentPeriod = null;
            Long remainingDays = null;
            if (now.before(pricePackDates.blindBirdEndDate)) {
                currentPeriod = PricePackPeriod.BLIND_BIRD;
                currentPrice = pricePack.blindBirdPrice;
                maxPrice = pricePack.regularPrice;
                remainingDays = DateUtils.getDaysBetweenDates(now, pricePackDates.blindBirdEndDate);
            } else if (now.before(pricePackDates.earlyBirdEndDate)) {
                currentPeriod = PricePackPeriod.EARLY_BIRD;
                currentPrice = pricePack.earlyBirdPrice;
                maxPrice = pricePack.regularPrice;
                remainingDays = DateUtils.getDaysBetweenDates(now, pricePackDates.earlyBirdEndDate);
            } else {
                currentPeriod = PricePackPeriod.REGULAR;
                currentPrice = pricePack.regularPrice;
                if (now.before(pricePackDates.regularEndDate)) {
                    remainingDays = DateUtils.getDaysBetweenDates(now, pricePackDates.regularEndDate);
                }
            }
            pricePackCurrentStateList.add(new PricePackCurrentState(pricePack.type, currentPrice, maxPrice,
                    pricePack.studentPrice, currentPeriod, remainingDays, pricePack.soldOut));
        }

        return Templates.subscribe(ticketingUrl, ticketingIsOpen, ticketingTrainingUrl, ticketingTrainingIsOpen, pricePackCurrentStateList);
    }

    @Path("/schedule")
    public TemplateInstance schedule() {
        boolean displayFullSchedule = Configuration.displayFullSchedule();
        boolean displayNewSpeakers = Configuration.displayNewSpeakers();
        boolean displayTalks = Configuration.displayTalks();

        List<Date> days = null;
        if (!displayFullSchedule) {
            days = (List)TemporarySlot.list(
                    "select distinct date_trunc('day', startDate) from TemporarySlot ORDER BY date_trunc('day', startDate)");
        } else {
            days = (List)Slot.list(
                    "select distinct date_trunc('day', startDate) from Slot ORDER BY date_trunc('day', startDate)");
        }
        List<Track> tracks = Track.listAll();
        Collections.sort(tracks);
        List<TalkTheme> themes = TalkTheme.findUsedThemes();
        List<TalkType> types = TalkType.findUsedTypes();
        Collections.sort(types);
        Level[] levels = Level.values();
        Language[] languages = Language.values();
        Map<Date, List<Track>> tracksPerDays = new HashMap<Date, List<Track>>();
        for (Date day : days) {
            List<Track> tracksPerDay = Talk.findTracksPerDay(day);
            Collections.sort(tracksPerDay);
            tracksPerDays.put(day, tracksPerDay);
        }

        return Templates.schedule(displayFullSchedule, displayNewSpeakers, displayTalks, days, tracks, languages, tracksPerDays, themes, types, levels);
    }

    @Path("/schedule-supersecret")
    public TemplateInstance scheduleSuperSecret() {
        List<Date> days = (List)Slot
                .list("select distinct date_trunc('day', startDate) from Slot ORDER BY date_trunc('day', startDate)");
        List<Track> tracks = Track.listAll();
        List<TalkTheme> themes = TalkTheme.findUsedThemes();
        List<TalkType> types = TalkType.findUsedTypes();
        Collections.sort(types);
        Level[] levels = Level.values();
        Map<Date, List<Track>> tracksPerDays = new HashMap<Date, List<Track>>();
        for (Date day : days) {
            List<Track> tracksPerDay = Talk.findTracksPerDay(day);
            Collections.sort(tracksPerDay);
            tracksPerDays.put(day, tracksPerDay);
        }
        Language[] languages = Language.values();
        return Templates.scheduleSuperSecret(days, tracks, tracksPerDays, themes, types, levels, languages);
    }

    @Path("/talks")
    public TemplateInstance talks() {
        if (!Configuration.displayTalks()) {
            index();
        }

        List<TalkTheme> themes = TalkTheme.findUsedThemes();
        Level[] levels = Level.values();
        List<TalkType> types = TalkType.findUsedTypes();
        Collections.sort(types);
        List<Talk> talks = Talk.list("isHiddenInTalksPage = false");
        Collections.sort(talks);
        Language[] languages = Language.values();
        return Templates.talks(themes, levels, talks, types, languages);
    }
    
    @Path("/speakers")
    public TemplateInstance speakers() {
        List<Speaker> speakers = Speaker.list("ORDER BY lastName, firstName");
        List<Speaker> speakersPreviousYears = PreviousSpeaker.list("ORDER BY lastName, firstName");
        boolean displayPreviousSpeakers = !Configuration.displayNewSpeakers();

        return Templates.speakers(speakers, speakersPreviousYears, displayPreviousSpeakers);
    }

    @Path("/speaker")
    public TemplateInstance speaker(@RestPath Long id) {
        Speaker speaker = Speaker.findById(id);
        notFoundIfNull(speaker);
        return Templates.speaker(speaker);
    }

    @Path("/sponsor")
    public TemplateInstance sponsor(@RestPath Long id) {
        Sponsor sponsor = Sponsor.findById(id);
        notFoundIfNull(sponsor);
        return Templates.sponsor(sponsor);
    }

    @Path("/session")
    public TemplateInstance talk(@RestPath Long id) {
        Talk talk = Talk.findById(id);
        notFoundIfNull(talk);
        boolean displayFullSchedule = Configuration.displayFullSchedule();
        return Templates.talk(talk, displayFullSchedule);
    }

    @Path("/sponsors")
    public void sponsors() {
        // Redirect to an anchor on home page
        seeOther("/#sponsors");

        // Keep old code because previous sponsors is not yet implemented on new site
        // SponsorsToDisplay sponsorsToDisplay = getSponsorsToDisplay();
        // Map<SponsorShip, List<Sponsor>> sponsors = sponsorsToDisplay.getSponsors();
        // List<Sponsor> sponsorsPreviousYears =
        // sponsorsToDisplay.getSponsorsPreviousYears();

        // render(sponsors, sponsorsPreviousYears);
    }

    @Path("/becomeSponsor")
    public void becomeSponsor() {
        String sponsoringLeafletUrl = Configuration.getSponsoringLeafletUrl();

        // Until becomeSponsor.html has a new design
        seeOther(sponsoringLeafletUrl);

        // render(sponsoringLeafletUrl);
    }

    @Transactional
    public Response previousSpeakerPhoto(@RestPath Long id) {
        PreviousSpeaker speaker = PreviousSpeaker.findById(id);
        notFoundIfNull(speaker);
        if (speaker.photo == null)
            seeOther("/public/images/mascotte/Ray_Cool.jpg");
        return binary(speaker.photo);
    }

    private Response binary(Blob photo) {
    	byte[] bytes;
		try {
			bytes = photo.getBytes(1, (int) photo.length());
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
    	return Response.ok(bytes, FileUtils.getMimeType(null, bytes)).build();
	}

    @Transactional
	public Response speakerPhoto(@RestPath Long id) {
        Speaker speaker = Speaker.findById(id);
        notFoundIfNull(speaker);
        if (speaker.photo == null)
            seeOther("/public/images/mascotte/Ray_Cool.jpg");
        return binary(speaker.photo);
    }

    @Transactional
    public Response sponsorLogo(@RestPath Long id) {
        Sponsor sponsor = Sponsor.findById(id);
        notFoundIfNull(sponsor);
        return binary(sponsor.logo);
    }

    @Transactional
    public Response orgaPhoto(@RestPath Long id) {
        Organiser organiser = Organiser.findById(id);
        notFoundIfNull(organiser);
        if (organiser.photo == null)
            seeOther("/public/images/mascotte/Ray_Badass.jpg");
        return binary(organiser.photo);
    }

    @Path("/team")
    public TemplateInstance organisers() {
        List<Organiser> orgas = Organiser.organisers();
        return Templates.organisers(orgas);
    }

    @Path("/team")
    public TemplateInstance organiser(@RestPath Long id) {
        Organiser orga = Organiser.findById(id);
        notFoundIfNull(orga);
        return Templates.organiser(orga);
    }

    @Path("/fishmarket")
    public TemplateInstance fishMarket(@RestPath String date) throws ParseException {
        Date day = new Date(System.currentTimeMillis());
        if (date != null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            day = format.parse(date);
        }
        List<Track> tracksForDay = Talk.findTracksPerDay(day);
        return Templates.fishMarket(tracksForDay, day);
    }

    @Path("/like-talk")
    @POST
    public Integer likeTalk(@RestPath Long id) {
        Talk talk = Talk.findById(id);
        if (talk != null) {
            talk.like();
        }
        if (getUser() != null) {
            // Return nb of likes only for connected user (i.e. admin)
            return talk.nbLikes;
        }
        return null;
    }

    @Path("/unlike-talk")
    @POST
    public Integer unlikeTalk(@RestPath Long id) {
        Talk talk = Talk.findById(id);
        if (talk != null) {
            talk.unlike();
        }
        if (getUser() != null) {
            // Return nb of likes only for connected user (i.e. admin)
            return talk.nbLikes;
        }
        return null;
    }

    @Path("/live")
    public TemplateInstance live() {
        List<Track> tracks = Track.listAll();
        Collections.sort(tracks);
        return Templates.live(tracks);
    }

    @Path("/live")
    public TemplateInstance liveTrack(@RestPath String track) {
        List<Track> tracks = Track.listAll();
        Collections.sort(tracks);
        List<Talk> keynotes = Talk.findKeynotes();
        return Templates.liveTrack(tracks, track, keynotes);
    }

    @Path("/schools")
    public TemplateInstance schools() {
        SponsorShip sponsorShip = SponsorShip.Schools;
        List<Sponsor> sponsors = Sponsor.list("level", sponsorShip);
        return Templates.schools(sponsorShip, sponsors);
    }
    
}

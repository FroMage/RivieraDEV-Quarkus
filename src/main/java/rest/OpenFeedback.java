package rest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.router.Router;
import jakarta.ws.rs.Path;
import model.BreakType;
import model.Speaker;
import model.Talk;

public class OpenFeedback extends Controller {

    public static class Root {
        public Map<String, OFSession> sessions = new HashMap<>();
        public Map<String, OFSpeaker> speakers = new HashMap<>();
    }

    public static class OFSession {
        public String title;
        public String id;
        public String startDate;
        public String endDate;
        public List<String> tags;
        public List<String> speakers;
        public String trackTitle;
        
    }

    public static class OFSpeaker {
        public String name;
        public String id;
        public List<Social> socials;
        public String photoUrl;
    }
    public static class Social {
        public String name;
        public String link;
    }

    @Path("/openfeedback")
    public Root openFeedback(){
        Root ret = new Root();

        List<Talk> talks = Talk.listAll();
        for(Talk talk : talks){
            OFSession session = new OFSession();

        if (talk.isBreak == BreakType.NotABreak){
            
            session.title = talk.getTitle();
            session.id = talk.id.toString();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
            session.startDate = formatter.format(talk.slot.startDate);
            session.endDate = formatter.format(talk.slot.endDate);
           
            if (talk.track != null) {
            session.trackTitle = talk.track.title;
            }

            session.speakers = new ArrayList<>();
            for (Speaker speaker : talk.speakers) {
                session.speakers.add(speaker.id.toString());
            }
            if (talk.theme != null) {
                session.tags = new ArrayList<>();
                session.tags.add(talk.theme.theme); 
            }
            
            ret.sessions.put(talk.id.toString(), session);
        }
    }
        List<Speaker> speakers = Speaker.listAll();
        for(Speaker speaker : speakers){
            OFSpeaker ofspeaker = new OFSpeaker();

            ofspeaker.name = speaker.firstName+" "+speaker.lastName;
            ofspeaker.id = speaker.id.toString();
            
            if (speaker.twitterAccount != null) {
                
                ofspeaker.socials = new ArrayList<>();
                Social social = new Social();
                social.name = "twitter";
                social.link = "https://twitter.com/" + speaker.twitterAccount;
                ofspeaker.socials.add(social);
            }
            String uri = Router.getAbsoluteURI(Application::speakerPhoto, speaker.id, null).toString();
                ofspeaker.photoUrl = uri;

            ret.speakers.put(speaker.id.toString(), ofspeaker);
        }

        return ret;
    }
}
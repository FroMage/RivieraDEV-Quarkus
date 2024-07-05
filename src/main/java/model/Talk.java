package model;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.validator.constraints.Length;

import io.quarkiverse.renarde.util.I18N;
import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;

@Entity
public class Talk extends PanacheEntity implements Comparable<Talk> {

	@ManyToOne
	public Slot slot;

	// At least one title must be filled
	public String titleEN;
	public String titleFR;
	
	// At least one description must be filled
	@JdbcTypeCode(Types.LONGVARCHAR)
	@Length(max = 10000)
	public String descriptionEN;

	@JdbcTypeCode(Types.LONGVARCHAR)
	@Length(max = 10000)
	public String descriptionFR; 
	
	@ManyToOne
	public Track track;
	
	@NotNull
	@Enumerated(EnumType.STRING)
	public BreakType isBreak;

	@NotNull
	@Enumerated(EnumType.STRING)
	public Language language;

	@Enumerated(EnumType.STRING)
	public Level level;

	@ManyToOne
	public TalkTheme theme;

	@ManyToOne
	public TalkType type;

	public String slidesUrl;

	public Integer nbLikes = 0;
	
	// the cfp app id, if imported
    public String importId;

	// Permet de cacher ce talk dans la page qui liste les talks.
	// (Ex: 'Keynote des Orga', 'Accueil', etc...)
	public boolean isHiddenInTalksPage;

    @JoinTable(
            name="talk_speaker",
            joinColumns=@JoinColumn(name="talk_id"),
            inverseJoinColumns=@JoinColumn(name="speakers_id")
        )
	@ManyToMany
	public List<Speaker> speakers = new ArrayList<Speaker>();
	
    public String getFeedbackUrl() {
    	return Configuration.getFeedbackUrl(id);
    }

	public String getTitle() {
		String displayedTitle = "";

        if (Arc.container().instance(I18N.class).get().get().equals("en")) { // English
			if (titleEN != null && titleEN.length() > 0) {
				displayedTitle = titleEN;
			} 
			else if (titleFR != null && titleFR.length() > 0) {
				displayedTitle = titleFR;
			}
		}
		else { // French
			if (titleFR != null && titleFR.length() > 0) {
				displayedTitle = titleFR;
			}
			else if (titleEN != null && titleEN.length() > 0) {
				displayedTitle = titleEN;
			} 
		}
		return displayedTitle;
	}

	public String getDescription() {
		String displayedDescription = "";

        if (Arc.container().instance(I18N.class).get().get().equals("en")) { // English
			if (descriptionEN != null && descriptionEN.length() > 0) {
				displayedDescription = descriptionEN;
			} 
			else if (descriptionFR != null && descriptionFR.length() > 0) {
				displayedDescription = descriptionFR;
			}
		}
		else { // French
			if (descriptionFR != null && descriptionFR.length() > 0) {
				displayedDescription = descriptionFR;
			}
			else if (descriptionEN != null && descriptionEN.length() > 0) {
				displayedDescription = descriptionEN;
			} 
		}
		return displayedDescription;
	}


	@Override
	public String toString() {
		return (slot != null ? slot : "no slot") 
		        + " "
		        + (track != null ? track : "All tracks")
				+ ": " + getTitle() 
				+ (speakers.size() > 0 ? " (" + speakers.stream().map(f -> f.toString()).collect(Collectors.joining(", ")) + ")" : "");
	}

	public static List<Track> findTracksPerDay(Date day) {
		return (List)list("SELECT DISTINCT talk.track FROM Talk talk LEFT JOIN talk.slot AS slot WHERE date_trunc('day', slot.startDate) = ?1", day);
	}

	public int compareTo(Talk other){
		return this.getTitle().compareTo(other.getTitle());
	}

	public static List<Talk> findKeynotes() {
		return Talk.list("track IS NULL AND isBreak = '" + BreakType.NotABreak + "' AND isHiddenInTalksPage = false");
	}

	public void like(){
		if(this.nbLikes == null){
			this.nbLikes = 0;
		}
		this.nbLikes++;
	}

	public void unlike(){
		if(this.nbLikes == null){
			this.nbLikes = 0;
		}
		else if(this.nbLikes > 0){
			this.nbLikes--;
		}
	}
}

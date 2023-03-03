package model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class TalkTheme extends PanacheEntity implements Comparable<TalkTheme>{
	
	@NotBlank
	public String theme;
	
	@NotNull
	@Enumerated(EnumType.STRING)
	public TalkThemeColor color;

	@OneToMany(mappedBy = "theme")
	public List<Talk> talks = new ArrayList<Talk>();
	
	// the cfp app id, if imported
	public String importId;
	
	@Override
	public String toString(){
		return theme;
	}

	@Override
	public int compareTo(TalkTheme other) {
		return theme.compareTo(other.theme);
	}

	public static List<TalkTheme> findUsedThemes() {
		return list("SELECT DISTINCT talk.theme FROM Talk talk");
	}

}
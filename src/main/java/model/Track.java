package model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Track extends PanacheEntity implements Comparable<Track>{
	@NotBlank
	public String title;
	
	@NotBlank
	public int position;

	public boolean isJUDCon;
	
	@OneToMany(mappedBy = "track")
	public List<Talk> talks = new ArrayList<Talk>();
	
	@Override
	public String toString(){
		return "[" + position + "] " + title;
	}

	@Override
	public int compareTo(Track other) {
		//return position.compareTo(other.position);
		return position - other.position;
	}
}

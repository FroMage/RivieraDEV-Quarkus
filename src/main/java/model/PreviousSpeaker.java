package model;

import java.sql.Blob;
import java.time.Instant;
import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class PreviousSpeaker extends PanacheEntity implements Comparable<PreviousSpeaker> {

	@NotBlank
	public String firstName;

	@NotBlank
	public String lastName;
	
	@NotBlank
	public String company;
	
	public Blob photo;
	public Date lastUpdated;

	/* La dernière année à laquelle l'orateur a participé au RivieraDEV */
	@NotNull
	public Integer year;
	
	@PreUpdate
	@PrePersist
	public void prePersist() {
		lastUpdated = Date.from(Instant.now());
	}
	
	@Override
	public String toString() {
		return firstName+" "+lastName;
	}

	@Override
	public int compareTo(PreviousSpeaker o) {
		return toString().compareTo(o.toString());
	}
}

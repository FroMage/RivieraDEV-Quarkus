package model;

import java.sql.Blob;

import jakarta.persistence.Entity;
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
	
	/* La dernière année à laquelle l'orateur a participé au RivieraDEV */
	@NotNull
	public Integer year;
	
	@Override
	public String toString() {
		return firstName+" "+lastName;
	}

	@Override
	public int compareTo(PreviousSpeaker o) {
		return toString().compareTo(o.toString());
	}
}

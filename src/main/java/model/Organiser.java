package model;

import java.sql.Blob;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import org.hibernate.annotations.Type;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Organiser extends PanacheEntity {
	public String firstName;
	@NotEmpty
	public String lastName;
	public String title;
	@Type(type="org.hibernate.type.TextType")
	@Lob
	@NotBlank
	@Length(max = 10000)
	public String biography;
	public String company;
	@URL
	public String companyURL;
	@URL
	public String blogURL;
	public String twitterAccount;
	
	public Blob photo;

	public boolean orga;
	public boolean cfp;
	
	@Override
	public String toString() {
		return firstName+" "+lastName;
	}

	public static List<Organiser> organisers() {
		return list("orga = true ORDER BY firstname, lastname");
	}

	public static List<Organiser> cfp() {
		return list("cfp = true ORDER BY firstname, lastname");
	}
}

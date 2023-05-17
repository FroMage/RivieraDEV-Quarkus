package model;

import java.sql.Blob;
import java.sql.Types;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@Entity
public class Organiser extends PanacheEntity {
	public String firstName;
	@NotEmpty
	public String lastName;
	public String title;
	@JdbcTypeCode(Types.LONGVARCHAR)
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

package model;

import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.NotBlank;

import org.hibernate.annotations.Type;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Speaker extends PanacheEntity implements Comparable<Speaker> {
	public String firstName;
	@NotBlank
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

	public String email;

	public Blob photo;
	
	// the cfp app id, if imported
    public String importId;

	/** Est-ce que ce speaker mérite d'être sur la page d'accueil ? */
	public boolean star;

	@ManyToMany(mappedBy = "speakers")
	public List<Talk> talks = new ArrayList<Talk>();
    
	public String phone;
	
	@Override
	public String toString() {
		return firstName+" "+lastName;
	}

	@Override
	public int compareTo(Speaker o) {
		return toString().compareTo(o.toString());
	}
}

package model;

import java.text.SimpleDateFormat;
import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class PricePackDate extends PanacheEntity {

	@NotBlank
    public Date blindBirdEndDate;

	@NotBlank
    public Date earlyBirdEndDate;

	@NotBlank
    public Date regularEndDate;

    @Override
    public String toString() {
      SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    
      StringBuffer strbuf = new StringBuffer();
		  strbuf.append(dateFormat.format(blindBirdEndDate))
		    .append(" - ")
		    .append(dateFormat.format(earlyBirdEndDate))
			  .append(" - ")
			  .append(dateFormat.format(regularEndDate));
      return strbuf.toString();
    }
}
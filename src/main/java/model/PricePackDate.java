package model;

import java.text.SimpleDateFormat;
import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class PricePackDate extends PanacheEntity {

	@NotNull
    public Date blindBirdEndDate;

	@NotNull
    public Date earlyBirdEndDate;

	@NotNull
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
package model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@SuppressWarnings("serial")
@Entity
public class TemporarySlot extends PanacheEntity {
	
	@Field("startDate")
	@NotBlank
	public Date startDate;
	
	@Field("endDate")
	@NotBlank
	public Date endDate;
	
	@Field("labelEN")
	public String labelEN;
	@Field("labelFR")
	public String labelFR;
	
	@Enumerated(EnumType.STRING)
	public BreakType isBreak;

	@Override
	public String toString() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
		
		StringBuffer strbuf = new StringBuffer();
		
		strbuf.append(dateFormat.format(startDate))
		      .append(" [")
		      .append(timeFormat.format(startDate))
			  .append(" - ")
			  .append(timeFormat.format(endDate))
			  .append("]");

		strbuf.append(" " + labelEN);
		
		if (isBreak != null) {
			strbuf.append(" (" + isBreak.getCode() + ")");
		}

		return strbuf.toString();
	}
	
	public static List<Slot> findPerDay(Date day){
		return list("date_trunc('day', startDate) = ?1 ORDER BY startDate", day);
	}
	
}
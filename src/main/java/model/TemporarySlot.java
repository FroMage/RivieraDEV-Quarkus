package model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.qute.TemplateData;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;

@TemplateData
@Entity
public class TemporarySlot extends PanacheEntity {
	
	@Field("startDate")
	@NotNull
	public Date startDate;
	
	@Field("endDate")
	@NotNull
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
	
	public static List<TemporarySlot> findPerDay(Date day){
		return list("date_trunc('day', startDate) = ?1 ORDER BY startDate", day);
	}
	
}
package rest;

import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.transporter.DatabaseTransporter;
import jakarta.transaction.Transactional;
import model.Configuration;
import model.Organiser;
import model.PreviousSpeaker;
import model.PricePack;
import model.PricePackDate;
import model.Slot;
import model.Speaker;
import model.Sponsor;
import model.Talk;
import model.TalkTheme;
import model.TalkType;
import model.TemporarySlot;
import model.Track;

public class Serialiser extends Controller {
	
	@Transactional
	public String json() {
		return DatabaseTransporter.export((entityType, fieldName, value) -> {
			if(entityType == Speaker.class) {
				// make sure we remove confidential info
				switch(fieldName) {
				case "email":
				case "phone":
					return null;
				}
			}
			return value;
		}, 
				// FIXME: this could be better, somehow
				Configuration.listAll(),
				Organiser.listAll(),
				PreviousSpeaker.listAll(),
				PricePack.listAll(),
				PricePackDate.listAll(),
				Slot.listAll(),
				Speaker.listAll(), 
				Sponsor.listAll(),
				Talk.listAll(),
				TalkTheme.listAll(),
				TalkType.listAll(),
				TemporarySlot.listAll(),
				Track.listAll());
	}
}

package model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.qute.TemplateData;

@TemplateData
@Entity
public class Slot extends PanacheEntity implements Comparable<Slot> {

    @Field("startDate")
	@NotNull
    public Date startDate;

    @Field("endDate")
	@NotNull
    public Date endDate;

    @OneToMany(mappedBy = "slot")
    public List<Talk> talks = new ArrayList<Talk>();

    @Override
    public String toString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        StringBuffer strbuf = new StringBuffer();

        strbuf.append(dateFormat.format(startDate)).append(" [").append(timeFormat.format(startDate)).append(" - ")
                .append(timeFormat.format(endDate)).append("]");
        /*
         * strbuf.append(" ("); boolean first = true; for(Talk talk : talks){ if(!first)
         * { strbuf.append(", "); } first = false; if(talk.track != null) {
         * strbuf.append(talk.track); } } strbuf.append(")");
         */
        return strbuf.toString();
    }

    public static List<Slot> findPerDay(Date day) {
        List<Slot> slots = list("date_trunc('day', startDate) = ?1 ORDER BY startDate, endDate", day);
        // En cas de slots déjà contenu dans 1 slots, ne renvoyer que le plus grand ?
        return slots;
    }

    /**
     * Permet de récupéer pour un jour, tous les slots avec des slots 'multi-slots'.
     * (Exemple : un slot de 50 min contient 2 slots de 25 min, seul le slot de 50
     * sera retourné)
     */
    public static List<Slot> findMultiPerDay(Date day) {
        List<Slot> slots = list("date_trunc('day', startDate) = ?1 ORDER BY startDate, endDate", day);
        Map<Date, Slot> multiSlots = new HashMap();
        NEXT: for (Slot slot : slots) {
            Slot existingSlot = multiSlots.get(slot.startDate);
            if (existingSlot != null) {
                // Compare each EndDate
                if (existingSlot.endDate.compareTo(slot.endDate) < 0) {
                    multiSlots.put(slot.startDate, slot);
                }
            } else {
                // if we find a new slot who starts after accepted slots (due to ORDER BY above)
                // and ends before or at
                // an accepted slot, it must be a smaller contained slot so we skip it
                for (Slot acceptedSlot : multiSlots.values()) {
                    if (slot.endDate.compareTo(acceptedSlot.endDate) <= 0)
                        continue NEXT;
                }
                multiSlots.put(slot.startDate, slot);
            }
        }
        List<Slot> result = new ArrayList(multiSlots.values());

        // Sorting
        Collections.sort(result);

        return result;
    }

    /**
     * Retourne le talk du Slot pour la track
     */
    public Talk getTalkPerTrack(Track track) {
        for (Talk talk : talks) {
            if (talk.track == track) {
                return talk;
            }
        }
        return null;
    }

    /**
     * Retourne tous les talks pour le Slot pour la track. (Exemple : 2 talks de 25
     * mins dans un slot de 50 mins)
     */
    public List<Talk> getTalksPerTrack(Track track) {
        // Récupérer tous les talks pour la track qui contient dans ce Slot et les slots
        // contenus
        List<Slot> slots = list("?1 <= startDate AND endDate <= ?2", this.startDate, this.endDate);

        List<Talk> talks = new ArrayList<Talk>();
        for (Slot slot : slots) {
            for (Talk talk : slot.talks) {
                if (talk.track == track) {
                    talks.add(talk);
                }
            }
        }
        // sort them in slot order
        talks.sort((t1, t2) -> t1.slot.compareTo(t2.slot));
        return talks;
    }

    /**
     * Retourne le talk s'il doit être sur toutes les tracks, null sinon
     */
    public Talk getAllTracksEvent() {
        if (talks.size() == 1) {
            // Il faut aussi vérifier s'il n'existe pas de slot plus petits (cas des
            // Snorkeling Session)
            // (Car un grand slot peut avoir un seul talk mais il peut y avoir en parallèle
            // des slots plus petits avec des talks)
            List<Slot> slots = list("?1 <= startDate AND endDate <= ?2", this.startDate, this.endDate);
            if (slots.size() > 1) {
                // Il y a au moins un autre slot en parralèle
                return null;
            }
            // Ok, ce talk peut être affiché sur toutes les tracks
            return talks.get(0);
        }
        return null;
    }

	@Override
	public int compareTo(Slot o) {
		int ret = startDate.compareTo(o.startDate);
		if(ret == 0) {
			ret = endDate.compareTo(o.endDate);
		}
		return ret;
	}
}
package model;

import java.util.ArrayList;
import java.util.List;

import io.quarkiverse.renarde.util.I18N;
import io.quarkus.arc.Arc;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;

@Entity
public class TalkType extends PanacheEntity implements Comparable<TalkType>{
	
	@NotBlank
	public String typeEN;

    public String typeFR;
    
	@OneToMany(mappedBy = "type")
	public List<Talk> talks = new ArrayList<Talk>();
	
	// the cfp app id, if imported
    public String importId;

	@Override
	public String toString(){
        if (typeFR == null || Arc.container().instance(I18N.class).get().get().equals("en")) { // English
            return typeEN;
        }
		return typeFR;
	}

	@Override
	public int compareTo(TalkType other) {
		return typeEN.compareTo(other.typeEN);
	}

	public static List<TalkType> findUsedTypes() {
		return list("SELECT DISTINCT talk.type FROM Talk talk");
	}

}
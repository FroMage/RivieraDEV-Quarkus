package util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkiverse.renarde.transporter.DatabaseTransporter;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.logging.Log;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
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

@ApplicationScoped
public class Startup {

	@ConfigProperty(name = "dev-auto-setup.url") 
	Optional<String> devAutoSetupUrl;

	/**
     * This method is executed at the start of your application
     */
	@Transactional
    public void start(@Observes StartupEvent evt) {
        // in DEV mode we seed some data
        if(LaunchMode.current() == LaunchMode.DEVELOPMENT && devAutoSetupUrl.isPresent()) {
        	String dataSource = devAutoSetupUrl.get();
        	Log.infof("Loading data from %s", dataSource);
        	try {
        		try(InputStream is = new URL(dataSource).openStream()){
        			String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        			// FIXME: this is not nice
        			Configuration.deleteAll();
        			Organiser.deleteAll();
        			PreviousSpeaker.deleteAll();
        			PricePack.deleteAll();
        			PricePackDate.deleteAll();
        			Slot.deleteAll();
        			Speaker.deleteAll(); 
        			Sponsor.deleteAll();
        			Talk.deleteAll();
        			TalkTheme.deleteAll();
        			TalkType.deleteAll();
        			TemporarySlot.deleteAll();
        			Track.deleteAll();
        			Map<Class<?>,List<? extends PanacheEntityBase>> entities = DatabaseTransporter.importEntities(json);
        			// Load the entities in the proper order: relation targets before relation owners
        			for (Class<? extends PanacheEntityBase> entityType : DatabaseTransporter.sortedEntityTypes()) {
        				List<? extends PanacheEntityBase> list = entities.get(entityType);
        				if(list != null) {
        					for (PanacheEntityBase entity : list) {
        						// FIXME: this is not nice
        						// remove the ID, to get a fresh entity
        						((PanacheEntity)entity).id = null;
        						entity.persist();
        					}
        				}
        			}
        		}
        	} catch (IOException e) {
        		throw new UncheckedIOException(e);
        	}
        }
    }
}

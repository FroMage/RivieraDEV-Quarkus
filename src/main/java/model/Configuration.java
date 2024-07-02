package model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Configuration extends PanacheEntity implements Comparable<Configuration> {

	@Enumerated(EnumType.STRING)
	public ConfigurationKey key;

	public String value;

	@Override
	public String toString() {
		return this.key + " : " + this.value;
	}
	
    /**
     * Retourne l'API KEY sauvée en BD. En local, si la clé n'est pas définie alors
     * la google map fonctionne quand même. MAIS en Prod/Staging, il FAUT une API
     * Key sinon la carte ne fonctionne pas c'est certainement une restriction
     * google.
     * 
     * L'API KEY de Prod ne peut pas être utilisée en local, car nous l'avons
     * restreinte pour ne fonctionner qu'avec les domaines *.rivieradev.fr et
     * *.rivieradev.com afin de suivre les recommandations de sécurité décrites par
     * Google.
     * 
     * Pour générer une nouvelle API KEY :
     * https://developers.google.com/maps/documentation/javascript/get-api-key?hl=Fr
     */
    public static String getGoogleMapApiKey() {
        Configuration config = Configuration.find("key", ConfigurationKey.GOOGLE_MAP_API_KEY).firstResult();
        if (config != null) {
            return config.value;
        }
        return null;
    }

    /**
     * Retourne la date de début de la conférence telle qu'elle est stockée en BD.
     * Elle devrait être au format ISO. Ex: 2019-05-15T08:20:00
     * 
     * @return la date de début de la conférence
     */
    public static String getEventStartDate() {
        Configuration config = Configuration.find("key", ConfigurationKey.EVENT_START_DATE).firstResult();
        if (config != null) {
            return config.value;
        }
        return null;
    }

    /**
     * Retourne la date de fin de la conférence telle qu'elle est stockée en BD.
     * Elle devrait être au format ISO. Ex: 2019-05-15T08:20:00
     * 
     * @return la date de fin de la conférence
     */
    public static String getEventEndDate() {
        Configuration config = Configuration.find("key", ConfigurationKey.EVENT_END_DATE).firstResult();
        if (config != null) {
            return config.value;
        }
        return null;
    }

    /**
     * Retourne true si le programme complet doit être affiché, faux sinon.
     */
    public static boolean displayFullSchedule() {
        Configuration config = Configuration.find("key", ConfigurationKey.DISPLAY_FULL_SCHEDULE).firstResult();
        return config != null && config.value.equals("true");
    }

    /**
     * Retourne true si les speakers de la nouvelle édition doivent être affichés
     * (utile avant que le programme définitif ne soit connu)
     */
    public static boolean displayNewSpeakers() {
        Configuration config = Configuration.find("key", ConfigurationKey.DISPLAY_NEW_SPEAKERS).firstResult();
        return config != null && config.value.equals("true");
    }

    /**
     * Retourne la page à mettre en avant sur la home page et dans le menu. 'CFP' :
     * La page du CFP 'TICKETS' : La page d'achat de tickets 'SPONSORS' : La page
     * pour devenir un sponsor
     */
    public static String getPromotedPage() {
        Configuration config = Configuration.find("key", ConfigurationKey.PROMOTED_PAGE).firstResult();
        return config != null ? config.value : null;
    }

    /**
     * Retourne la 2ème page à mettre en avant sur la home page. 'SPONSORS' : La
     * page pour devenir un sponsor 'SCHEDULE' : Le programme
     */
    public static String getPromotedPage2() {
        Configuration config = Configuration.find("key", ConfigurationKey.PROMOTED_PAGE_2).firstResult();
        return config != null ? config.value : null;
    }

    /**
     * Retourne l'Url de la page où on peut acheter les billets.
     */
    public static String getTicketingUrl() {
        Configuration config = Configuration.find("key", ConfigurationKey.TICKETING_URL).firstResult();
        return config != null ? config.value : null;
    }

    /**
     * Retourne true s'il est possible d'acheter des billets. (utile pour enlever
     * l'accès à la page de vente des billets)
     */
    public static boolean ticketingIsOpen() {
        Configuration config = Configuration.find("key", ConfigurationKey.TICKETING_OPEN).firstResult();
        return config != null && config.value.equals("true");
    }

    /**
     * Retourne l'Url de la page de l'organisme de formation.
     */
    public static String getTicketingTrainingUrl() {
        Configuration config = Configuration.find("key", ConfigurationKey.TICKETING_TRAINING_URL).firstResult();
        return config != null ? config.value : null;
    }

    /**
     * Retourne true s'il est possible d'accéder à la page de l'organisme de formation 
     * (utile en attendant que la page soit prête)
     */
    public static boolean ticketingTrainingIsOpen() {
        Configuration config = Configuration.find("key", ConfigurationKey.TICKETING_TRAINING_OPEN).firstResult();
        return config != null && config.value.equals("true");
    }

    /**
     * Retourne l'Url de la page de Openfeedback du talk.
     */
    public static String getFeedbackUrl(Long id) {
        Configuration config = Configuration.find("key", ConfigurationKey.FEEDBACK_URL).firstResult();
        return config != null ? config.value + "/" + id : null;
    }

    /**
     * Return true if Call For Paper is opened, false otherwise
     */
    public static boolean cfpIsOpened() {
        Configuration config = Configuration.find("key", ConfigurationKey.CFP_OPEN).firstResult();
        return config != null && config.value.equals("true");
    }

    /**
     * Return the Call for Paper URL
     */
    public static String getCfpUrl() {
        Configuration config = Configuration.find("key", ConfigurationKey.CFP_URL).firstResult();
        return config != null ? config.value : null;
    }

    /**
     * Retourne true si le menu doit permettre d'afficher la page des talks (utile
     * pour enlever l'accès à la page tant qu'on n'a pas encore de talks)
     */
    public static boolean displayTalks() {
        Configuration config = Configuration.find("key", ConfigurationKey.DISPLAY_TALKS).firstResult();
        return config != null && config.value.equals("true");
    }

    public static String getSponsoringLeafletUrl() {
        Configuration config = Configuration.find("key", ConfigurationKey.SPONSORING_LEAFLET_URL).firstResult();
        return config != null ? config.value : null;
    }

    /**
     * Covid19 newsletter URL
     */
    public static String getCancelledUrl() {
        Configuration config = Configuration.find("key", ConfigurationKey.CANCELLED_URL).firstResult();
        return config != null ? config.value : null;
    }

	@Override
	public int compareTo(Configuration o) {
		return key.compareTo(o.key);
	}
}

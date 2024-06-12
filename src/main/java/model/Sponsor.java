package model;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.logging.Log;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Sponsor extends PanacheEntity implements Comparable<Sponsor> {
	@NotBlank
	public String company;
	@JdbcTypeCode(Types.LONGVARCHAR)
	@NotBlank
	@Length(max = 10000)
	public String about;
	@JdbcTypeCode(Types.LONGVARCHAR)
	@NotBlank
	@Length(max = 10000)
	public String aboutEN;
	@URL
	public String companyURL;
	@URL
	public String otherURL;
	public String twitterAccount;
	public String linkedInAccount;

	@Enumerated(EnumType.STRING)
	public SponsorShip level;
	
	public Blob logo;
	
	// set on save/create
	public Integer width, height;
	public Date lastUpdated;

	@PreUpdate
	@PrePersist
	public void prePersist() {
		updateImageSizes();
		lastUpdated = Date.from(Instant.now());
	}
	
	private void updateImageSizes() {
		if(logo != null){
			try (InputStream is = logo.getBinaryStream()) {
				BufferedImage image = ImageIO.read(is);
				this.width = image.getWidth();
				this.height = image.getHeight();
			} catch (IOException e) {
				Log.error("Failed to read image", e);
			} catch (SQLException e) {
				Log.error("Failed to read image", e);
			}
		}
	}

	@Override
	public String toString() {
		return company;
	}
	
	@Override
	public int compareTo(Sponsor other) {
		int ret = level.compareTo(other.level);
		if(ret != 0)
			return ret;
		return company.compareTo(other.company);
	}
	
	public int getWidth(int surface, int maxSide){
		if(width == null)
			return 0;
		int biggerSide = Math.max(width, height);
		float sizeFactor = (float)maxSide / biggerSide;
		float area = width * height * sizeFactor * sizeFactor;
		float areaFactor = Math.min(1, (float)surface / area);
		return (int)Math.floor(width * sizeFactor * areaFactor);
	}

	public int getHeight(int surface, int maxSide){
		if(height == null)
			return 0;
		int biggerSide = Math.max(width, height);
		float sizeFactor = (float)maxSide / biggerSide;
		float area = width * height * sizeFactor * sizeFactor;
		float areaFactor = Math.min(1, (float)surface / area);
		return (int)Math.floor(height * sizeFactor * areaFactor);
	}

    public static class SponsorsToDisplay {
        private Map<SponsorShip, List<Sponsor>> sponsors;
        private List<Sponsor> sponsorsPreviousYears;

        public SponsorsToDisplay(Map<SponsorShip, List<Sponsor>> sponsors, List<Sponsor> sponsorsPreviousYears) {
            this.sponsors = sponsors;
            this.sponsorsPreviousYears = sponsorsPreviousYears;
        }

        public Map<SponsorShip, List<Sponsor>> getSponsors() {
            return this.sponsors;
        }

        public List<Sponsor> getSponsorsPreviousYears() {
            return this.sponsorsPreviousYears;
        }
    }
    
    public static SponsorsToDisplay getSponsorsToDisplay() {
        boolean mustDisplaySponsorsPreviousYears = true;

        Map<SponsorShip, List<Sponsor>> sponsors = new TreeMap<SponsorShip, List<Sponsor>>();
        for (SponsorShip sponsorShip : SponsorShip.values()) {
            if (sponsorShip != SponsorShip.PreviousYears) {
                List<Sponsor> sponsorsBySponsorShip = Sponsor.list("level", sponsorShip);
                if (sponsorsBySponsorShip != null && sponsorsBySponsorShip.size() > 0) {
                    mustDisplaySponsorsPreviousYears = false;
                    Collections.sort(sponsorsBySponsorShip);
                    sponsors.put(sponsorShip, sponsorsBySponsorShip);
                }
            }
        }

        List<Sponsor> sponsorsPreviousYears = null;
        if (mustDisplaySponsorsPreviousYears) {
            sponsorsPreviousYears = Sponsor.list("level", SponsorShip.PreviousYears);
            Collections.sort(sponsorsPreviousYears);
        }

        return new SponsorsToDisplay(sponsors, sponsorsPreviousYears);
    }

}

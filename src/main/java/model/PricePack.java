package model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class PricePack extends PanacheEntity implements Comparable<PricePack> {

	@NotBlank
    @Enumerated(EnumType.STRING)
    public PricePackType type;

	@NotBlank
    public Integer blindBirdPrice;

	@NotBlank
    public Integer earlyBirdPrice;

	@NotBlank
    public Integer regularPrice;

	@NotBlank
    public Integer studentPrice;
    
    public Boolean soldOut;

    @Override
    public String toString() {
      return type.toString() 
      + " (" 
      + studentPrice + "€ - " 
      + blindBirdPrice + "€ - " 
      + earlyBirdPrice + "€ - " 
      + regularPrice + "€" 
      + ")";
    }

    public int compareTo(PricePack other) {
      return type.getOrder() - other.type.getOrder();
    }
}
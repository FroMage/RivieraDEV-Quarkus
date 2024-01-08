package model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class PricePack extends PanacheEntity implements Comparable<PricePack> {

	@NotNull
    @Enumerated(EnumType.STRING)
    public PricePackType type;

	@NotNull
    public Integer blindBirdPrice;

	@NotNull
    public Integer earlyBirdPrice;

	@NotNull
    public Integer regularPrice;

	@NotNull
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
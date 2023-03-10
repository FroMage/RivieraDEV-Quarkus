package model;

public class PricePackCurrentState {
    
    public PricePackType type;

    public Integer currentPrice;

    public Integer maxPrice;

    public Integer studentPrice;

    public PricePackPeriod currentPeriod;

    public Long remainingDays;

    public Boolean soldOut;

    public PricePackCurrentState(PricePackType type, Integer currentPrice, Integer maxPrice, 
                                 Integer studentPrice, PricePackPeriod currentPeriod, Long remainingDays,
                                 Boolean soldOut) {
        this.type = type;
        this.currentPrice = currentPrice;
        this.maxPrice = maxPrice;
        this.studentPrice = studentPrice;
        this.currentPeriod = currentPeriod;
        this.remainingDays = remainingDays;
        this.soldOut = soldOut;
    }

}
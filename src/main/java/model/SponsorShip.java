package model;

public enum SponsorShip {
	Diamond			("diamond"),
	Platinum		("platinum"),
	Gold			("gold"), 
	Silver			("silver"), 
	Lunches			("lunches"), 
	Party			("party"),
	Basic			("basic"),
	Partner			("partner"),
	PreviousYears	("previousYears"),
	Schools         ("schools");

	private final String code;

	SponsorShip(String code) {
		this.code = code;
	}

	public String getCode() { return this.code; }
}

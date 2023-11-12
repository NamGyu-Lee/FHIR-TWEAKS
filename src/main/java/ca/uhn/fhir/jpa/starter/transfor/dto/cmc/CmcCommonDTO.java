package ca.uhn.fhir.jpa.starter.transfor.dto.cmc;

public abstract class CmcCommonDTO {
	public String type;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}

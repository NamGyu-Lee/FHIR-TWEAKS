package ca.uhn.fhir.jpa.starter.transfor.operation.code;

import lombok.Getter;

@Getter
public enum ResponseStateCode {

	OK(200, "true"),
	FORBIDDEN(403, "false"),
	BAD_REQUEST(400, "false")
	;

	ResponseStateCode(int stateCode, String success){
		this.stateCode = stateCode;
		this.success = success;
	}

	private int stateCode;

	private String success;

}

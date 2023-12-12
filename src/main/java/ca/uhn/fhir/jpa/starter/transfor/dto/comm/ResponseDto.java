package ca.uhn.fhir.jpa.starter.transfor.dto.comm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

import java.io.Serializable;


@Data
@AllArgsConstructor
@Builder
public class ResponseDto<T> implements Serializable {
	private String success;

	@NonNull
	private int stateCode;

	private String errorReason = "-";

	T body;

}

package ca.uhn.fhir.jpa.starter.transfor.base.map;


import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReferenceParamNode {

	private String sourceStr;

	private String cacheTargetStr;

	private String fhirTargetStr;

	public String retReferenceNode(){
		return sourceStr +  " -> " + cacheTargetStr + " : " + fhirTargetStr;
	}

}

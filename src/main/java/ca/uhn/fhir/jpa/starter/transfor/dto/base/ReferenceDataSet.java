package ca.uhn.fhir.jpa.starter.transfor.dto.base;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.hl7.fhir.r4.model.Reference;

import java.util.Map;

/** 2023. 11. 13.
 *  FHIR 에서 데이터를 생성하는 과정에서 정의되는 Structure Def 에서 요구하는 Reference 를
 *  반환하는 기능을 위하여 데이터 타입을 정의한다.
 */
@Getter
@Setter
@Builder
public class ReferenceDataSet{

	// 1.1. Concept 1. Key 유형
	@NonNull
	private String identifierId;

	// 1.2. Concept 2. Set
	private Map<String, String> identifierSet;

	// 2. Reference
	private Map<String, Reference> referenceList;

}

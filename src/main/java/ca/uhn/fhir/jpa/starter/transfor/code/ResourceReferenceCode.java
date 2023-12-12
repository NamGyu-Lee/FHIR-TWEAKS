package ca.uhn.fhir.jpa.starter.transfor.code;

/**
 * 2023. 12.
 * 해당 리소스가 FHIR의 reference 관계에서 어떤 역할을 수행하는지 정의한다.
 *
 * 1. 레퍼런스 정의측
 * Organization - 가장 최상위 객체
 * Basement - Organization 바로 하위 개체
 * Header - 진료 시점에 발생하는 다양한 데이터들의 피레퍼런스
 *
 * 2. 레퍼런스 활용측
 * PatientExtendData - Patient의 속성. Patient 만 존재하면 되는 데이터
 * Others - Encounter 를 활용하는 다양한 리소스들
 */
public enum ResourceReferenceCode {

	ORGANIZATION("Organization","Organization"),
	PATIENT("Patient","Basement"),
	PRACTITIONERROLE("PractitionerRole","Basement"),
	PRACTITIONER("Practitioner","Basement"),
	ENCOUNTER("Encounter","Header"),
	ALLERGYINTOLERANCE("AllergyIntolerance", "PatientExtendData"),
	PROCEDURE("Procedure", "PatientExtendData"),
	OTHERS("-", "Others")
	;

	ResourceReferenceCode(String resourceName, String baseType){
		this.resourceName = resourceName;
		this.baseType = baseType;
	}

	private String resourceName;

	private String baseType;

	public String getResourceName() {
		return resourceName;
	}

	public String getBaseType() {
		return baseType;
	}

	public static ResourceReferenceCode searchResourceReferenceCodeWithResourceName(String resourceName){
		for(ResourceReferenceCode code : ResourceReferenceCode.values()){
			if(code.resourceName.equals(resourceName)){
				return code;
			}
		}

		return ResourceReferenceCode.OTHERS;
	}

	public static ResourceReferenceCode searchResourceReferenceCodeWithContainResName(String resourceContainName){
		for(ResourceReferenceCode code : ResourceReferenceCode.values()){
			if(resourceContainName.contains(code.getResourceName())){
				return code;
			}
		}

		return ResourceReferenceCode.OTHERS;
	}

}

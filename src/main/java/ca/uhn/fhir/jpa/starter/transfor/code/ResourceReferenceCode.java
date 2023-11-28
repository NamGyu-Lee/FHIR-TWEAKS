package ca.uhn.fhir.jpa.starter.transfor.code;

public enum ResourceReferenceCode {

	PATIENT("Patient","Basement"),
	PRACTITIONER("Practitioner","Basement"),
	PRACTITIONERROLE("PractitionerRole","Basement"),
	ENCOUNTER("Encounter","Header"),
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

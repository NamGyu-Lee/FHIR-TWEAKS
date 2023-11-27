package ca.uhn.fhir.jpa.starter.transfor.code;

public enum ResourceNameSummaryCode {

	PATIENT("Patient", "PAT"),
	PRACTITIONER("Practitioner", "PRAT"),
	PRACTITIONERROLE("PractitionerRole", "PROL"),
	ENCOUNTER("PractitionerRole", "PROL"),
	CONDITION("PractitionerRole", "PROL"),
	MEDICATION("PractitionerRole", "MED"),
	MEDICATIONREQUEST("PractitionerRole", "MEDR"),
	DIAGNOSTICREPORT("DiagnosticReport", "DR"),
	OBSERVATION("Observation", "OB"),
	PROCEDURE("Procedure", "PD"),
	ALLERGY("AllergyIntolerance", "ALG"),
	DEVICE("Device", "DV"),
	ORGANIZATION("PractitionerRole", "OG"),
	IMMUNIZATION("Immunization", "IMU"),
	IMAGINGSTUDY("ImagingStudy", "IMGS"),
	;

	private String fullName;

	private String summaryName;

	ResourceNameSummaryCode(String fullName, String summaryName){
		this.fullName = fullName;
		this.summaryName = summaryName;
	}

	public static ResourceNameSummaryCode findSummaryName(String fullName){
		for(ResourceNameSummaryCode each : ResourceNameSummaryCode.values()){
			if(each.fullName.equals(fullName)){
				return each;
			}
		}

		return null;
	}

	public static boolean isCanbeSummaryName(String fullName){
		for(ResourceNameSummaryCode each : ResourceNameSummaryCode.values()){
			if(each.fullName.equals(fullName)){
				return true;
			}
		}
		return false;
	}

	public String getFullName() {
		return fullName;
	}

	public String getSummaryName() {
		return summaryName;
	}
}

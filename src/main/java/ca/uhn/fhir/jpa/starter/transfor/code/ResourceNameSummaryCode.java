package ca.uhn.fhir.jpa.starter.transfor.code;

public enum ResourceNameSummaryCode {

	ORGANIZATION("Organization", ""),
	PATIENT("Patient", ""),
	PRACTITIONER("Practitioner", "PRAT"),
	PRACTITIONERROLE("PractitionerRole", "PROL"),
	ENCOUNTER("Encounter", "ENC"),
	CONDITION("Condition", "PROL"),
	MEDICATION("Medication", "MED"),
	MEDICATIONREQUEST("MedicationRequest", "MEDR"),
	DIAGNOSTICREPORT("DiagnosticReport", "DR"),
	OBSERVATION("Observation", "OBV"),
	PROCEDURE("Procedure", "PD"),
	ALLERGY("AllergyIntolerance", "ALG"),
	DEVICE("Device", "DV"),
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

package ca.uhn.fhir.jpa.starter.transfor.service.cmc;

import org.hl7.fhir.r4.model.*;

import java.util.Map;

/** 2023. 11. 13.
 * The interface Cmc data transfor service.
 *  StructureMap 기반 활용이 시간소요가 커서 우선적으로 하드코드하게 맵핑하는것을 구현한다.
 */
public interface CmcDataTransforService {

	/**
	 * Transform plat data to fhir organization.
	 *
	 * @param requestMap the request map
	 * @return the organization
	 */
	public Organization transformPlatDataToFhirOrganization(Map<String, String> requestMap);

	/**
	 * Transform plat data to fhir patient.
	 *
	 * @param requestMap the request map
	 * @return the patient
	 */
	public Patient transformPlatDataToFhirPatient(String organizationId, Map<String, String> requestMap);

	/**
	 * Transform plat data to fhir practitioner practitioner.
	 *
	 * @param requestMap the request map
	 * @return the practitioner
	 */
	public Practitioner transformPlatDataToFhirPractitioner(String organizationId, Map<String, String> requestMap);

	/**
	 * Transform plat data to fhir practitioner role practitioner role.
	 *
	 * @param requestMap the request map
	 * @return the practitioner role
	 */
	public PractitionerRole transformPlatDataToFhirPractitionerRole(String organizationId, String practitionerId, Map<String, String> requestMap);

	/**
	 * Transform plat data to fhir encounter encounter.
	 *
	 * @param requestMap the request map
	 * @return the encounter
	 */
	public Encounter transformPlatDataToFhirEncounter(String organizationId, String practitionerRoleId, String patientId, Map<String, String> requestMap);

	/**
	 * Transform palt data to fhir condition condition.
	 *
	 * @param patientId   the patient id
	 * @param encounterId the encounter id
	 * @param requestMap  the request map
	 * @return the condition
	 */
	public Condition transformPaltDataToFhirCondition(String organizationId, String patientId, String encounterId, Map<String, String> requestMap);

	/**
	 * Transform plat data to fhir medication request medication request.
	 *
	 * @param patientId          the patient id
	 * @param practitionerRoleId the practitioner role id
	 * @param encounterId        the encounter id
	 * @param requestMap         the request map
	 * @return the medication request
	 */
	public MedicationRequest transformPlatDataToFhirMedicationRequest(String organizationId, String patientId, String practitionerRoleId, String encounterId, Map<String, String> requestMap);

	/**
	 * Transform palt data to fhir medication medication.
	 *
	 * @param requestMap the request map
	 * @return the medication
	 */
	public Medication transformPlatDataToFhirMedication(Map<String, String> requestMap);

	/**
	 * Transform plat data to fhir observation observation.
	 * KR Core의 기능검사 측정 정보에 적용되는 Observation 구조. 기능검사는 진단검사, 영상검사, 병리검사를 제외한 검사를 의미함
	 * @param requestMap the request map
	 * @return the observation
	 */
	public Observation transformPlatDataToFhirObservationExam(String organizationId, String patientId, String encounterId, Map<String, String> requestMap);

	public Procedure transformPlatDataToFhirProcedure(String organizationId, String patientId, String encounterId, Map<String, String> requestMap);

	/**
	 * Transform plat data to fhir service request service request.
	 *
	 * @param requestMap the request map
	 * @return the service request
	 */
	public ServiceRequest transformPlatDataToFhirServiceRequest(String organizationId, String patientId, String practitionerRoleId, String encounterId,  Map<String, String> requestMap);



}

package ca.uhn.fhir.jpa.starter.transfor.service.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import org.h2.util.json.JSONObject;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.codesystems.DoseRateType;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/** 2024. 03. 29.
 * The type Cmc transfor service.
 *
 * DTx 기반 데이터 변환모듈.
 */
public class CmcTransforServiceImpl implements CmcTransforService{

	private static final SimpleDateFormat SDF_YMD = new SimpleDateFormat("yyyyMMdd");
	private static final SimpleDateFormat SDF_Y_M_D = new SimpleDateFormat("yyyy-MM-dd");

	@Override
	public Organization transformPlatDataToFhirOrganization(Map<String, String> requestMap) {
		Organization organization = new Organization();

		// profile
		Meta meta = new Meta();
		meta.setProfile(createProfiles("http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-healthcare-organization", "http://connectdtx.kr/fhir/StructureDefinition/connectdtx-organization"));
		organization.setMeta(meta);

		// id
		List<String> prcpCdSplict = this.splitByDot(requestMap.get("proc_corp_cd"));
		organization.setId(prcpCdSplict.get(prcpCdSplict.size()-1) + "." + requestMap.get("inst_cd"));

		// identifier
		Map<String, String> reqIdentifierKrCore = new HashMap<>();
		reqIdentifierKrCore.put("system", "http://www.hl7korea.or.kr/Identifier/hira-krnpi");
		reqIdentifierKrCore.put("value", requestMap.get("proc_corp_cd"));

		Map<String, String> reqIdentifierDtx = new HashMap<>();
		reqIdentifierDtx.put("system", "urn:ietf:rfc:3986");
		reqIdentifierDtx.put("value", requestMap.get("proc_corp_cd"));
		organization.setIdentifier(createIdentifier(null, reqIdentifierKrCore, reqIdentifierDtx));

		// type
		Map<String, String> codingMap = new HashMap<>();
		codingMap.put("system", "http://www.hl7korea.or.kr/CodeSystem/hira-healthcare-organization-types");
		codingMap.put("code", requestMap.get("hosp_flag"));
		List<Coding> codingList = createCoding(codingMap);

		CodeableConcept cc = new CodeableConcept();
		cc.setCoding(codingList);

		List<org.hl7.fhir.r4.model.CodeableConcept> concepts = new ArrayList<>();
		concepts.add(cc);
		organization.setType(concepts);

		// active
		organization.setActive(true);

		// name
		organization.setName(requestMap.get("hosp_nm"));

		// telecom
		ContactPoint contactPoint = new ContactPoint();
		contactPoint.setSystem(ContactPoint.ContactPointSystem.PHONE);
		contactPoint.setValue(requestMap.get("telno"));
		List<org.hl7.fhir.r4.model.ContactPoint> contactPoints = new ArrayList<>();
		organization.setTelecom(contactPoints);

		// address
		Address address = new Address();
		address.setText(requestMap.get("hosp_addr"));
		address.setPostalCode(requestMap.get("zipcd"));

		List<Address> addressList = new ArrayList<>();
		addressList.add(address);
		organization.setAddress(addressList);

		return organization;
	}

	@Override
	public Patient transformPlatDataToFhirPatient(String organizationId, Map<String, String> requestMap) {
		Patient patient = new Patient();

		// profile
		Meta meta = new Meta();
		meta.setProfile(createProfiles("http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-patient", "http://connectdtx.kr/fhir/StructureDefinition/connectdtx-patient"));
		patient.setMeta(meta);

		String uniqueIdent = requestMap.get("proc_corp_cd") + "." + requestMap.get("pid");

		// id
		patient.setId(uniqueIdent);

		// active
		patient.setActive(true);

		// identifier
		// coding
		Map<String, String> reqCodeIdentifier = new HashMap<>();
		reqCodeIdentifier.put("system", "http://terminology.hl7.org/CodeSystem/v2-0203");
		reqCodeIdentifier.put("code", "MR");

		// data
		Map<String, String> reqDataIdentifier = new HashMap<>();
		reqDataIdentifier.put("system", "http://www.hl7korea.or.kr/Identifier/hira-krnpi");
		reqDataIdentifier.put("value", uniqueIdent);
		patient.setIdentifier(createIdentifier(reqCodeIdentifier, reqDataIdentifier));

		// name
		HumanName hn = new HumanName();
		hn.setText(requestMap.get("hng_nm"));
		List<HumanName> humanNames = new ArrayList<>();
		humanNames.add(hn);
		patient.setName(humanNames);

		// telecom
		ContactPoint contactPoint = new ContactPoint();
		contactPoint.setSystem(ContactPoint.ContactPointSystem.PHONE);
		contactPoint.setValue(requestMap.get("prtb_telno"));
		List<org.hl7.fhir.r4.model.ContactPoint> contactPointList = new ArrayList<>();
		contactPointList.add(contactPoint);
		patient.setTelecom(contactPointList);

		// gender
		String genderCd = requestMap.get("sex_cd");
		if("M".equals(genderCd)){
			patient.setGender(Enumerations.AdministrativeGender.MALE);
		}else if("F".equals(genderCd)){
			patient.setGender(Enumerations.AdministrativeGender.FEMALE);
		}else{
			patient.setGender(Enumerations.AdministrativeGender.UNKNOWN);
		}

		// birthdate
		try {
			patient.setBirthDate(SDF_YMD.parse(requestMap.get("brth_dd")));
		}catch(java.text.ParseException e){
			throw new IllegalArgumentException(" Patient 생성 과정중 오류가 발생하였습니다. 생년월일은 YYYYMMDD 형식이어야 합니다.");
		}

		// managingOrganization
		Reference ref = new Reference();
		ref.setReference("Organization/" + organizationId);
		patient.setManagingOrganization(ref);

		return patient;
	}

	@Override
	public Practitioner transformPlatDataToFhirPractitioner(String organizationId, Map<String, String> requestMap) {
		Practitioner practitioner = new Practitioner();

		// id
		practitioner.setId("PRCT." + organizationId + "." + requestMap.get("ord_dr_id"));

		// identifier
		Map<String, String> reqIdentifier = new HashMap<>();
		reqIdentifier.put("system", "http://www.hl7korea.or.kr/Identifier/local-practitioner-identifier");
		reqIdentifier.put("value", requestMap.get("ord_dr_id"));
		practitioner.setIdentifier(createIdentifier(null, reqIdentifier));

		// profile
		Meta meta = new Meta();
		meta.setProfile(createProfiles("http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-medical-doctor"));
		practitioner.setMeta(meta);

		// active
		practitioner.setActive(true);

		// name
		HumanName humanName = new HumanName();
		humanName.setText(requestMap.get("ord_dr_nm"));

		List<HumanName> humanNameList = new ArrayList<>();
		humanNameList.add(humanName);
		practitioner.setName(humanNameList);

		// qualification
		// 1. identifier
		Identifier identifier = new Identifier();
		identifier.setSystem("http://www.hl7korea.or.kr/Identifier/mohw-md-license-id");
		// TODO 의사 라이센스 코드
		identifier.setValue("temp");
		Practitioner.PractitionerQualificationComponent qualificationComponent = new Practitioner.PractitionerQualificationComponent();
		qualificationComponent.addIdentifier(identifier);
		List<Practitioner.PractitionerQualificationComponent> qualificationComponentList = new ArrayList<>();

		qualificationComponentList.add(qualificationComponent);

		// 2. code
		Coding coding = new Coding();
		coding.setSystem("http://www.hl7korea.or.kr/CodeSystem/mohw-practitioner-qualification-types");
		coding.setCode("의사");
		List<Coding> codingList = new ArrayList<>();
		codingList.add(coding);

		CodeableConcept concept = new CodeableConcept();
		concept.setCoding(codingList);
		qualificationComponent.setCode(concept);
		practitioner.setQualification(qualificationComponentList);

		return practitioner;
	}

	@Override
	public PractitionerRole transformPlatDataToFhirPractitionerRole(String organizationId, String practitionerId, Map<String, String> requestMap) {
		PractitionerRole practitionerRole = new PractitionerRole();

		// id
		practitionerRole.setId("PROL." + organizationId + "." + requestMap.get("ord_dr_id"));

		// identifier
		Map<String, String> reqIdentifier = new HashMap<>();
		reqIdentifier.put("system", "http://www.hl7korea.or.kr/Identifier/local-practitionerrole-identifier");
		//reqIdentifier.put("value", "ROLE" + "." + requestMap.get("ord_dr_id"));

		// 2023. 11. 14. ConnectDtx 에서 요구사항에 따라 해당 값은 SHA-256 해시 처리
		List<String> organizationIdSplit = this.splitByDot(organizationId);
		String organizationOid = organizationIdSplit.get(0);
		String hashTarget = organizationOid + "|" + requestMap.get("clam_dept_cd") + "|" +requestMap.get("ord_dr_id");
		String identifierValue = "";
		try {
			identifierValue = this.hash("SHA-256", hashTarget);
		}catch(java.security.NoSuchAlgorithmException e){

		}
		reqIdentifier.put("value", "ROLE" + "." + requestMap.get("ord_dr_id"));
		practitionerRole.setIdentifier(createIdentifier(null, reqIdentifier));

		// profile
		Meta meta = new Meta();

		// KR core , ConnectDtx 조합
		//meta.setProfile(createProfiles("http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-medical-doctor-role", "http://connectdtx.kr/fhir/StructureDefinition/connectdtx-practitionerrole"));

		// Connect DTx 기준
		meta.setProfile(createProfiles("http://connectdtx.kr/fhir/StructureDefinition/connectdtx-practitionerrole"));
		practitionerRole.setMeta(meta);

		// active
		practitionerRole.setActive(true);

		// code
		// kr core 기준
		Map<String, String> codeMap = new HashMap<>();
		codeMap.put("system", "http://terminology.hl7.org/CodeSystem/practitioner-role");
		codeMap.put("code", "doctor");
		List<Coding> codingList = createCoding(codeMap);
		CodeableConcept cc = new CodeableConcept();
		cc.setCoding(codingList);

		List<org.hl7.fhir.r4.model.CodeableConcept> concepts = new ArrayList<>();
		concepts.add(cc);
		practitionerRole.setCode(concepts);

		// speciality
		// 1. KR Core 기준
		/*
		Map<String, String> specMap = new HashMap<>();
		specMap.put("system", "http://www.hl7korea.or.kr/CodeSystem/hira-medical-department");
		specMap.put("code", requestMap.get("clam_dept_cd"));
		specMap.put("display", requestMap.get("clam_dept_nm"));
		List<Coding> specCodingList = createCoding(specMap);
		CodeableConcept sepcc = new CodeableConcept();
		sepcc.setCoding(specCodingList);
		*/

		// 2. ConnectDTx 구조
		Map<String, String> specMap = new HashMap<>();
		specMap.put("system", "https://hira.or.kr/CodeSystem/medical-subject");
		specMap.put("code", requestMap.get("clam_dept_cd"));
		specMap.put("display", requestMap.get("clam_dept_nm"));
		List<Coding> specCodingList = createCoding(specMap);
		CodeableConcept sepcc = new CodeableConcept();
		sepcc.setCoding(specCodingList);

		List<org.hl7.fhir.r4.model.CodeableConcept> specCodeConcept = new ArrayList<>();
		specCodeConcept.add(sepcc);
		practitionerRole.setSpecialty(specCodeConcept);

		// practitioner
		practitionerRole.setPractitioner(new Reference("Practitioner/" + practitionerId));

		// organization
		practitionerRole.setOrganization(new Reference("Organization/" +organizationId));

		return practitionerRole;
	}

	@Override
	public Encounter transformPlatDataToFhirEncounter(String organizationId, String practitionerRoleId, String patientId, Map<String, String> requestMap) {
		Encounter encounter = new Encounter();
		String uniqueId = "ENC."
			+ organizationId + "."
			+ requestMap.get("ord_dept_cd") + "."
			+ requestMap.get("ord_dd") + "."
			+ requestMap.get("cret_no") + "."
			+ requestMap.get("ord_dr_id") + "."
			+ requestMap.get("ord_type_cd")
			;

		// identifier
		Map<String, String> reqIdentifier = new HashMap<>();
		reqIdentifier.put("system", "http://www.hl7korea.or.kr/Identifier/local-encounter-identifier");
		reqIdentifier.put("value", uniqueId);
		encounter.setIdentifier(createIdentifier(null, reqIdentifier));

		// id
		encounter.setId(uniqueId);

		// profile
		Meta meta = new Meta();
		meta.setProfile(createProfiles("http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-encounter", "http://connectdtx.kr/fhir/StructureDefinition/connectdtx-encounter"));
		encounter.setMeta(meta);

		// active
		encounter.setStatus(Encounter.EncounterStatus.FINISHED);

		// extends 청구유형 . Cardinality 0..1
		/*
		Extension entClam = new Extension();
		entClam.setUrl("http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-insuranceTypes");

		Coding clamCoding = new Coding();
		clamCoding.setSystem("http://www.hl7korea.or.kr/CodeSystem/hira-insurance-types");
		clamCoding.setCode("");
		clamCoding.setDisplay("");
		entClam.setValue(clamCoding);

		List<Extension> extensionList = new ArrayList<>();
		extensionList.add(entClam);
		entClam.setExtension(extensionList);

		encounter.addExtension(entClam);
		*/

		// class
		String IoFlagStr = requestMap.get("ord_type_cd");
		String exchangedIOFlagCode = "";
		if("O".equals(IoFlagStr)){
			exchangedIOFlagCode = "AMB";
		}else if("I".equals(IoFlagStr)){
			exchangedIOFlagCode = "IMP";
		}else if("E".equals(IoFlagStr)){
			exchangedIOFlagCode = "EMER";
		}else{
			throw new IllegalArgumentException("진료 내역 중 알 수없는 유형코드(외래 입원 등) 가 들어와 오류가 발생하였습니다.");
		}
		Map<String, String> encouterCodingMap = new HashMap<>();
		encouterCodingMap.put("system", "http://terminology.hl7.org/CodeSystem/v3-ActCode");
		encouterCodingMap.put("code", exchangedIOFlagCode);
		encounter.setClass_(createCoding(encouterCodingMap).get(0));

		// period
		Period period = new Period();
		Date periodStartDate;
		try {
			periodStartDate = SDF_YMD.parse(requestMap.get("ord_dd"));
		} catch (java.text.ParseException e) {
			throw new IllegalArgumentException("진료 시작일자 오류.");
		}
		DateTimeType periodStart = new DateTimeType();
		periodStart.setValue(periodStartDate, TemporalPrecisionEnum.DAY);
		period.setStartElement(periodStart);

		Date periodEndDate;
		try {
			periodEndDate = SDF_YMD.parse(requestMap.get("dsch_dd"));
		} catch (java.text.ParseException e) {
			throw new IllegalArgumentException("진료 시작일자 오류.");
		}
		DateTimeType periodEnd = new DateTimeType();
		periodEnd.setValue(periodEndDate, TemporalPrecisionEnum.DAY);
		period.setEndElement(periodEnd);

		encounter.setPeriod(period);

		// serviceProvider
		encounter.setServiceProvider(new Reference("Organization/" + organizationId));

		// subject
		encounter.setSubject(new Reference("Patient/" + patientId));

		// participate
		Encounter.EncounterParticipantComponent component = new Encounter.EncounterParticipantComponent();
		component.setIndividual(new Reference("PractitionerRole/" + practitionerRoleId));

		List<Encounter.EncounterParticipantComponent> participantComponents = new ArrayList<>();
		participantComponents.add(component);
		encounter.setParticipant(participantComponents);

		return encounter;
	}

	@Override
	public Condition transformPaltDataToFhirCondition(String organizationId, String patientId, String encounterId, Map<String, String> requestMap) {
		Condition condition = new Condition();

		String conditionIdentifier = "CD." + organizationId + "." + patientId + "." + encounterId + "." + requestMap.get("diag_no");

		// identifier
		Map<String, String> reqIdentifier = new HashMap<>();
		reqIdentifier.put("system", "http://www.hl7korea.or.kr/Identifier/local-condition-identifier");
		reqIdentifier.put("value", conditionIdentifier);
		condition.setIdentifier(createIdentifier(null, reqIdentifier));

		// id
		// condition.setId(conditionId);

		// profile
		Meta meta = new Meta();
		meta.setProfile(createProfiles("http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-condition-chief-complaint", "http://connectdtx.kr/fhir/StructureDefinition/connectdtx-condition"));
		condition.setMeta(meta);

		// category
		List<CodeableConcept> categoryConcepts = new ArrayList<>();
		CodeableConcept categoryConcept = new CodeableConcept();
		List<Coding> categoryCodingList = new ArrayList<>();
		Coding cod = new Coding();
		cod.setSystem("http://www.hl7korea.or.kr/fhir/krcore/CodeSystem/krcore-condition-category-types");
		cod.setCode("주호소");
		categoryCodingList.add(cod);
		categoryConcept.setCoding(categoryCodingList);
		categoryConcepts.add(categoryConcept);
		condition.setCategory(categoryConcepts);

		// coding
		// KCD 마지막 자릿수가 .으로 끝날때 소거
		String diagCd = requestMap.get("diag_cd");
		if(diagCd.substring(diagCd.length()-1).equals(".")){
			diagCd = diagCd.substring(0, diagCd.length()-1);
		}
		Map<String, String> code = new HashMap<>();
		code.put("system", "https://kostat.or.kr/CodeSystem/kcd-8");
		code.put("code", diagCd);
		code.put("display", requestMap.get("diag_eng_nm"));
		List<Coding> codingList = createCoding(code);
		CodeableConcept concept = new CodeableConcept();
		concept.setCoding(codingList);
		condition.setCode(concept);

		// subject
		condition.setSubject(new Reference("Patient/" + patientId));

		// encounter
		condition.setEncounter(new Reference("Encounter/" + encounterId));

		// recordedDate
		try{
			condition.setRecordedDate(SDF_YMD.parse(requestMap.get("diag_dd")));
		}catch(java.text.ParseException e){
			throw new IllegalArgumentException(" > Condition 생성 과정에서 생성 날짜타입이 달라 Parsing 이 제대로 처리되지 않았습니다.");
		}

		return condition;
	}

	@Override
	public MedicationRequest transformPlatDataToFhirMedicationRequest(String organizationId, String patientId, String practitionerRoleId, String encounterId, Map<String, String> requestMap){
		MedicationRequest medicationRequest = new MedicationRequest();

		String medIdentifier = "MR." + organizationId + "." + patientId + "." + requestMap.get("prcp_no");

		// identifier
		Map<String, String> reqIdentifier = new HashMap<>();
		reqIdentifier.put("system", "http://www.hl7korea.or.kr/Identifier/local-medicationRequest-identifier");
		reqIdentifier.put("value", medIdentifier);
		medicationRequest.setIdentifier(createIdentifier(null, reqIdentifier));

		// id
		medicationRequest.setId(medIdentifier);

		// profile
		Meta meta = new Meta();
		meta.setProfile(createProfiles("http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-medicationrequest", "http://connectdtx.kr/fhir/StructureDefinition/connectdtx-medicationrequest"));
		medicationRequest.setMeta(meta);

		// intent
		medicationRequest.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);

		// status
		medicationRequest.setStatus(MedicationRequest.MedicationRequestStatus.COMPLETED);

		// medication
		medicationRequest.setMedication(new Reference("Medication/" + "MD." + requestMap.get("edi_cd")));

		// subject
		medicationRequest.setSubject(new Reference("Patient/" + patientId));

		// encounter
		medicationRequest.setEncounter(new Reference("Encounter/" + encounterId));

		// authorized
		try {
			medicationRequest.setAuthoredOn(SDF_YMD.parse(requestMap.get("prcp_dd")));
		}catch(java.text.ParseException e){
			throw new IllegalArgumentException("MedicationRequest 데이터 생성 과정에서 날짜타입이 상이하여 오류가 발생하였습니다.");
		}

		// dosage
		List<org.hl7.fhir.r4.model.Dosage> theDosageInstruction = new ArrayList<>();
		org.hl7.fhir.r4.model.Dosage dosage = new org.hl7.fhir.r4.model.Dosage();

		Dosage.DosageDoseAndRateComponent comp = new Dosage.DosageDoseAndRateComponent();
		// 1) dose
		SimpleQuantity dosageQuantity = new SimpleQuantity();
		dosageQuantity.setValue(Double.parseDouble(requestMap.get("prcp_vol")));
		dosageQuantity.setUnit(requestMap.get("prcp_vol_unit_nm"));
		//dosageQuantity.setSystem("http://unitsofmeasure.org");
		//dosageQuantity.setCode("{tbl}");
		comp.setDose(dosageQuantity);
		dosage.addDoseAndRate(comp);

		// 2) timing
		Timing timing = new Timing();
		Timing.TimingRepeatComponent timingRepeatComponent = new  Timing.TimingRepeatComponent();
		timingRepeatComponent.setFrequency(Integer.parseInt(requestMap.get("freq_cnt")));
		timingRepeatComponent.setPeriod(Double.parseDouble(requestMap.get("prcp_days")));
		timingRepeatComponent.setPeriodUnit(Timing.UnitsOfTime.D);

		timing.setRepeat(timingRepeatComponent);
		dosage.setTiming(timing);

		// 3) route
		CodeableConcept concept = new CodeableConcept();
		concept.setText("unknown");
		dosage.setRoute(concept);

		// 4) text
		dosage.setText(requestMap.get("abbr_nm"));

		theDosageInstruction.add(dosage);
		medicationRequest.setDosageInstruction(theDosageInstruction);

		return medicationRequest;
	}

	@Override
	public Medication transformPlatDataToFhirMedication(Map<String, String> requestMap){
		Medication medication = new Medication();

		String medicationIdentifier = "MD."+requestMap.get("edi_cd");

		// id
		medication.setId(medicationIdentifier);

		// identifier
		Map<String, String> reqIdentifier = new HashMap<>();
		reqIdentifier.put("system", "http://www.hl7korea.or.kr/CodeSystem/hira-edi-medication");
		reqIdentifier.put("value", medicationIdentifier);
		reqIdentifier.put("display", requestMap.get("edi_nm"));
		medication.setIdentifier(createIdentifier(null, reqIdentifier));

		// profile
		Meta meta = new Meta();
		meta.setProfile(createProfiles("http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-medication"));
		medication.setMeta(meta);

		// status
		medication.setStatus(Medication.MedicationStatus.ACTIVE);

		// code
		CodeableConcept codeableConcept = new CodeableConcept();
		List<Coding> codingList = new ArrayList<>();
		Coding coding = new Coding();
		coding.setSystem("http://www.hl7korea.or.kr/CodeSystem/hira-edi-medication");
		coding.setCode(requestMap.get("edi_cd"));
		coding.setDisplay(requestMap.get("edi_nm"));
		codingList.add(coding);
		codeableConcept.setCoding(codingList);
		medication.setCode(codeableConcept);

		// ingredient
		List<Medication.MedicationIngredientComponent> medicationIngredientComponentList = new ArrayList<>();
		Medication.MedicationIngredientComponent medicationIngredientComponent = new Medication.MedicationIngredientComponent();
		CodeableConcept conceptIngredient = new CodeableConcept();
		List<Coding> codingIngredientList = new ArrayList<>();
		Coding codingIngredient = new Coding();
		codingIngredient.setSystem("http://www.whocc.no/atc");
		codingIngredient.setCode(requestMap.get("edi_cd"));
		codingIngredientList.add(codingIngredient);
		conceptIngredient.setCoding(codingIngredientList);
		medicationIngredientComponent.setItem(conceptIngredient);
		medicationIngredientComponentList.add(medicationIngredientComponent);
		medication.setIngredient(medicationIngredientComponentList);

		return medication;
	}


	@Override
	public Observation transformPlatDataToFhirObservation(String organizationId, String patientId, String encounterId, Map<String, String> requestMap){
		Observation observation = new Observation();

		String uniqueId = "OB." + organizationId + "." + patientId + requestMap.get("exec_prcp_uniq_no");

		// id
		observation.setId(uniqueId);

		// identifier
		Map<String, String> reqIdentifier = new HashMap<>();
		reqIdentifier.put("system", "http://www.hl7korea.or.kr/Identifier/local-observation-identifier");
		reqIdentifier.put("value", uniqueId);
		observation.setIdentifier(createIdentifier(null, reqIdentifier));

		// status
		observation.setStatus(Observation.ObservationStatus.FINAL);

		// profile
		Meta meta = new Meta();
		meta.setProfile(createProfiles("http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-observation-laboratory-result", "http://connectdtx.kr/fhir/StructureDefinition/connectdtx-observation"));
		observation.setMeta(meta);

		// category
		List<CodeableConcept> conceptList = new ArrayList<>();
		CodeableConcept concept = new CodeableConcept();
		List<Coding> codingList = new ArrayList<>();
		Coding coding = new Coding();
		coding.setSystem("http://terminology.hl7.org/CodeSystem/observation-category");
		coding.setCode("laboratory");
		codingList.add(coding);
		concept.setCoding(codingList);
		observation.setCategory(conceptList);

		// issue
		try {
			observation.setIssued(SDF_YMD.parse(requestMap.get("prcp_dd")));
		}catch (ParseException e){
			throw new IllegalArgumentException("Observation 생성 과정중 오류가 발생하였습니다. 실시일자(test_dt) 파싱 과정에서 오류가 발생하였습니다.");
		}

		// code
		CodeableConcept conceptForCoding = new CodeableConcept();
		List<Coding> codingListForCoding = new ArrayList<>();
		Coding codingForCoding = new Coding();
		codingForCoding.setSystem("https://hira.or.kr/CodeSystem/lab-result-code");
		codingForCoding.setCode(requestMap.get("edi_cd"));
		codingForCoding.setDisplay(requestMap.get("test_cls_nm"));
		codingListForCoding.add(codingForCoding);
		conceptForCoding.setCoding(codingListForCoding);
		observation.setCode(conceptForCoding);

		// value
		if("valueQuantity".equals(requestMap.get("chk_flag"))){
			Quantity quantity = new Quantity();
			quantity.setValue(Double.parseDouble(requestMap.get("rslt_val")));
			quantity.setUnit(requestMap.get("rslt_val_unit_cd"));
			observation.setValue(quantity);
		}else if("valueString".equals(requestMap.get(""))){
			StringType stringType = new StringType();
			stringType.setValue(requestMap.get("rslt_val"));
		}else{
			System.out.println("오류 발생 ... !");
			//throw new IllegalArgumentException("Observation 생성 과정중 오류가 발생하였습니다. 정상적인 결과값 유형이 아닙니다.");
		}

		try {
			// referenceRange
			if ("valueQuantity".equals(requestMap.get("chk_flag"))) {
				Observation.ObservationReferenceRangeComponent component = new Observation.ObservationReferenceRangeComponent();
				String rfvlLower = requestMap.get("rfvl_lwlm_val");
				String rfvlUpper = requestMap.get("rfvl_uplm_val");
				if (rfvlLower != null && rfvlUpper != null && !"null".equals(rfvlLower) && !"null".equals(rfvlUpper)) {
					Quantity quantityLow = new Quantity(Double.parseDouble(rfvlLower));
					quantityLow.setUnit(requestMap.get("rslt_val_unit_cd"));
					component.setLow(quantityLow);

					Quantity quantityUpper = new Quantity(Double.parseDouble(rfvlUpper));
					quantityUpper.setUnit(requestMap.get("rslt_val_unit_cd"));
					component.setLow(quantityUpper);
				} else if (rfvlLower == null && rfvlUpper != null && "null".equals(rfvlLower) && !"null".equals(rfvlUpper)) {
					Quantity quantityUpper = new Quantity(Double.parseDouble(rfvlUpper));
					quantityUpper.setUnit(requestMap.get("rslt_val_unit_cd"));
					component.setLow(quantityUpper);
				} else if (rfvlUpper == null && rfvlLower != null && !"null".equals(rfvlLower) && "null".equals(rfvlUpper)) {
					Quantity quantityLow = new Quantity(Double.parseDouble(rfvlLower));
					quantityLow.setUnit(requestMap.get("rslt_val_unit_cd"));
					component.setLow(quantityLow);
				}

				List<Observation.ObservationReferenceRangeComponent> componentList = new ArrayList<>();
				componentList.add(component);
				observation.setReferenceRange(componentList);
			}
		}catch(Exception e){
			System.out.println("오류 발생 ... !");
		}

		// subject
		observation.setSubject(new Reference("Patient/" + patientId));

		// encounter
		observation.setEncounter(new Reference("Encounter/" + encounterId));

		return observation;
	}

	@Override
	public Observation transformPlatDataToFhirObservationExam(String organizationId, String patientId, String encounterId, Map<String, String> requestMap){
		Observation observation = new Observation();

		String uniqueId = "OB.EX." + organizationId + "." + patientId + requestMap.get("exec_prcp_uniq_no");

		// id
		observation.setId(uniqueId);

		// identifier
		Map<String, String> reqIdentifier = new HashMap<>();
		reqIdentifier.put("system", "http://www.hl7korea.or.kr/Identifier/local-observation-exam-identifier");
		reqIdentifier.put("value", uniqueId);
		observation.setIdentifier(createIdentifier(null, reqIdentifier));

		// status
		observation.setStatus(Observation.ObservationStatus.FINAL);

		// profile
		Meta meta = new Meta();
		meta.setProfile(createProfiles("http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-observation-function-test", "http://connectdtx.kr/fhir/StructureDefinition/connectdtx-observation"));
		observation.setMeta(meta);

		// category
		List<CodeableConcept> codeableConceptList = new ArrayList<>();
		CodeableConcept concept = new CodeableConcept();
		List<Coding> categoryCodingList = new ArrayList<>();
		Coding coding = new Coding();
		coding.setSystem("http://terminology.hl7.org/CodeSystem/observation-category");
		coding.setCode("exam");
		concept.setCoding(categoryCodingList);
		codeableConceptList.add(concept);
		observation.setCategory(codeableConceptList);

		// code
		CodeableConcept codeableConcept = new CodeableConcept();
		List<Coding> codingList = new ArrayList<>();
		Coding codeForCoding = new Coding();
		codeForCoding.setSystem("http://www.hl7korea.or.kr/CodeSystem/hira-edi-procedure");
		codeForCoding.setCode(requestMap.get("edi_cd"));
		codeForCoding.setDisplay(requestMap.get("test_nm"));
		codeableConcept.setText(requestMap.get("test_nm"));
		observation.setCode(codeableConcept);

		// subject
		observation.setSubject(new Reference("Patient/" + patientId));

		// encounter
		observation.setEncounter(new Reference("Encounter/" + encounterId));

		// performer
		/*
		List<Reference> peformerRef = new ArrayList<>();
		peformerRef.add(new Reference("PractitionerRule/" + performerId));
		observation.setPerformer(peformerRef);
		*/

		// effective
		DateTimeType dt = new DateTimeType();
		try {
			dt.setValue(SDF_YMD.parse(requestMap.get("test_dt").substring(0, 8)));
		}catch(ParseException e){
			throw new IllegalArgumentException(" Observation Exam 생성 과정중 오류가 발생하였습니다. 실시일자(test_dt) 파싱 과정에서 오류가 발생하였습니다.");
		}
		observation.setEffective(dt);

		return observation;
	}

	@Override
	public DiagnosticReport transformPlatDataToFhirDiagnosticReportPathology(String organizationId, String patientId, String encounterId, Map<String, String> requestMap){
		DiagnosticReport diagnosticReport = new DiagnosticReport();

		String uniqueId = "DR.P." + organizationId + "." + requestMap.get("exec_prcp_uniq_no");

		// id
		diagnosticReport.setId(uniqueId);

		// identifier
		Map<String, String> reqIdentifier = new HashMap<>();
		reqIdentifier.put("system", "http://www.hl7korea.or.kr/Identifier/local-diagnosticReport-pat-identifier" );
		reqIdentifier.put("value", uniqueId);
		diagnosticReport.setIdentifier(createIdentifier(null, reqIdentifier));

		// profile
		Meta meta = new Meta();
		meta.setProfile(createProfiles("http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-diagnosticreport-pathology-results"));
		diagnosticReport.setMeta(meta);

		// state
		diagnosticReport.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);

		// code
		CodeableConcept concept = new CodeableConcept();
		List<Coding> codingList = new ArrayList<>();
		Coding coding = new Coding();
		coding.setSystem("http://www.hl7korea.or.kr/CodeSystem/hira-edi-procedure");
		coding.setCode(requestMap.get("edi_cd"));
		coding.setDisplay(requestMap.get("cd_text_nm"));
		codingList.add(coding);
		concept.setCoding(codingList);
		concept.setText(requestMap.get("cd_text_nm"));
		diagnosticReport.setCode(concept);

		// effective DateTime
		/*
		try {
			diagnosticReport.setEffective(new DateType(SDF_YMD.parse(requestMap.get("exec_dt"))));
		}catch(ParseException e){
			throw new IllegalArgumentException("DiagnosticReport 형성 과정에서 오류가 발생하였습니다. 생성일자가 YYYYMMDD 형식이 아닙니다.");
		}
		*/

		// conclusion
		diagnosticReport.setConclusion(requestMap.get("concl_val"));

		// subject
		diagnosticReport.setSubject(new Reference("Patient/" + patientId));

		// encounter
		diagnosticReport.setEncounter(new Reference("Encounter/" + encounterId));

		return diagnosticReport;
	}

	@Override
	public DiagnosticReport transformPlatDataToFhirDiagnosticReportRadiology(String organizationId, String patientId, String encounterId, Map<String, String> requestMap){
		DiagnosticReport diagnosticReport = new DiagnosticReport();
		String uniqueId = "DR.R." + organizationId + "." + requestMap.get("exec_prcp_uniq_no");

		// id
		diagnosticReport.setId(uniqueId);

		// identifier
		Map<String, String> reqIdentifier = new HashMap<>();
		reqIdentifier.put("system", "http://www.hl7korea.or.kr/Identifier/local-diagnosticReport-rdo-identifier" );
		reqIdentifier.put("value", uniqueId);
		diagnosticReport.setIdentifier(createIdentifier(null, reqIdentifier));

		// profile
		Meta meta = new Meta();
		meta.setProfile(createProfiles("http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-diagnosticreport-diagnostic-imaging"));
		diagnosticReport.setMeta(meta);

		// status
		diagnosticReport.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);

		// category
		List<CodeableConcept> categoryCodeList = new ArrayList<>();
		CodeableConcept categoryConcept = new CodeableConcept();
		List<Coding> categoryCodingList = new ArrayList<>();
		Coding categoryCoding = new Coding();
		categoryCoding.setSystem("http://terminology.hl7.org/CodeSystem/v2-0074");
		categoryCoding.setCode("IMG");
		categoryCodingList.add(categoryCoding);
		categoryConcept.setCoding(categoryCodingList);
		categoryCodeList.add(categoryConcept);
		diagnosticReport.setCategory(categoryCodeList);

		// code
		CodeableConcept concept = new CodeableConcept();
		List<Coding> codingList = new ArrayList<>();
		Coding coding = new Coding();
		coding.setSystem("http://www.hl7korea.or.kr/CodeSystem/hira-edi-procedure");
		coding.setCode(requestMap.get("edi_cd"));
		coding.setDisplay(requestMap.get("test_nm"));
		codingList.add(coding);
		concept.setCoding(codingList);
		concept.setText(requestMap.get("text_nm"));
		diagnosticReport.setCode(concept);

		// subject
		diagnosticReport.setSubject(new Reference("Patient/" + patientId));

		// encounter
		diagnosticReport.setEncounter(new Reference("Encounter/"+ encounterId));

		return diagnosticReport;
	}

	@Override
	public Procedure transformPlatDataToFhirProcedure(String organizationId, String patientId, String encounterId, Map<String, String> requestMap){
		Procedure procedure = new Procedure();

		String uniqueId = "PD." + organizationId + "." + requestMap.get("op_rsrv_no");

		// id
		procedure.setId(uniqueId);

		// identifier
		Map<String, String> reqIdentifier = new HashMap<>();
		reqIdentifier.put("system", "http://www.hl7korea.or.kr/Identifier/local-procedure-identifier");
		reqIdentifier.put("value", uniqueId);
		procedure.setIdentifier(createIdentifier(null, reqIdentifier));

		// profile
		Meta meta = new Meta();
		meta.setProfile(createProfiles("http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-procedure"));
		procedure.setMeta(meta);

		// status
		procedure.setStatus(Procedure.ProcedureStatus.COMPLETED);

		// code
		CodeableConcept codeableConcept = new CodeableConcept();
		List<Coding> codingList = new ArrayList<>();
		Coding coding = new Coding();
		coding.setSystem("http://www.hl7korea.or.kr/CodeSystem/hira-edi-procedure");
		coding.setCode(requestMap.get("icd9n_cd"));
		coding.setDisplay(requestMap.get("op_nm"));
		codingList.add(coding);
		codeableConcept.setCoding(codingList);
		codeableConcept.setText(requestMap.get("op_nm"));
		procedure.setCode(codeableConcept);

		// performedDateTime
		try {
			procedure.setPerformed(new DateTimeType(SDF_YMD.parse(requestMap.get("op_dd"))));
		}catch (ParseException e){
			throw new IllegalArgumentException(" Procedure 생성 과정중 오류가 발생하였습니다. 실시일자(op_dd) 파싱 과정에서 오류가 발생하였습니다.");
		}

		// subject
		procedure.setSubject(new Reference("Patient/" + patientId));

		// encounter
		procedure.setEncounter(new Reference("Encounter/" + encounterId));

		return procedure;
	}

	@Override
	public ServiceRequest transformPlatDataToFhirServiceRequest(String organizationId, String patientId, String practitionerRoleId, String encounterId,  Map<String, String> requestMap) {
		ServiceRequest serviceRequest = new ServiceRequest();

		String uniqueId = "SR" + "."
			+ organizationId + "."
			+ encounterId + "."
			+ requestMap.get("prcp_cd");

		// id
		// 고유키 활용
		serviceRequest.setId("SQ." + requestMap.get("svc_req_key"));

		// identifier
		Map<String, String> reqIdentifier = new HashMap<>();

		List<String> organizationIdSplit = splitByDot(organizationId);

		reqIdentifier.put("system", organizationIdSplit.get(0));
		reqIdentifier.put("value", uniqueId);
		serviceRequest.setIdentifier(createIdentifier(null, reqIdentifier));

		// profile
		Meta meta = new Meta();
		meta.setProfile(createProfiles("http://connectdtx.kr/fhir/StructureDefinition/connectdtx-servicerequest"));
		serviceRequest.setMeta(meta);

		// status
		serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);

		// donotPerform
		BooleanType bt = new BooleanType();
		bt.setValue(true);
		serviceRequest.setDoNotPerformElement(bt);

		// code
		Map<String, String> code = new HashMap<>();
		code.put("system", "https://hira.or.kr/CodeSystem/diagnostic-behavior");
		// TODO DTx 등의 유형코드
		code.put("code", "somnus");
		List<Coding> codingList = createCoding(code);
		CodeableConcept concept = new CodeableConcept();
		concept.setCoding(codingList);
		serviceRequest.setCode(concept);

		// intent
		serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);

		// subject
		serviceRequest.setSubject(new Reference("Patient/" + patientId));

		// encounter
		serviceRequest.setEncounter(new Reference("Encounter/" + encounterId));

		// requester
		serviceRequest.setRequester(new Reference("PractitionerRole/" + practitionerRoleId));

		// authored
		serviceRequest.setAuthoredOn(new Date());

		return serviceRequest;
	}

	private List<CanonicalType> createProfiles(String... urls) {
		List<CanonicalType> canonicalTypeList = new ArrayList<>();
		for (String url : urls) {
			CanonicalType canonicalType = new CanonicalType();
			canonicalType.setValueAsString(url);
			canonicalTypeList.add(canonicalType);
		}
		return canonicalTypeList;
	}

	private List<Coding> createCoding(Map<String, String>... reqIdentifiers){
		List<Coding> codingList = new ArrayList<>();

		for(Map<String, String> eachReq : reqIdentifiers){
			Coding coding = new Coding();
			coding.setSystem(eachReq.get("system"));
			coding.setCode(eachReq.get("code"));
			if(eachReq.get("display") != null){
				coding.setDisplay(eachReq.get("display"));
			}
			codingList.add(coding);
		}
		return codingList;
	}

	private List<Identifier> createIdentifier(Map<String, String> codingMap, Map<String, String>... reqIdentifiers){
		List<Identifier> identifierList = new ArrayList<>();

		for(Map<String, String> eachReq : reqIdentifiers){
			Identifier identifier = new Identifier();
			// 1
			if(codingMap != null){
				CodeableConcept concept = new CodeableConcept();

				List<Coding> codingList = new ArrayList<>();
				Coding coding = new Coding();
				coding.setSystem(codingMap.get("system"));
				coding.setCode(codingMap.get("code"));
				codingList.add(coding);
				concept.setCoding(codingList);
				identifier.setType(concept);
			}

			// 2
			identifier.setSystem(eachReq.get("system"));
			identifier.setValue(eachReq.get("value"));
			identifierList.add(identifier);
		}
		return identifierList;
	}

	// type.coding 값이 다양한 Identifier 를 구성하기 위하여 정의한다.
	private List<Identifier> createIdentifier(Map<String, Map<String, String>>... reqIdentifiers){
		List<Identifier> identifierList = new ArrayList<>();

		for(Map<String, Map<String, String>> eachIdentifier : reqIdentifiers){
			Identifier identifier = new Identifier();
			// 1
			if(eachIdentifier.get("code") != null){
				CodeableConcept concept = new CodeableConcept();

				List<Coding> codingList = new ArrayList<>();
				Coding coding = new Coding();
				coding.setSystem(eachIdentifier.get("type.coding").get("system"));
				coding.setCode(eachIdentifier.get("type.coding").get("code"));
				codingList.add(coding);
				concept.setCoding(codingList);
				identifier.setType(concept);
			}

			// 2
			identifier.setSystem(eachIdentifier.get("identifier").get("system"));
			identifier.setValue(eachIdentifier.get("identifier").get("value"));
			identifierList.add(identifier);
		}
		return identifierList;
	}

	public List<String> splitByDot(String input) {
		List<String> resultList = new ArrayList<>();
		// 점(.)을 기준으로 문자열을 나누고 배열로 반환
		String[] parts = input.split("\\.");

		// 배열의 각 요소를 리스트에 추가
		for (String part : parts) {
			resultList.add(part);
		}
		return resultList;
	}

	public static String hash(String algorithm, String data) throws NoSuchAlgorithmException {
		// MessageDigest 인스턴스 생성
		MessageDigest digest = MessageDigest.getInstance(algorithm);

		// 데이터를 바이트 배열로 변환 후 해시 계산
		byte[] encodedhash = digest.digest(data.getBytes());

		// 바이트 배열을 16진수 문자열로 변환
		StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
		for (int i = 0; i < encodedhash.length; i++) {
			String hex = Integer.toHexString(0xff & encodedhash[i]);
			if(hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}

		return hexString.toString();
	}

	public String retResourceToString(IBaseResource resource){
		FhirContext fn = new FhirContext(FhirVersionEnum.R4);
		return fn.newJsonParser().encodeResourceToString(resource);
	}
}

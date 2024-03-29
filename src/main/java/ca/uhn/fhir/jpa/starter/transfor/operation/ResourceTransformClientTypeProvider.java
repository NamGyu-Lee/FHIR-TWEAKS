package ca.uhn.fhir.jpa.starter.transfor.operation;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.provider.BaseJpaProvider;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.jpa.starter.transfor.config.TransformDataOperationConfigProperties;
import ca.uhn.fhir.jpa.starter.transfor.base.reference.structure.ReferenceDataMatcher;
import ca.uhn.fhir.jpa.starter.transfor.base.reference.structure.ReferenceDataSet;
import ca.uhn.fhir.jpa.starter.transfor.service.client.CmcTransforService;
import ca.uhn.fhir.jpa.starter.transfor.service.client.CmcTransforServiceImpl;
import ca.uhn.fhir.jpa.starter.transfor.util.TransformUtil;
import ca.uhn.fhir.jpa.starter.util.PerformanceChecker;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.api.SearchTotalModeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.StringParam;
import com.google.gson.*;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/** 2024. 03. 28. Client 방식의 데이터 변환 구성
 *  성능 비교 용도로써 활용
 * The type Resource transform client type provider.
 */
public class ResourceTransformClientTypeProvider extends BaseJpaProvider {
	private static final Logger ourLog = LoggerFactory.getLogger(ResourceTransformClientTypeProvider.class);

	private DaoRegistry myDaoRegistry;

	@Autowired
	void setMyDaoRegistry(DaoRegistry myDaoRegistry){
		this.myDaoRegistry = myDaoRegistry;
	}

	private TransformDataOperationConfigProperties transformDataOperationConfigProperties;

	@Autowired
	@Primary
	void setTransformDataOperationConfigProperties(TransformDataOperationConfigProperties transformDataOperationConfigProperties){
		this.transformDataOperationConfigProperties = transformDataOperationConfigProperties;
		transformUtil = new TransformUtil(transformDataOperationConfigProperties);
		timer = new PerformanceChecker(transformDataOperationConfigProperties.isDebugPerformanceTrackingTimeEach(), transformDataOperationConfigProperties.isDebugPerformancePrintOperationTimeStack());
	}

	private TransformUtil transformUtil;

	private FhirContext fn;

	public ResourceTransformClientTypeProvider(){
		fn = this.getContext();
		transformUtil = new TransformUtil(transformDataOperationConfigProperties);
		timer = new PerformanceChecker(transformDataOperationConfigProperties.isDebugPerformanceTrackingTimeEach(), transformDataOperationConfigProperties.isDebugPerformancePrintOperationTimeStack());
	}

	public ResourceTransformClientTypeProvider(FhirContext fn){
		this.fn = fn;
	}

	ReferenceDataMatcher referenceDataMatcher = new ReferenceDataMatcher();

	CmcTransforService cmcTransforService = new CmcTransforServiceImpl();

	PerformanceChecker timer;

	// 1. CMC 기반 데이터를 전달받아 Patient 를 생성한다.
	// theServletResponse 에 write으로 리턴하는 방식.
	// locahost:8080/fhir/$sample-custom-operation
	@Operation(
		name="$tranform-resource-basic",
		idempotent = false,
		manualRequest = true,
		manualResponse = true
	)
	public void transforResourceStandardService(HttpServletRequest theServletRequest, HttpServletResponse theResponse) throws IOException {
		String retMessage = "-";
		ourLog.info(" > Create CMC Standard Data Transfor initalized.. ");

		FhirContext fn = this.getContext();
		timer = new PerformanceChecker(transformDataOperationConfigProperties.isDebugPerformanceTrackingTimeEach(), transformDataOperationConfigProperties.isDebugPerformancePrintOperationTimeStack());
		try {
			byte[] bytes = IOUtils.toByteArray(theServletRequest.getInputStream());
			String bodyData = new String(bytes);

			JsonParser jsonParser = new JsonParser();
			JsonElement jsonElement = jsonParser.parse(bodyData);
			JsonObject jsonObject = jsonElement.getAsJsonObject();

			Queue<Map.Entry<String, JsonElement>> sortedQueue = transformUtil.sortingCreateResourceArgument(jsonObject);

			// 실질적인 변환 부분
			// 해당 영역부터 개별 대상자로 한정
			Map<String, String> noSearchArg = new HashMap<>();
			noSearchArg.put("Don't Search Main Volumns", "9999999");
			referenceDataMatcher.inputMappingData("Standard-Ref", noSearchArg, new HashMap<>());
			while(sortedQueue.size() != 0){
				Map.Entry<String, JsonElement> entry = sortedQueue.poll();

				this.createResource(entry);
			}

		}catch(Exception e){
			e.printStackTrace();
			retMessage = e.getMessage();
		}finally{
			timer.printAllTimeStack();
			timer.exportStackToExcel("C:\\Client-Test-Data" + this.getCurrentDateTime() + ".xlsx");

			theResponse.setContentType("text/plain");
			theResponse.getWriter().write(retMessage);
			theResponse.getWriter().close();
		}
	}

	public static String getCurrentDateTime() {
		SimpleDateFormat formatter = new SimpleDateFormat("yyMMddHHmmss");
		Date now = new Date();
		return formatter.format(now);
	}

	@Operation(
		name="$tranform-resource-detail",
		idempotent = false,
		manualRequest = true,
		manualResponse = true
	)
	public void transforResourceDetailService(HttpServletRequest theServletRequest, HttpServletResponse theResponse) throws IOException {
		String retMessage = "-";
		ourLog.info(" > Create CMC Standard Data Transfor initalized.. ");


		try {
			byte[] bytes = IOUtils.toByteArray(theServletRequest.getInputStream());
			String bodyData = new String(bytes);

			JsonParser jsonParser = new JsonParser();
			JsonElement jsonElement = jsonParser.parse(bodyData);
			JsonObject jsonObject = jsonElement.getAsJsonObject();

			Queue<Map.Entry<String, JsonElement>> sortedQueue = transformUtil.sortingCreateResourceArgument(jsonObject);

			// 실질적인 변환 부분
			// 해당 영역부터 개별 대상자로 한정
			Map<String, String> noSearchArg = new HashMap<>();
			noSearchArg.put("Don't Search Main Volumns", "9999999");
			referenceDataMatcher.inputMappingData("Standard-Ref", noSearchArg, new HashMap<>());
			while(sortedQueue.size() != 0){
				Map.Entry<String, JsonElement> entry = sortedQueue.poll();
				this.createResource(entry);
			}

		}catch(Exception e){
			e.printStackTrace();
			retMessage = e.getMessage();
		}finally{
			timer.printAllTimeStack();
			timer.exportStackToExcel("C:\\Client-Test-Data" + this.getCurrentDateTime() + ".xlsx");

			theResponse.setContentType("text/plain");
			theResponse.getWriter().write(retMessage);
			theResponse.getWriter().close();
		}
	}

	// 리소스를 생성한다.
	private void createResource(Map.Entry<String, JsonElement> entry){
		JsonElement elements = entry.getValue();
		JsonArray jsonArray = elements.getAsJsonArray();

		for(int eachRowCount = 0; jsonArray.size() > eachRowCount; eachRowCount++){
			JsonObject eachRowJsonObj = jsonArray.get(eachRowCount).getAsJsonObject();
			Map<String, String> rowMap = TransformUtil.convertJsonObjectToMap(eachRowJsonObj);
			if ("organization".equals(entry.getKey())) {
				timer.startTimer();
				ourLog.info("-------------------------- Organization");
				Organization organization = cmcTransforService.transformPlatDataToFhirOrganization(rowMap);

				referenceDataMatcher.setReference("Standard-Ref", "Organization", new Reference(organization.getId()));
				referenceDataMatcher.setReference("Standard-Ref", "Organization-oid", new Reference(organization.getIdentifier().get(0).getValue()));

				// 2. 생성요청
				//IFhirResourceDao resourceProviderForOrganization = myDaoRegistry.getResourceDao("Organization");
				//resourceProviderForOrganization.update(organization);

				timer.endTimer("Create Organization...");
				loggingInDebugMode("organ : " + fn.newJsonParser().encodeResourceToString(organization));
			} else if ("patient".equals(entry.getKey())) {
				timer.startTimer();
				ourLog.info("-------------------------- Patient");
				String organizationId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get("Organization").getReference();
				Patient patient = cmcTransforService.transformPlatDataToFhirPatient(organizationId, rowMap);
				referenceDataMatcher.setReference("Standard-Ref", "Patient" , new Reference(patient.getId()));

				// 2. 생성요청
				IFhirResourceDao resourceProviderForPatient = myDaoRegistry.getResourceDao("Patient");
				resourceProviderForPatient.update(patient);
				loggingInDebugMode("patient : " + fn.newJsonParser().encodeResourceToString(patient));
				timer.endTimer("Create Patient...");
			} else if ("practitioner".equals(entry.getKey())) {
				timer.startTimer();
				ourLog.info("-------------------------- practition Data");

				String organizationId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get("Organization").getReference();

				Practitioner practitioner = cmcTransforService.transformPlatDataToFhirPractitioner(organizationId, rowMap);

				referenceDataMatcher.setReference("Standard-Ref", practitioner.getId(), new Reference(practitioner.getId()));
				String practitionerId = practitioner.getId();

				// 2. 생성요청
				//IFhirResourceDao resourceProviderForPractitioner = myDaoRegistry.getResourceDao("Practitioner");
				//resourceProviderForPractitioner.update(practitioner);

				loggingInDebugMode("practitioner Data : " + fn.newJsonParser().encodeResourceToString(practitioner));
				timer.endTimer("Create Practition...");
			}else if("practitionerrole".equals(entry.getKey())) {
				ourLog.info("-------------------------- Practitioner Role");
				timer.startTimer();
				String organizationId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get("Organization").getReference();
				String practitionerId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get("PRCT." + organizationId + "." + rowMap.get("ord_dr_id")).getReference();

				PractitionerRole practitionerRole = cmcTransforService.transformPlatDataToFhirPractitionerRole(organizationId, practitionerId, rowMap);
				referenceDataMatcher.setReference("Standard-Ref", practitionerRole.getId(), new Reference(practitionerRole.getId()));

				// 2. 생성요청
				//IFhirResourceDao resourceProviderForPractitionerRole = myDaoRegistry.getResourceDao("PractitionerRole");
				//resourceProviderForPractitionerRole.update(practitionerRole);

				loggingInDebugMode("practitionRole Data : " + fn.newJsonParser().encodeResourceToString(practitionerRole));
				timer.endTimer("Create PractitionRole...");
			}else if ("encounter".equals(entry.getKey())) {
				ourLog.info("-------------------------- Encounter");
				timer.startTimer();

				String organizationId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get("Organization").getReference();
				String patientId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get("Patient").getReference();
				String practitionerRoleId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get("PROL." + organizationId + "." + rowMap.get("ord_dr_id")).getReference();
				String practitionerId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get("PRCT." + organizationId + "." + rowMap.get("ord_dr_id")).getReference();

				Encounter encounter = cmcTransforService.transformPlatDataToFhirEncounter(organizationId, practitionerRoleId, patientId, rowMap);

				//encounterId = encounter.getId();
				LinkedHashMap<String, String> identifierSet = new LinkedHashMap<>();
				identifierSet.put("inst_cd", rowMap.get("inst_cd"));
				identifierSet.put("pid", rowMap.get("pid"));
				identifierSet.put("ord_dd", rowMap.get("ord_dd"));
				identifierSet.put("ord_dept_cd", rowMap.get("ord_dept_cd"));
				identifierSet.put("ord_type_cd", rowMap.get("ord_type_cd"));
				identifierSet.put("ord_dr_id", rowMap.get("ord_dr_id"));
				identifierSet.put("cret_no", rowMap.get("cret_no"));

				Map<String, Reference> encounterIncludedRefSet = new HashMap<>();
				encounterIncludedRefSet.put("Organization", new Reference(organizationId));
				encounterIncludedRefSet.put("Patient", new Reference(patientId));
				encounterIncludedRefSet.put("Encounter", new Reference(encounter.getId()));
				encounterIncludedRefSet.put("Practitioner", new Reference(practitionerId));
				encounterIncludedRefSet.put("PractitionerRole", new Reference(practitionerRoleId));
				String id = referenceDataMatcher.inputMappingData(identifierSet, encounterIncludedRefSet);

				//IFhirResourceDao resourceProviderForEncounter = myDaoRegistry.getResourceDao("Encounter");
				//resourceProviderForEncounter.update(encounter);

				loggingInDebugMode("encounter Data : " + fn.newJsonParser().encodeResourceToString(encounter));
				timer.endTimer("Create Encounter...");
			} else if("condition".equals(entry.getKey())){
				ourLog.info("-------------------------- Condition");
				timer.startTimer();
				Map<String, String> identifierSet = new HashMap<>();
				identifierSet.put("inst_cd", rowMap.get("inst_cd"));
				identifierSet.put("pid", rowMap.get("pid"));
				identifierSet.put("ord_dd", rowMap.get("ord_dd"));
				identifierSet.put("ord_dept_cd", rowMap.get("ord_dept_cd"));
				identifierSet.put("ord_type_cd", rowMap.get("io_flag"));
				identifierSet.put("ord_dr_id", rowMap.get("ord_dr_id"));
				identifierSet.put("cret_no", rowMap.get("cret_no"));

				ReferenceDataSet ds = referenceDataMatcher.searchMapperWithMapType(identifierSet);
				// 2024. 03. 28. 테스트를 위해서 무시시키기
				/*
				if (ds == null) {
					if (transformDataOperationConfigProperties.isTransforIgnoreHasNoEncounter()) {
						loggingInDebugMode(" > 해당 리소스의 Encounter 를 찾을 수 없어 해당 데이터는 생성이 생략되었습니다. " + identifierSet);
						continue;
					} else {
						throw new IllegalArgumentException(" > 해당 리소스의 Encounter를 찾을 수 없어 오류가 발생하였습니다.");
					}
				}*/
				String organizationId = "-";
				String patientId = "-";
				String encounterId = "-";
				if (ds != null) {
					organizationId = ds.getReferenceList().get("Organization").getReference();
					patientId = ds.getReferenceList().get("Patient").getReference();
					encounterId = ds.getReferenceList().get("Encounter").getReference();
				}

				Condition condition = cmcTransforService.transformPaltDataToFhirCondition(organizationId, patientId, encounterId, rowMap);
				String identifiedRefer = referenceDataMatcher.searchMapperWithMapTypeRetKeyStr(identifierSet);

				//IFhirResourceDao resourceProviderForCondition = myDaoRegistry.getResourceDao("Condition");
				//resourceProviderForCondition.create(condition);

				referenceDataMatcher.setReference(identifiedRefer, condition.getIdentifier().get(0).getValue(), new Reference(condition.getIdPart()));
				loggingInDebugMode("condition Data : " + fn.newJsonParser().encodeResourceToString(condition));
				timer.endTimer("Create Condition...");
			}else if("medicationrequest".equals(entry.getKey())){
				ourLog.info("-------------------------- medicationrequest");
				timer.startTimer();
				// 1. 대상 진료 이력을 해당 리소스 기준에 맞게 조회하여 모든 레퍼런스 가져오기
				Map<String, String> identifierSet = new HashMap<>();
				identifierSet.put("inst_cd", rowMap.get("inst_cd"));
				identifierSet.put("pid", rowMap.get("pid"));
				identifierSet.put("ord_dd", rowMap.get("ord_dd"));
				identifierSet.put("ord_dept_cd", rowMap.get("ord_dept_cd"));
				identifierSet.put("ord_type_cd", rowMap.get("ord_type_cd"));
				identifierSet.put("ord_dr_id", rowMap.get("ord_dr_id"));
				identifierSet.put("io_flag", rowMap.get("io_flag"));
				ReferenceDataSet ds = referenceDataMatcher.searchMapperWithMapType(identifierSet);
				/*
				if (ds == null) {
					if (transformDataOperationConfigProperties.isTransforIgnoreHasNoEncounter()) {
						loggingInDebugMode(" > 해당 리소스의 Encounter 를 찾을 수 없어 해당 데이터는 생성이 생략되었습니다. " + identifierSet);
						continue;
					} else {
						throw new IllegalArgumentException(" > 해당 리소스의 Encounter를 찾을 수 없어 오류가 발생하였습니다.");
					}
				}*/
				String organizationId = "-";
				String patientId = "-";
				String encounterId = "-";
				String practitionerRoleId = "-";
				if (ds != null) {
					organizationId = ds.getReferenceList().get("Organization").getReference();
					patientId = ds.getReferenceList().get("Patient").getReference();
					encounterId = ds.getReferenceList().get("Encounter").getReference();
					practitionerRoleId = ds.getReferenceList().get("PractitionerRole").getReference();
				}

				// 1.1. medication, medicationRequest 를 분개 불가능한 경우 활용
				// 기준자료와 거의 동일하고, edi_cd 외에는 키값 분개 할 이유가 없는 전 병원 공통이므로 mapper에 반영치 아니함.
				Medication medication = cmcTransforService.transformPlatDataToFhirMedication(rowMap);
				//IFhirResourceDao resourceProviderForMedication = myDaoRegistry.getResourceDao("Medication");
				//resourceProviderForMedication.update(medication);

				loggingInDebugMode("Medication : " + cmcTransforService.retResourceToString(medication));

				// 2. 대상 리소스 생성요청
				MedicationRequest medicationReqeust =  cmcTransforService.transformPlatDataToFhirMedicationRequest(organizationId, patientId, practitionerRoleId, encounterId, rowMap);

				// 3. 데이터 생성
				// 아이디 부여가 없는 경우 create
				//IFhirResourceDao resourceProviderForMedicationRequest = myDaoRegistry.getResourceDao("MedicationRequest");
				//resourceProviderForMedicationRequest.update(medicationReqeust);

				loggingInDebugMode("MedicationRequest : " + cmcTransforService.retResourceToString(medicationReqeust));
				timer.endTimer("Create MedicationRequest...");
			}else if("observation".equals(entry.getKey())){
				ourLog.info("-------------------------- observation");
				timer.startTimer();
				Map<String, String> identifierSet = new HashMap<>();
				identifierSet.put("inst_cd", rowMap.get("inst_cd"));
				identifierSet.put("pid", rowMap.get("pid"));
				identifierSet.put("ord_dd", rowMap.get("ord_dd"));
				identifierSet.put("ord_dept_cd", rowMap.get("ord_dept_cd"));
				identifierSet.put("ord_dr_id", rowMap.get("ord_dr_id"));
				identifierSet.put("cret_no", rowMap.get("cret_no"));

				ReferenceDataSet ds = referenceDataMatcher.searchMapperWithMapType(identifierSet);
				/*
				if (ds == null) {
					if (transformDataOperationConfigProperties.isTransforIgnoreHasNoEncounter()) {
						loggingInDebugMode(" > 해당 리소스의 Encounter 를 찾을 수 없어 해당 데이터는 생성이 생략되었습니다. " + identifierSet);
						continue;
					} else {
						throw new IllegalArgumentException(" > 해당 리소스의 Encounter를 찾을 수 없어 오류가 발생하였습니다.");
					}
				}*/
				String organizationId = "-";
				String patientId = "-";
				String encounterId = "-";
				String practitionerRoleId = "-";
				if (ds != null) {
					organizationId = ds.getReferenceList().get("Organization").getReference();
					patientId = ds.getReferenceList().get("Patient").getReference();
					encounterId = ds.getReferenceList().get("Encounter").getReference();
				}

				Observation observation = cmcTransforService.transformPlatDataToFhirObservation(organizationId, patientId, encounterId, rowMap);
				//IFhirResourceDao resourceProviderForServiceRequest = myDaoRegistry.getResourceDao("Observation");
				//resourceProviderForServiceRequest.update(observation);

				loggingInDebugMode("Observation Request : " + cmcTransforService.retResourceToString(observation));
				timer.endTimer("Create MedicationRequest...");
			}else if("observation-exam".equals(entry.getKey())){
				ourLog.info("-------------------------- observation-exam");
				timer.startTimer();
				Map<String, String> identifierSet = new HashMap<>();
				identifierSet.put("inst_cd", rowMap.get("inst_cd"));
				identifierSet.put("pid", rowMap.get("pid"));
				identifierSet.put("ord_dd", rowMap.get("ord_dd"));
				identifierSet.put("ord_dept_cd", rowMap.get("ord_dept_cd"));
				identifierSet.put("ord_type_cd", rowMap.get("io_flag"));
				identifierSet.put("ord_dr_id", rowMap.get("ord_dr_id"));
				identifierSet.put("cret_no", rowMap.get("cret_no"));
				ReferenceDataSet ds = referenceDataMatcher.searchMapperWithMapType(identifierSet);
				/*
				if (ds == null) {
					if (transformDataOperationConfigProperties.isTransforIgnoreHasNoEncounter()) {
						loggingInDebugMode(" > 해당 리소스의 Encounter 를 찾을 수 없어 해당 데이터는 생성이 생략되었습니다. " + identifierSet);
						continue;
					} else {
						throw new IllegalArgumentException(" > 해당 리소스의 Encounter를 찾을 수 없어 오류가 발생하였습니다.");
					}
				}*/
				String organizationId = "-";
				String patientId = "-";
				String encounterId = "-";
				String practitionerRoleId = "-";
				if (ds != null) {
					organizationId = ds.getReferenceList().get("Organization").getReference();
					patientId = ds.getReferenceList().get("Patient").getReference();
					encounterId = ds.getReferenceList().get("Encounter").getReference();
				}

				Observation observation = cmcTransforService.transformPlatDataToFhirObservationExam(organizationId, patientId, encounterId, rowMap);
				//IFhirResourceDao resourceProviderForServiceRequest = myDaoRegistry.getResourceDao("Observation");
				//resourceProviderForServiceRequest.update(observation);

				loggingInDebugMode("Observation Request : " + cmcTransforService.retResourceToString(observation));
				timer.endTimer("Create Observation Exam...");
			}else if("medication".equals(entry.getKey())){
				ourLog.info("-------------------------- Medication");
				// medication, medicationRequest 를 분개 가능한 경우 활용

			}else if("diagnosticreport-pathology".equals(entry.getKey())){
				ourLog.info("-------------------------- diagnosticreport-pathology");
				timer.startTimer();
				// 병리
				Map<String, String> identifierSet = new HashMap<>();
				identifierSet.put("inst_cd", rowMap.get("inst_cd"));
				identifierSet.put("pid", rowMap.get("pid"));
				identifierSet.put("ord_dd", rowMap.get("ord_dd"));
				identifierSet.put("ord_dept_cd", rowMap.get("ord_dept_cd"));
				identifierSet.put("ord_dr_id", rowMap.get("ord_dr_id"));
				identifierSet.put("cret_no", rowMap.get("cret_no"));
				identifierSet.put("ord_type_cd", rowMap.get("prcp_genr_flag"));
				ReferenceDataSet ds = referenceDataMatcher.searchMapperWithMapType(identifierSet);
				/*
				if (ds == null) {
					if (transformDataOperationConfigProperties.isTransforIgnoreHasNoEncounter()) {
						loggingInDebugMode(" > 해당 리소스의 Encounter 를 찾을 수 없어 해당 데이터는 생성이 생략되었습니다. " + identifierSet);
						continue;
					} else {
						throw new IllegalArgumentException(" > 해당 리소스의 Encounter를 찾을 수 없어 오류가 발생하였습니다.");
					}
				}*/
				String organizationId = "-";
				String patientId = "-";
				String encounterId = "-";
				String practitionerRoleId = "-";
				if (ds != null) {
					organizationId = ds.getReferenceList().get("Organization").getReference();
					patientId = ds.getReferenceList().get("Patient").getReference();
					encounterId = ds.getReferenceList().get("Encounter").getReference();
					//practitionerRoleId = ds.getReferenceList().get("practitionerRole").getReference();
				}

				DiagnosticReport diagnosticReport = cmcTransforService.transformPlatDataToFhirDiagnosticReportPathology(organizationId, patientId, encounterId, rowMap);
				//IFhirResourceDao resourceProviderForServiceRequest = myDaoRegistry.getResourceDao("DiagnosticReport");
				//resourceProviderForServiceRequest.update(diagnosticReport);

				loggingInDebugMode("diagnosticReport-pathology Request : " + cmcTransforService.retResourceToString(diagnosticReport));
				timer.endTimer("Create DiagnosticReport-pathology...");
			}else if("diagnosticreport-radiology".equals(entry.getKey())){
				ourLog.info("-------------------------- diagnosticreport-radiology");
				timer.startTimer();
				// 병리
				LinkedHashMap<String, String> identifierSet = new LinkedHashMap<>();
				identifierSet.put("inst_cd", rowMap.get("inst_cd"));
				identifierSet.put("pid", rowMap.get("pid"));
				identifierSet.put("ord_dd", rowMap.get("ord_dd"));
				identifierSet.put("ord_dept_cd", rowMap.get("ord_dept_cd"));
				identifierSet.put("ord_dr_id", rowMap.get("ord_dr_id"));
				identifierSet.put("cret_no", rowMap.get("cret_no"));
				identifierSet.put("ord_type_cd", rowMap.get("io_flag"));
				ReferenceDataSet ds = referenceDataMatcher.searchMapperWithMapType(identifierSet);
				/*
				if (ds == null) {
					if (transformDataOperationConfigProperties.isTransforIgnoreHasNoEncounter()) {
						loggingInDebugMode(" > 해당 리소스의 Encounter 를 찾을 수 없어 해당 데이터는 생성이 생략되었습니다. " + identifierSet);
						continue;
					} else {
						throw new IllegalArgumentException(" > 해당 리소스의 Encounter를 찾을 수 없어 오류가 발생하였습니다.");
					}
				}*/
				String organizationId = "-";
				String patientId = "-";
				String encounterId = "-";
				String practitionerRoleId = "-";
				if (ds != null) {
					organizationId = ds.getReferenceList().get("Organization").getReference();
					patientId = ds.getReferenceList().get("Patient").getReference();
					encounterId = ds.getReferenceList().get("Encounter").getReference();
					practitionerRoleId = ds.getReferenceList().get("PractitionerRole").getReference();
				}

				DiagnosticReport diagnosticReport = cmcTransforService.transformPlatDataToFhirDiagnosticReportRadiology(organizationId, patientId, encounterId, rowMap);
				//IFhirResourceDao resourceProviderForServiceRequest = myDaoRegistry.getResourceDao("DiagnosticReport");
				//resourceProviderForServiceRequest.update(diagnosticReport);

				loggingInDebugMode("diagnosticReport-radiology Request : " + cmcTransforService.retResourceToString(diagnosticReport));
				timer.endTimer("Create DiagnosticReport-radiology...");
			}else if("procedure".equals(entry.getKey())){
				ourLog.info("-------------------------- Procedure");
				timer.startTimer();
				Map<String, String> identifierSet = new HashMap<>();
				identifierSet.put("inst_cd", rowMap.get("inst_cd"));
				identifierSet.put("pid", rowMap.get("pid"));
				identifierSet.put("ord_dd", rowMap.get("ord_dd"));
				identifierSet.put("ord_dept_cd", rowMap.get("ord_dept_cd"));
				identifierSet.put("ord_dr_id", rowMap.get("ord_dr_id"));
				ReferenceDataSet ds = referenceDataMatcher.searchMapperWithMapType(identifierSet);
				/*
				if (ds == null) {
					if (transformDataOperationConfigProperties.isTransforIgnoreHasNoEncounter()) {
						loggingInDebugMode(" > 해당 리소스의 Encounter 를 찾을 수 없어 해당 데이터는 생성이 생략되었습니다. " + identifierSet);
						continue;
					} else {
						throw new IllegalArgumentException(" > 해당 리소스의 Encounter를 찾을 수 없어 오류가 발생하였습니다.");
					}
				}*/
				String organizationId = "-";
				String patientId = "-";
				String encounterId = "-";
				String practitionerRoleId = "-";
				if (ds != null) {
					organizationId = ds.getReferenceList().get("Organization").getReference();
					patientId = ds.getReferenceList().get("Patient").getReference();
					encounterId = ds.getReferenceList().get("Encounter").getReference();
					//practitionerRoleId = ds.getReferenceList().get("PractitionerRole").getReference();
				}

				Procedure procedure = cmcTransforService.transformPlatDataToFhirProcedure(organizationId, patientId, encounterId, rowMap);
				//IFhirResourceDao resourceProviderForProcedure = myDaoRegistry.getResourceDao("Procedure");
				//resourceProviderForProcedure.update(procedure);

				loggingInDebugMode("Procedure Request : " + cmcTransforService.retResourceToString(procedure));
				timer.endTimer("Create Procedure...");
			}else if ("serviceRequest".equals(entry.getKey())) {
				ourLog.info("-------------------------- ServiceRequest");
				Map<String, String> identifierSet = new HashMap<>();
				identifierSet.put("inst_cd", rowMap.get("inst_cd"));
				identifierSet.put("pid", rowMap.get("pid"));
				identifierSet.put("ord_dd", rowMap.get("prcp_dd"));
				identifierSet.put("ord_dept_cd", rowMap.get("rgst_dept_cd"));
				identifierSet.put("ord_type_cd", rowMap.get("io_flag"));
				identifierSet.put("ord_dr_id", rowMap.get("prcp_dr_id"));
				identifierSet.put("io_flag", rowMap.get("io_flag"));
				ReferenceDataSet ds = referenceDataMatcher.searchMapperWithMapType(identifierSet);
				/*
				if (ds == null) {
					if (transformDataOperationConfigProperties.isTransforIgnoreHasNoEncounter()) {
						loggingInDebugMode(" > 해당 리소스의 Encounter 를 찾을 수 없어 해당 데이터는 생성이 생략되었습니다. " + identifierSet);
						continue;
					} else {
						throw new IllegalArgumentException(" > 해당 리소스의 Encounter를 찾을 수 없어 오류가 발생하였습니다.");
					}
				}*/
				String organizationId = "-";
				String patientId = "-";
				String encounterId = "-";
				String practitionerRoleId = "-";
				if (ds != null) {
					organizationId = ds.getReferenceList().get("Organization").getReference();
					patientId = ds.getReferenceList().get("Patient").getReference();
					encounterId = ds.getReferenceList().get("Encounter").getReference();
					practitionerRoleId = ds.getReferenceList().get("PractitionerRole").getReference();
				}

				ServiceRequest serviceRequest = cmcTransforService.transformPlatDataToFhirServiceRequest(organizationId, patientId, practitionerRoleId, encounterId, rowMap);

				IFhirResourceDao resourceProviderForServiceRequest = myDaoRegistry.getResourceDao("ServiceRequest");
				//resourceProviderForServiceRequest.update(serviceRequest);

				loggingInDebugMode("service Request : " + cmcTransforService.retResourceToString(serviceRequest));
			}else{
				loggingInDebugMode(" >>>>> UN Develops Resource : " + entry.getKey());
			}
		}
	}

	// Mapping 의 값이 Matcher 에 존재하지 않으면, FHIR 에서 해당 Encounter 에서 데이터를 가져와서 Matcher에 등록시킨다.
	private void registEncounterReferenceInFHIRResource(String organizationId, LinkedHashMap<String, String> encounterKey){
		loggingInDebugMode(" registEncounterReferenceInFHIRResource Start ... ");

		// 1. 최소특정조건으로 Encounter 의 조회
		String uniqueId = "ENC."
			+ organizationId + "."
			+ encounterKey.get("ord_dept_cd") + "."
			+ encounterKey.get("ord_dd") + "."
			+ encounterKey.get("cret_no") + "."
			+ encounterKey.get("ord_dr_id");

		IFhirResourceDao resourceProviderForEncounter = myDaoRegistry.getResourceDao("Encounter");
		IQueryParameterType stringParam = new StringParam(uniqueId);
		SearchParameterMap searchParameterMapForStructureDef = new SearchParameterMap().add(StructureDefinition.SP_RES_ID, stringParam);
		searchParameterMapForStructureDef.setSearchTotalMode(SearchTotalModeEnum.ESTIMATED);
		searchParameterMapForStructureDef.setCount(1000);

		IBundleProvider results = resourceProviderForEncounter.search(searchParameterMapForStructureDef);
		loggingInDebugMode(" > Encounter Search Result ... : " + results.size());

		// 2. 해당 필수키와 유사한 Encounter 가 있는 경우
		// 레퍼런스 맵에 등록한다.
		// 1) 맵에 등록할 IdentifierSet
		LinkedHashMap<String, String> identifierKeySet = new LinkedHashMap<>();
		for(IBaseResource bs : results.getAllResources()){
			Encounter encounter = (Encounter) bs;
			identifierKeySet = encounterKey;

			String ordType = "";
			if("AMB".equals(encounter.getClass_().getCode())){
				ordType = "O";
			}else if("EMER".equals(encounter.getClass_().getCode())){
				ordType = "E";
			}else if("IMP".equals(encounter.getClass_().getCode())){
				ordType = "I";
			}else{
				throw new IllegalArgumentException(" 해당 대상자의 FHIR 의 적재된 Encounter " + encounter.getId() + " 의 Class값이 기능상 정의되지 않은 값이 조회되었습니다.");
			}
			identifierKeySet.put("ord_type_cd", ordType);

			// 2) 맵에 등록할 Encounter 연관 Identifier Reference
			Map<String, Reference> referenceMap = new HashMap<>();
			referenceMap.put("Encounter", new Reference(encounter.getIdPart()));
			referenceMap.put("Patient", new Reference(encounter.getSubject().getReference().replace("Patient/", "")));
			referenceMap.put("PractitionerRole", new Reference(encounter.getParticipant().get(0).getIndividual().getReference().replace("PractitionerRole/", "")));
			referenceMap.put("Organization", new Reference(encounter.getServiceProvider().getReference().replace("Organization/", "")));

			referenceDataMatcher.inputMappingData(identifierKeySet, referenceMap);
		}
	}

	private void loggingInDebugMode(String arg){
		if(transformDataOperationConfigProperties.isTransforLogging()){
			ourLog.info(arg);
		}
	}
}

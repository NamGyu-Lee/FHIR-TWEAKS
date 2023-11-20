package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.starter.transfor.config.TransformDataOperationConfigProperties;
import ca.uhn.fhir.jpa.starter.transfor.dto.base.ReferenceDataMatcher;
import ca.uhn.fhir.jpa.starter.transfor.dto.base.ReferenceDataSet;
import ca.uhn.fhir.jpa.starter.transfor.service.cmc.CmcDataTransforServiceImpl;
import com.google.gson.*;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;

import java.util.*;

/** 간단한 서버내 FHIR 기능 테스트 목적
 *  서버연동 하지 않은 상태임.
 * The type Sample test.
 */
public class SampleTest {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(SampleTest.class);

	ReferenceDataMatcher referenceDataMatcher = new ReferenceDataMatcher();

	@Test
	public void test(){
		String arg = "{\n" +
			"  \"organization\": [\n" +
			"    {\n" +
			"      \"proc_corp_cd\": \"urn:oid:1.2.410.100110.10.11100338\",\n" +
			"      \"hosp_flag\": \"01\",\n" +
			"      \"inst_cd\": \"012\",\n" +
			"      \"telno\": \"02-2258-5518\",\n" +
			"      \"hosp_addr\": \"서울 서초구  반포대로222(반포4동)\",\n" +
			"      \"hosp_nm\": \"가톨릭대학교 서울성모병원\",\n" +
			"      \"zipcd\": \"06591\"\n" +
			"    }\n" +
			"  ],\n" +
			"  \"patient\": [\n" +
			"    {\n" +
			"      \"pspt_no\": \"-\",\n" +
			"      \"proc_corp_cd\": \"11100338\",\n" +
			"      \"relign_cd\": \"-\",\n" +
			"      \"nati_cd\": \"-\",\n" +
			"      \"inst_cd\": \"012\",\n" +
			"      \"telno\": \"01000000000\",\n" +
			"      \"hosp_nm\": \"가톨릭대학교 서울성모병원\",\n" +
			"      \"forger_yn\": \"N\",\n" +
			"      \"detl_addr\": \"-\",\n" +
			"      \"pid\": \"10280955\",\n" +
			"      \"addr\": \"-\",\n" +
			"      \"brth_dd\": \"19670101\",\n" +
			"      \"sex_cd\": \"M\",\n" +
			"      \"home_telno\": \"-\",\n" +
			"      \"eng_nm\": \"유순이\",\n" +
			"      \"hng_nm\": \"유순이\",\n" +
			"      \"hosp_addr\": \"서울 서초구  반포대로222(반포4동)\",\n" +
			"      \"prtb_telno\": \"010-0000-0000\",\n" +
			"      \"zipcd\": \"000000\"\n" +
			"    }\n" +
			"  ],\n" +
			"  \"practitionData\": [\n" +
			"    {\n" +
			"      \"prcp_dr_nm\": \"doctorname\",\n" +
			"      \"prcp_dr_id\": \"20180001\",\n" +
			"      \"rgst_dept_nm\": \"deptNm\",\n" +
			"      \"rgst_dept_cd\": \"20900000\",\n" +
			"      \"clam_dept_cd\": \"01\",\n" +
			"      \"clam_dept_nm\": \"내과\",\n" +
			"      \"licns_no\": \"1233\"\n" +
			"    }\n" +
			"  ],\n" +
			"  \"serviceRequest\": [\n" +
			"    {\n" +
			"      \"io_flag\": \"O\",\n" +
			"      \"prcp_cd\": \"1234\",\n" +
			"      \"prcp_no\": 1,\n" +
			"      \"perd_cnts\": \"1\",\n" +
			"      \"freq_max_tmcnt\": 1,\n" +
			"      \"prcp_nm\": \"prcpname\",\n" +
			"      \"repeat_tmcnt\": 1,\n" +
			"      \"repeat_max_tmcnt\": 1,\n" +
			"      \"freq_cnt\": 1,\n" +
			"      \"exec_to_tm\": \"1521\",\n" +
			"      \"exec_time_desc\": \"TIMEDESC\",\n" +
			"      \"prcp_dr_id\": \"20180001\",\n" +
			"      \"perd_unit_cd\": \"CD\",\n" +
			"      \"exec_from_tm\": \"142121\",\n" +
			"      \"prcp_dd\": \"20231101\",\n" +
			"      \"rgst_dept_nm\": \"deptNm\",\n" +
			"      \"prcp_desc\": \"PORCPDESC\",\n" +
			"      \"svc_req_key\": \"14142345\",\n" +
			"      \"inst_cd\": \"012\",\n" +
			"      \"agre_hist_no\": 1,\n" +
			"      \"pid\": \"10280955\",\n" +
			"      \"tmcnt_val\": 1,\n" +
			"      \"rgst_dept_cd\": \"20900000\",\n" +
			"      \"prcp_dr_nm\": \"doctorname\",\n" +
			"      \"prcp_expt_desc\": \"PRCPEXPTDESC\",\n" +
			"      \"exec_from_dd\": \"20231109\",\n" +
			"      \"perd_max_tmcnt\": \"1\",\n" +
			"      \"exec_to_dd\": \"20231109\",\n" +
			"      \"exec_stat_cd\": \"STA\",\n" +
			"      \"repeat_unit_cd\": \"CD\",\n" +
			"      \"freq_unit_cd\": \"CD\"\n" +
			"    }\n" +
			"  ],\n" +
			"  \"encounter\": [\n" +
			"  {   \n" +
			"      \"inst_cd\" : \"012\",\n" +
			"      \"ord_dept_cd\": \"123123\",\n" +
			"      \"pid\": \"10280955\",\n" +
			"      \"clam_dept_cd\": \"12\",\n" +
			"      \"clam_dept_nm\": \"그런과\",\n" +
			"      \"ord_dr_id\": \"20180001\",\n" +
			"      \"cret_no\": \"1\",\n" +
			"      \"ord_dd\": \"20231101\",\n" +
			"      \"dsch_dd\": \"20231101\",\n" +
			"      \"io_flag\": \"O\"\n" +
			"   }\n" +
			"  ]\n" +
			"}";

			JsonParser jsonParser = new JsonParser();
			JsonElement jsonElement = jsonParser.parse(arg);
			JsonObject jsonObject = jsonElement.getAsJsonObject();

			System.out.println("JSON Size : " + jsonObject.size());

			System.out.println(jsonObject.getAsJsonArray("organization").size());

			Queue<Map.Entry<String, JsonElement>> sortedQueue = sortingCreateResourceArgument(jsonObject);

			CmcDataTransforServiceImpl cmcDataTransforService = new CmcDataTransforServiceImpl();

			// 실질적인 변환 부분.
			String organizationId = "";
			String practitionerId = "";
			String practitionerRoleId = "";
			String patientId = "";
			String encounterId = "";
			while(sortedQueue.size() != 0){
				Map.Entry<String, JsonElement> entry = sortedQueue.poll();
				JsonElement elements = entry.getValue();
				JsonArray jsonArray = elements.getAsJsonArray();
				if(jsonArray.size() <= 0){
					continue;
				}

				for(int eachRowCount = 0; jsonArray.size() > eachRowCount; eachRowCount++){
					JsonObject eachRowJsonObj = jsonArray.get(eachRowCount).getAsJsonObject();
					Map<String, String> rowMap = convertJsonObjectToMap(eachRowJsonObj);

					System.out.println(entry.getKey());
					if ("organization".equals(entry.getKey())) {
						Organization organization = cmcDataTransforService.transformPlatDataToFhirOrganization(rowMap);
						organizationId = organization.getId();

						FhirContext fn = new FhirContext(FhirVersionEnum.R4);
						System.out.println("organ : " + fn.newJsonParser().encodeResourceToString(organization));
					} else if ("patient".equals(entry.getKey())) {
						Patient patient = cmcDataTransforService.transformPlatDataToFhirPatient(organizationId, rowMap);

						patientId = patient.getId();

						FhirContext fn = new FhirContext(FhirVersionEnum.R4);
						System.out.println("patient : " + fn.newJsonParser().encodeResourceToString(patient));
					} else if ("practitionData".equals(entry.getKey())) {
						Practitioner practitioner = cmcDataTransforService.transformPlatDataToFhirPractitioner(organizationId, rowMap);
						practitionerId = practitioner.getId();

						PractitionerRole practitionerRole = cmcDataTransforService.transformPlatDataToFhirPractitionerRole(organizationId, practitionerId, rowMap);
						practitionerRoleId = practitionerRole.getId();

						FhirContext fn = new FhirContext(FhirVersionEnum.R4);
						System.out.println("practition Data : " + fn.newJsonParser().encodeResourceToString(practitioner));
						System.out.println("practitionRole Data : " + fn.newJsonParser().encodeResourceToString(practitionerRole));

					} else if ("encounter".equals(entry.getKey())) {
						Encounter encounter = cmcDataTransforService.transformPlatDataToFhirEncounter(organizationId, practitionerRoleId, patientId, rowMap);

						encounterId = encounter.getId();

						FhirContext fn = new FhirContext(FhirVersionEnum.R4);
						System.out.println("encounter Data : " + fn.newJsonParser().encodeResourceToString(encounter));
					} else if ("serviceRequest".equals(entry.getKey())) {
						ServiceRequest serviceRequest = cmcDataTransforService.transformPlatDataToFhirServiceRequest(organizationId, encounterId, patientId, practitionerRoleId, rowMap);

						System.out.println("service Request : " + cmcDataTransforService.retResourceToString(serviceRequest));
					}
				}
			}
	}

	private TransformDataOperationConfigProperties transformDataOperationConfigProperties = new TransformDataOperationConfigProperties();

	private Queue<Map.Entry<String, JsonElement>> sortingCreateResourceArgument(JsonObject jsonObject){
		Queue<Map.Entry<String, JsonElement>> upperSortingQueue = new LinkedList<>();
		Queue<Map.Entry<String, JsonElement>> lowerSortingQueue = new LinkedList<>();
		Queue<Map.Entry<String, JsonElement>> nonSortingQueue = new LinkedList<>();
		Queue<Map.Entry<String, JsonElement>> sortedQueue = new LinkedList<>();

		boolean isInjected;
		for(Map.Entry<String, JsonElement> eachEntry : jsonObject.entrySet()){
			isInjected = false;

			// first
			for(String upperString : transformDataOperationConfigProperties.getResourceUpperSortingReferenceSet()){
				if(eachEntry.getKey().equals(upperString)){
					upperSortingQueue.add(eachEntry);
					isInjected = true;
				}
			}

			// lower
			for(String lowerString : transformDataOperationConfigProperties.getResourceLowerSortingReferenceSet()){
				if(eachEntry.getKey().equals(lowerString)){
					lowerSortingQueue.add(eachEntry);
					isInjected = true;
				}
			}

			// middle
			if(!isInjected) {
				nonSortingQueue.add(eachEntry);
			}
		}
		// 1.2. merge
		while(upperSortingQueue.size() != 0){
			sortedQueue.add(upperSortingQueue.poll());
		}
		while(nonSortingQueue.size() != 0){
			sortedQueue.add(nonSortingQueue.poll());
		}
		while(lowerSortingQueue.size() != 0){
			sortedQueue.add(lowerSortingQueue.poll());
		}

		return sortedQueue;
	}


	private Map<String, String> convertJsonObjectToMap(JsonObject jsonObject) {
		Gson gson = new Gson();
		return gson.fromJson(jsonObject, Map.class);
	}

	@Test
	void TestValue(){
		ReferenceDataMatcher referenceDataMatcher = new ReferenceDataMatcher();

		// 1. 해당 타입 구성
		LinkedHashMap<String, String> eachPrcp = new LinkedHashMap<>();
		eachPrcp.put("prcp_dd", "20231121");
		eachPrcp.put("prcp_cd", "prcpcd");
		eachPrcp.put("ioflag", "O");
		eachPrcp.put("pid", "3211");
		eachPrcp.put("prcp_dr_id", "2011");
		referenceDataMatcher.inputMappingData("1", eachPrcp, new HashMap<>());
		try {
			referenceDataMatcher.inputMappingData(eachPrcp, null);
		}catch(Exception e){
			System.out.println("ok");
		}
		try {
			referenceDataMatcher.inputMappingData("2", eachPrcp, new HashMap<>());
		}catch(Exception e){
			System.out.println("ok");
		}

		// 2. 구성된 타입별 테스트
		Map<String, Reference> refSet = new HashMap<>();
		Reference ref = new Reference("Patient/3");
		referenceDataMatcher.setReference("1", "Patient", ref);

		// 3. 조회
		Reference refv = referenceDataMatcher.searchMapperWithKeyType("1").getReferenceList().get("Patient");

		FhirContext fn = new FhirContext(FhirVersionEnum.R4);
		System.out.println("Data : " + fn.newJsonParser().encodeToString(refv));

	}

	/**
	 * Test 3. Key 활용
	 */
	@Test
	public void test3(){
		String arg = "{\n" +
			"  \"organization\": [\n" +
			"    {\n" +
			"      \"proc_corp_cd\": \"urn:oid:1.2.410.100110.10.11100338\",\n" +
			"      \"hosp_flag\": \"01\",\n" +
			"      \"inst_cd\": \"012\",\n" +
			"      \"telno\": \"02-2258-5518\",\n" +
			"      \"hosp_addr\": \"서울 서초구  반포대로222(반포4동)\",\n" +
			"      \"hosp_nm\": \"가톨릭대학교 서울성모병원\",\n" +
			"      \"zipcd\": \"06591\"\n" +
			"    }\n" +
			"  ],\n" +
			"  \"patient\": [\n" +
			"    {\n" +
			"      \"pspt_no\": \"-\",\n" +
			"      \"proc_corp_cd\": \"11100338\",\n" +
			"      \"relign_cd\": \"-\",\n" +
			"      \"nati_cd\": \"-\",\n" +
			"      \"inst_cd\": \"012\",\n" +
			"      \"telno\": \"01000000000\",\n" +
			"      \"hosp_nm\": \"가톨릭대학교 서울성모병원\",\n" +
			"      \"forger_yn\": \"N\",\n" +
			"      \"detl_addr\": \"-\",\n" +
			"      \"pid\": \"10280955\",\n" +
			"      \"addr\": \"-\",\n" +
			"      \"brth_dd\": \"19670101\",\n" +
			"      \"sex_cd\": \"M\",\n" +
			"      \"home_telno\": \"-\",\n" +
			"      \"eng_nm\": \"유순이\",\n" +
			"      \"hng_nm\": \"유순이\",\n" +
			"      \"hosp_addr\": \"서울 서초구  반포대로222(반포4동)\",\n" +
			"      \"prtb_telno\": \"010-0000-0000\",\n" +
			"      \"zipcd\": \"000000\"\n" +
			"    }\n" +
			"  ],\n" +
			"  \"practitionData\": [\n" +
			"    {\n" +
			"      \"prcp_dr_nm\": \"doctorname\",\n" +
			"      \"prcp_dr_id\": \"20180001\",\n" +
			"      \"rgst_dept_nm\": \"deptNm\",\n" +
			"      \"rgst_dept_cd\": \"20900000\",\n" +
			"      \"clam_dept_cd\": \"01\",\n" +
			"      \"clam_dept_nm\": \"내과\",\n" +
			"      \"licns_no\": \"1233\"\n" +
			"    }\n" +
			"  ],\n" +
			"  \"serviceRequest\": [\n" +
			"    {\n" +
			"      \"io_flag\": \"O\",\n" +
			"      \"prcp_cd\": \"1234\",\n" +
			"      \"prcp_no\": 1,\n" +
			"      \"perd_cnts\": \"1\",\n" +
			"      \"freq_max_tmcnt\": 1,\n" +
			"      \"prcp_nm\": \"prcpname\",\n" +
			"      \"repeat_tmcnt\": 1,\n" +
			"      \"repeat_max_tmcnt\": 1,\n" +
			"      \"freq_cnt\": 1,\n" +
			"      \"exec_to_tm\": \"1521\",\n" +
			"      \"exec_time_desc\": \"TIMEDESC\",\n" +
			"      \"prcp_dr_id\": \"20180001\",\n" +
			"      \"perd_unit_cd\": \"CD\",\n" +
			"      \"exec_from_tm\": \"142121\",\n" +
			"      \"prcp_dd\": \"20231101\",\n" +
			"      \"rgst_dept_nm\": \"deptNm\",\n" +
			"      \"prcp_desc\": \"PORCPDESC\",\n" +
			"      \"svc_req_key\": \"14142345\",\n" +
			"      \"inst_cd\": \"012\",\n" +
			"      \"agre_hist_no\": 1,\n" +
			"      \"pid\": \"10280955\",\n" +
			"      \"tmcnt_val\": 1,\n" +
			"      \"rgst_dept_cd\": \"20900000\",\n" +
			"      \"prcp_dr_nm\": \"doctorname\",\n" +
			"      \"prcp_expt_desc\": \"PRCPEXPTDESC\",\n" +
			"      \"exec_from_dd\": \"20231109\",\n" +
			"      \"perd_max_tmcnt\": \"1\",\n" +
			"      \"exec_to_dd\": \"20231109\",\n" +
			"      \"exec_stat_cd\": \"STA\",\n" +
			"      \"repeat_unit_cd\": \"CD\",\n" +
			"      \"freq_unit_cd\": \"CD\"\n" +
			"    }\n" +
			"  ],\n" +
			"  \"encounter\": [\n" +
			"  {   \n" +
			"      \"inst_cd\" : \"012\",\n" +
			"      \"ord_dept_cd\": \"123123\",\n" +
			"      \"pid\": \"10280955\",\n" +
			"      \"clam_dept_cd\": \"12\",\n" +
			"      \"clam_dept_nm\": \"그런과\",\n" +
			"      \"ord_dr_id\": \"20180001\",\n" +
			"      \"cret_no\": \"1\",\n" +
			"      \"ord_dd\": \"20231101\",\n" +
			"      \"dsch_dd\": \"20231101\",\n" +
			"      \"io_flag\": \"O\"\n" +
			"   }\n" +
			"  ]\n" +
			"}";

		JsonParser jsonParser = new JsonParser();
		JsonElement jsonElement = jsonParser.parse(arg);
		JsonObject jsonObject = jsonElement.getAsJsonObject();

		System.out.println("JSON Size : " + jsonObject.size());
		System.out.println(jsonObject.getAsJsonArray("organization").size());

		Queue<Map.Entry<String, JsonElement>> sortedQueue = sortingCreateResourceArgument(jsonObject);

		CmcDataTransforServiceImpl cmcDataTransforService = new CmcDataTransforServiceImpl();

		// 실질적인 변환 부분
		// 해당 영역부터 개별 대상자로 한정
		Map<String, String> noSearchArg = new HashMap<>();
		noSearchArg.put("Don't Search Main Volumns", "9999999");
		referenceDataMatcher.inputMappingData("Standard-Ref", noSearchArg, new HashMap<>());
		while(sortedQueue.size() != 0){
			Map.Entry<String, JsonElement> entry = sortedQueue.poll();
			JsonElement elements = entry.getValue();
			JsonArray jsonArray = elements.getAsJsonArray();
			if(jsonArray.size() <= 0){
				continue;
			}

			for(int eachRowCount = 0; jsonArray.size() > eachRowCount; eachRowCount++){
				JsonObject eachRowJsonObj = jsonArray.get(eachRowCount).getAsJsonObject();
				Map<String, String> rowMap = convertJsonObjectToMap(eachRowJsonObj);

				System.out.println(entry.getKey());
				if ("organization".equals(entry.getKey())) {
					Organization organization = cmcDataTransforService.transformPlatDataToFhirOrganization(rowMap);

					referenceDataMatcher.setReference("Standard-Ref", "Organization", new Reference(organization.getId()));

					FhirContext fn = new FhirContext(FhirVersionEnum.R4);
					System.out.println("organ : " + fn.newJsonParser().encodeResourceToString(organization));
				} else if ("patient".equals(entry.getKey())) {
					String organizationId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get("Organization").getReference();
					Patient patient = cmcDataTransforService.transformPlatDataToFhirPatient(organizationId, rowMap);

					referenceDataMatcher.setReference("Standard-Ref", "Patient" , new Reference(patient.getId()));

					FhirContext fn = new FhirContext(FhirVersionEnum.R4);
					System.out.println("patient : " + fn.newJsonParser().encodeResourceToString(patient));
				} else if ("practitionData".equals(entry.getKey())) {
					String organizationId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get("Organization").getReference();

					Practitioner practitioner = cmcDataTransforService.transformPlatDataToFhirPractitioner(organizationId, rowMap);

					referenceDataMatcher.setReference("Standard-Ref", practitioner.getId(), new Reference(practitioner.getId()));
					String practitionerId = practitioner.getId();

					PractitionerRole practitionerRole = cmcDataTransforService.transformPlatDataToFhirPractitionerRole(organizationId, practitionerId, rowMap);
					referenceDataMatcher.setReference("Standard-Ref", practitionerRole.getId(), new Reference(practitionerRole.getId()));

					FhirContext fn = new FhirContext(FhirVersionEnum.R4);
					System.out.println("practition Data : " + fn.newJsonParser().encodeResourceToString(practitioner));
					System.out.println("practitionRole Data : " + fn.newJsonParser().encodeResourceToString(practitionerRole));

				} else if ("encounter".equals(entry.getKey())) {
					String organizationId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get("Organization").getReference();
					String patientId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get("Patient").getReference();
					String practitionerRoleId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get("PROL." + organizationId + "." + rowMap.get("ord_dr_id")).getReference();
					String practitionerId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get("PRCT." + organizationId + "." + rowMap.get("ord_dr_id")).getReference();

					Encounter encounter = cmcDataTransforService.transformPlatDataToFhirEncounter(organizationId, practitionerRoleId, patientId, rowMap);

					//encounterId = encounter.getId();
					LinkedHashMap<String, String> identifierSet = new LinkedHashMap<>();
					identifierSet.put("inst_cd", rowMap.get("inst_cd"));
					identifierSet.put("pid", rowMap.get("pid"));
					identifierSet.put("cret_no", rowMap.get("cret_no"));
					identifierSet.put("ord_dr_id", rowMap.get("ord_dr_id"));
					identifierSet.put("ord_dd", rowMap.get("ord_dd"));
					identifierSet.put("io_flag", rowMap.get("io_flag"));

					for(String key : identifierSet.keySet()){
						System.out.println(key + " : " + identifierSet.get(key));
					}

					Map<String, Reference> encounterIncludedRefSet = new HashMap<>();
					encounterIncludedRefSet.put("Organization", new Reference(organizationId));
					encounterIncludedRefSet.put("Patient", new Reference(patientId));
					encounterIncludedRefSet.put("Encounter", new Reference(encounter.getId()));
					encounterIncludedRefSet.put("Practitioner", new Reference(practitionerId));
					encounterIncludedRefSet.put("PractitionerRole", new Reference(practitionerRoleId));
					String id = referenceDataMatcher.inputMappingData(identifierSet, encounterIncludedRefSet);

					FhirContext fn = new FhirContext(FhirVersionEnum.R4);
					System.out.println("encounter Data : " + fn.newJsonParser().encodeResourceToString(encounter));
				} else if ("serviceRequest".equals(entry.getKey())) {

					Map<String, String> identifierSet = new HashMap<>();
					identifierSet.put("inst_cd", rowMap.get("inst_cd"));
					identifierSet.put("pid", rowMap.get("pid"));
					identifierSet.put("ord_dr_id", rowMap.get("prcp_dr_id"));
					identifierSet.put("ord_dd", rowMap.get("prcp_dd"));
					identifierSet.put("io_flag", rowMap.get("io_flag"));
					ReferenceDataSet ds = referenceDataMatcher.searchMapperWithMapType(identifierSet);

					String organizationId = ds.getReferenceList().get("Organization").getReference();
					String patientId = ds.getReferenceList().get("Patient").getReference();
					String practitionerId = ds.getReferenceList().get("Practitioner").getReference();
					String practitionerRoleId = ds.getReferenceList().get("PractitionerRole").getReference();
					String encounterId = ds.getReferenceList().get("Encounter").getReference();

					ServiceRequest serviceRequest = cmcDataTransforService.transformPlatDataToFhirServiceRequest(organizationId, patientId, practitionerRoleId, encounterId, rowMap);

					System.out.println("service Request : " + cmcDataTransforService.retResourceToString(serviceRequest));
				}
			}
		}
	}


}

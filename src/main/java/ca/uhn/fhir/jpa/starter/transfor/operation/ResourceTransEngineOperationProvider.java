package ca.uhn.fhir.jpa.starter.transfor.operation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.provider.BaseJpaProvider;
import ca.uhn.fhir.jpa.starter.transfor.base.core.TransformEngine;
import ca.uhn.fhir.jpa.starter.transfor.base.map.RuleNode;
import ca.uhn.fhir.jpa.starter.transfor.code.ResourceReferenceCode;
import ca.uhn.fhir.jpa.starter.transfor.config.TransformDataOperationConfigProperties;
import ca.uhn.fhir.jpa.starter.transfor.base.reference.structure.ReferenceDataMatcher;
import ca.uhn.fhir.jpa.starter.transfor.base.reference.structure.ReferenceDataSet;
import ca.uhn.fhir.jpa.starter.transfor.dto.comm.ResponseDto;
import ca.uhn.fhir.jpa.starter.transfor.operation.code.ResponseStateCode;
import ca.uhn.fhir.jpa.starter.transfor.util.TransformUtil;
import ca.uhn.fhir.jpa.starter.validation.config.CustomValidationRemoteConfigProperties;
import ca.uhn.fhir.rest.annotation.Operation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/** 2023 . 11. 27.
 *  FHIR의 데이터 생성의 대하여 TransformEngine 의 기능을 활용하여 서비스를 구성한다.
 *  사용자에게 서비스를 제공하는 Controller 역할을 수행한다.
 *  old 모델. 유동적 구성 가능 모델  ver 2.0
 */
public class ResourceTransEngineOperationProvider extends BaseJpaProvider {

	private static final Logger ourLog = LoggerFactory.getLogger(ResourceTransEngineOperationProvider.class);

	ReferenceDataMatcher referenceDataMatcher = new ReferenceDataMatcher();

	private TransformEngine transformEngine;

	private TransformUtil transformUtil;

	private FhirContext fn;

	public ResourceTransEngineOperationProvider(FhirContext fn){
		this.fn = fn;
		transformEngine = new TransformEngine(null, customValidationRemoteConfigProperties);

		// 캐시처리
		Map<String, String> noSearchArg = new HashMap<>();
		noSearchArg.put("Don't Search Main Volumns", "-");
		referenceDataMatcher.inputMappingData("Standard-Ref", noSearchArg, new HashMap<>());
	}

	private TransformDataOperationConfigProperties transformDataOperationConfigProperties;
	@Autowired
	void setTransformDataOperationConfigProperties(TransformDataOperationConfigProperties transformDataOperationConfigProperties){
		this.transformDataOperationConfigProperties = transformDataOperationConfigProperties;
		transformUtil = new TransformUtil(transformDataOperationConfigProperties);
	}

	private CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties;
	@Autowired
	void setCustomValidationRemoteConfigProperties(CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties){
		this.customValidationRemoteConfigProperties = customValidationRemoteConfigProperties;
	}

	@Operation(
		name="$tranform-resource-basic",
		idempotent = false,
		manualRequest = true,
		manualResponse = true
	)
	public void transforResourceStandardService(HttpServletRequest theServletRequest, HttpServletResponse theResponse) throws IOException {
		String retMessage = "-";
		ourLog.info(" > Create Engine Based Data Transfor initalized.. ");

		try {
			byte[] bytes = IOUtils.toByteArray(theServletRequest.getInputStream());
			String bodyData = new String(bytes);
			JsonParser jsonParser = new JsonParser();
			JsonElement jsonElement = jsonParser.parse(bodyData);
			JsonObject jsonObject = jsonElement.getAsJsonObject();

			// sorting
			Queue<Map.Entry<String, JsonElement>> sortedQueue = transformUtil.sortingCreateResourceArgument(jsonObject);

			// 실질적인 변환 부분
			// 해당 영역부터 개별 대상자로 한정
			referenceDataMatcher.printAllReference("Standard-Ref");

			// 2. 생성 시작
			Bundle bundle = new Bundle();
			while(sortedQueue.size() != 0){
				Map.Entry<String, JsonElement> entry = sortedQueue.poll();
				List<IBaseResource> baseResourceList =  new ArrayList<>(); // this.createResource(entry);
				for(IBaseResource resource : baseResourceList){
					Bundle.BundleEntryComponent comp = new Bundle.BundleEntryComponent();
					comp.setResource((Resource)resource);
					bundle.addEntry(comp);
				}
			}

			// 3. 결과 리턴
			String retBundle = fn.newJsonParser().encodeResourceToString(bundle);
			ResponseDto<String> responseDto = ResponseDto.<String>builder().success(ResponseStateCode.OK.getSuccess()).stateCode(ResponseStateCode.OK.getStateCode()).errorReason("-").body(retBundle).build();
			ObjectMapper mapper = new ObjectMapper();
			String jsonStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseDto);

			retMessage = jsonStr;

		}catch(Exception e){
			e.printStackTrace();

			ResponseDto<String> responseDto = ResponseDto.<String>builder().success(ResponseStateCode.BAD_REQUEST.getSuccess()).stateCode(ResponseStateCode.BAD_REQUEST.getStateCode()).errorReason("-").body("-").build();
			ObjectMapper mapper = new ObjectMapper();
			String jsonStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseDto);

			retMessage = jsonStr;

		}finally{
			theResponse.setCharacterEncoding("UTF-8");
			theResponse.setContentType("text/plain");
			theResponse.getWriter().write(retMessage);
			theResponse.getWriter().close();
		}
	}

	/*
	private List<IBaseResource> createResource(Map.Entry<String, JsonElement> entry){
		List<IBaseResource> retResourceList = new ArrayList<>();
		JsonElement elements = entry.getValue();
		JsonArray jsonArray = elements.getAsJsonArray();

		// 리소스 병합 수행
		// 리소스 생성요청별 맵 구성
		// Resource : MapType 을 1:1로 고정
		JsonObject searchFirstSourceData = jsonArray.get(0).getAsJsonObject();
		String mapScript = "";
		String mapType = "";
		try {
			JSONObject sourceObject = new JSONObject(searchFirstSourceData.toString());
			mapType = sourceObject.getString("maptype");
			if(mapType == null){
				ourLog.error("[ERR] 해당 리소스에 MapType이 없습니다.");
				throw new IllegalArgumentException("[ERR] 해당 리소스에 MapType이 없습니다.");
			}else{
				mapScript = TransformUtil.getMap(mapType);
			}
			
		}catch(JSONException e){
			throw new IllegalArgumentException("[ERR] Source 데이터를 조회하는 시점에서 JSONException 오류가 발생하였습니다.");
		}

		//  2) Map 구성
		List<RuleNode> ruleNodeList = transformEngine.createRuleNodeTree(mapScript);
		if(mapScript == "" || mapScript == null || mapType == "" || mapType == null){
			throw new IllegalArgumentException("[ERR] Map이 조회되지 않았습니다.");
		}
		Set<String> keySet =
		Set<String> mergeKeySet =

		// 3) Map을 통한 데이터 merge 수행
		List<JsonObject> jsonObjectList = new ArrayList<>();
		for(int eachRowCount = 0; jsonArray.size() > eachRowCount; eachRowCount++){
			jsonObjectList.add(jsonArray.get(eachRowCount).getAsJsonObject());
		}
		TransformUtil.mergeJsonObjectPattern(keySet, boundKeySet, jsonObjectList);

		// 리소스 개별 생성 수행시작
		for(int eachRowCount = 0; jsonArray.size() > eachRowCount; eachRowCount++){
			JsonObject eachRowJsonObj = jsonArray.get(eachRowCount).getAsJsonObject();
			try {
				// 각 오브젝트별 동작 수행 시작
				JSONObject sourceObject = new JSONObject(eachRowJsonObj.toString());

				//  3) 리소스별 필요 레퍼런스 추가
				try {
					sourceObject = this.settingSourceWithReferenceSet(mapType, sourceObject);
				}catch(IllegalArgumentException | JSONException e){
					e.printStackTrace();

					for(String eachUpperResourceStr : transformDataOperationConfigProperties.getResourceUpperSortingReferenceSet()){
						if(eachUpperResourceStr.equals(sourceObject.getString("resourcetype"))){
							loggingInDebugMode("[ERR] 해당 래퍼런스 조회 과정에서 오류가 발생하였습니다. " + sourceObject);
							loggingInDebugMode("   ㄴ map type " + mapType);
							loggingInDebugMode("   ㄴ cause :  " + e.getMessage());
							throw new IllegalArgumentException("[ERR] 해당 리소스가 레퍼런스 조회과정에서 오류가 발생하였습니다.");
						}
					}

					loggingInDebugMode("[ERR] 해당 래퍼런스 조회 과정에서 오류가 발생하였습니다. " + sourceObject);
					loggingInDebugMode("   ㄴ map type " + mapType);
					loggingInDebugMode("   ㄴ cause :  " + e.getMessage());
					continue;
				}

				loggingInDebugMode("----");
				loggingInDebugMode(" > [DEV] Reference 추가된 Source Object : " + sourceObject.toString());
				loggingInDebugMode("----");

				// 2. 매핑 수행

				//  1) Resource 생성
				IBaseResource resource = transformEngine.transformDataToResource(ruleNodeList, sourceObject);

				// 3. 생성
				loggingInDebugMode("----");
				loggingInDebugMode(" > [DEV] Created THis Resource : " + this.getContext().newJsonParser().encodeResourceToString(resource));
				loggingInDebugMode("----");

				// 4. 리소스별 레퍼런스 등록
				try {
					boolean bindReferenceSuccessYn = this.bindReference(resource, sourceObject);
				}catch(IllegalArgumentException | JSONException e){
					loggingInDebugMode(" 래퍼런스 생성 과정에서 오류가 발생하였습니다. ");
				}

				retResourceList.add(resource);
			}catch (JSONException e){
				e.printStackTrace();
			}
		}


		return retResourceList;
	}
	 */

	/**
	 * 2023. 11. 28. 데이터를 생성하기 전 Reference 를 등록한다.
	 * Reference 는 Organization - Basement(Patient, Practitioner..) - Header(Encounter) - OTHERS 단위로 나뉜다.
	 * @param mapType      the map type
	 * @param sourceObject the source object
	 * @return the reference set
	 * @throws JSONException the json exception
	 */
	public JSONObject settingSourceWithReferenceSet(String mapType, JSONObject sourceObject) throws JSONException {
		try {
			loggingInDebugMode("[DEV] mapType : " + mapType);
			ResourceReferenceCode referenceCode = ResourceReferenceCode.searchResourceReferenceCodeWithContainResName(mapType);

			loggingInDebugMode("[DEV] referenceCode.getResourceName : " + referenceCode.getResourceName());
			loggingInDebugMode("[DEV] referenceCode.getBaseType : " + referenceCode.getBaseType());

			if (referenceCode.getBaseType().equals("Organization")) {
				// Organization 은 아무런 Reference 도 요구되지 않는다.
			} else if (referenceCode.getBaseType().equals("Basement")) {
				if (referenceCode.getResourceName().equals("Patient")) {
					// 키 조회
					LinkedHashSet<String> searchKeyPartSet = new LinkedHashSet<>();
					// TODO transformEngine.getResourceIdentifierSet("Organization"); 를 활용해서 사용자가 정의한 Map 기준대로 Key 조회해서 매핑하기
					searchKeyPartSet.add("inst_cd");
					String searchId = TransformUtil.createResourceId("Organization", searchKeyPartSet, sourceObject);
					String organizationId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get(searchId).getReference();
					sourceObject.put("Organization_id", organizationId);

				} else if (referenceCode.getResourceName().equals("PractitionerRole")) {
					LinkedHashSet<String> searchKeyPartSet = new LinkedHashSet<>();
					searchKeyPartSet.add("inst_cd");
					String searchId = TransformUtil.createResourceId("Organization", searchKeyPartSet, sourceObject);
					String organizationId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get(searchId).getReference();
					sourceObject.put("Organization_id", organizationId);

					String organizationOId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get(searchId + ".oid").getReference();
					sourceObject.put("Organization_oid", organizationOId);

					searchKeyPartSet.clear();
					searchKeyPartSet.add("Organization_id");
					searchKeyPartSet.add("ord_dr_id");
					String searchPractitionerId = TransformUtil.createResourceId("Practitioner", searchKeyPartSet, sourceObject);

					loggingInDebugMode("searchPractitionerId : " + searchPractitionerId);
					String practitionerId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get(searchPractitionerId).getReference();
					loggingInDebugMode("practitionerId : " + practitionerId);

					sourceObject.put("Practitioner_id", practitionerId);

				} else if (referenceCode.getResourceName().equals("Practitioner")) {
					LinkedHashSet<String> searchKeyPartSet = new LinkedHashSet<>();
					searchKeyPartSet.add("inst_cd");
					String searchId = TransformUtil.createResourceId("Organization", searchKeyPartSet, sourceObject);

					loggingInDebugMode("searchId : " + searchId);
					String organizationId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get(searchId).getReference();

					loggingInDebugMode("OrganizationId : " + organizationId);
					sourceObject.put("Organization_id", organizationId);

					String organizationOId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get(searchId + ".oid").getReference();
					sourceObject.put("Organization_oid", organizationOId);
				}
			} else if (referenceCode.getBaseType().equals("Header")) {
				// Header 기준으로 등록
				LinkedHashSet<String> searchKeyPartSet = new LinkedHashSet<>();
				searchKeyPartSet.add("inst_cd");
				String searchId = TransformUtil.createResourceId("Organization", searchKeyPartSet, sourceObject);
				String organizationId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get(searchId).getReference();
				sourceObject.put("Organization_id", organizationId);
				loggingInDebugMode(" >>> regist Organization_id : " + organizationId);

				String organizationOId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get(searchId + ".oid").getReference();
				loggingInDebugMode(" >>> regist Organization_oid : " + organizationOId);
				sourceObject.put("Organization_oid", organizationOId);

				searchKeyPartSet.clear();
				searchKeyPartSet.add("Organization_id");
				searchKeyPartSet.add("pid");
				searchId = TransformUtil.createResourceId("Patient", searchKeyPartSet, sourceObject);
				loggingInDebugMode("PAT SEARCH : " + searchId);
				String patientId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get(searchId).getReference();
				loggingInDebugMode("PAT SEARCHED : " + patientId);
				sourceObject.put("Patient_id", patientId);

				searchKeyPartSet.clear();
				searchKeyPartSet.add("Organization_id");
				searchKeyPartSet.add("ord_dr_id");
				searchId = TransformUtil.createResourceId("PractitionerRole", searchKeyPartSet, sourceObject);
				loggingInDebugMode("PractitionerRole_id SEARCH : " + searchId);
				String practitionerRoleId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get(searchId).getReference();
				sourceObject.put("PractitionerRole_id", practitionerRoleId);

				searchKeyPartSet.clear();
				searchKeyPartSet.add("Organization_id");
				searchKeyPartSet.add("ord_dr_id");
				searchId = TransformUtil.createResourceId("Practitioner", searchKeyPartSet, sourceObject);
				loggingInDebugMode("Practitioner_id SEARCH : " + searchId);
				String practitionerId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get(searchId).getReference();
				sourceObject.put("Practitioner_id", practitionerId);

			}else if(referenceCode.getBaseType().equals("PatientExtendData")){
				// 환자정보 / 기관정보 외 필요없는 케이스( ex)알러지 등
				LinkedHashSet<String> searchKeyPartSet = new LinkedHashSet<>();
				searchKeyPartSet.add("inst_cd");
				String searchId = TransformUtil.createResourceId("Organization", searchKeyPartSet, sourceObject);
				String organizationId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get(searchId).getReference();
				sourceObject.put("Organization_id", organizationId);
				loggingInDebugMode(" >>> regist Organization_id : " + organizationId);

				String organizationOId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get(searchId + ".oid").getReference();
				loggingInDebugMode(" >>> regist Organization_oid : " + organizationOId);
				sourceObject.put("Organization_oid", organizationOId);

				searchKeyPartSet.clear();
				searchKeyPartSet.add("Organization_id");
				searchKeyPartSet.add("pid");
				searchId = TransformUtil.createResourceId("Patient", searchKeyPartSet, sourceObject);
				loggingInDebugMode("PAT SEARCH : " + searchId);
				String patientId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get(searchId).getReference();
				loggingInDebugMode("PAT SEARCHED : " + patientId);
				sourceObject.put("Patient_id", patientId);

			}else if (referenceCode.getBaseType().equals("Others")){
				// 리소스 서치
				Map<String, String> identifierSet = new HashMap<>();
				// TODO. Header인 Encounter 리소스를 가져오는 과정에서 해당 리소스가 가진 키값의 정의를 
				// 키가 알아서 하게 구성하면 안되려나?
				identifierSet.put("inst_cd", sourceObject.getString("inst_cd"));
				identifierSet.put("pid", sourceObject.getString("pid"));
				identifierSet.put("ord_dd", sourceObject.getString("ord_dd"));
				identifierSet.put("ord_dept_cd", sourceObject.getString("ord_dept_cd"));
				try{
					identifierSet.put("ord_type_cd", sourceObject.getString("ord_type_cd"));
				}catch(org.json.JSONException e){
					loggingInDebugMode("  >> 해당 리소스는 ordTypeCD 가 존재하지 않아 해당 내용없이 인덱스를 조회합니다.");
				}
				identifierSet.put("ord_dr_id", sourceObject.getString("ord_dr_id"));
				identifierSet.put("cret_no", sourceObject.getString("cret_no"));
				ReferenceDataSet ds = referenceDataMatcher.searchMapperWithMapType(identifierSet);
				if (ds == null) {
					if (transformDataOperationConfigProperties.isTransforIgnoreHasNoEncounter()) {
						loggingInDebugMode(" > 해당 리소스의 Encounter 를 찾을 수 없어 해당 데이터는 생성이 생략되었습니다. " + identifierSet);
					} else {
						throw new IllegalArgumentException(" > 해당 리소스의 Encounter를 찾을 수 없어 오류가 발생하였습니다.");
					}
				}else{
					sourceObject.put("Organization_id", ds.getReferenceList().get("Organization_id").getReference());
					sourceObject.put("Organization_oid", ds.getReferenceList().get("Organization_oid").getReference());
					sourceObject.put("Patient_id", ds.getReferenceList().get("Patient_id").getReference());
					sourceObject.put("PractitionerRole_id", ds.getReferenceList().get("PractitionerRole_id").getReference());
					sourceObject.put("Practitioner_id", ds.getReferenceList().get("Practitioner_id").getReference());
					sourceObject.put("Encounter_id", ds.getReferenceList().get("Encounter_id").getReference());
				}
			}

			return sourceObject;
		}catch(NullPointerException e){
			e.printStackTrace();
			throw new IllegalArgumentException("해당 리소스가 존재하지 않습니다. ");
		}
	}

	/**
	 *  2023. 11. 28. Reference 를 Bean에 등록한다.
	 * @return the boolean
	 */
	public boolean bindReference(IBaseResource baseResource, JSONObject jsonObject) throws JSONException{
		loggingInDebugMode(" >>> BIND Reference Type : " + baseResource.fhirType());

		// basement
		if(baseResource.fhirType().equals("Organization")){
			Organization organization = (Organization) baseResource;
			// TODO. 여러병원의 정보를 하나의 Convert가 수행할때는 이 기준을 수정해야..
			LinkedHashSet<String> searchKeyPartSet = new LinkedHashSet<>();
			searchKeyPartSet.add("inst_cd");
			String searchId = TransformUtil.createResourceId("Organization", searchKeyPartSet, jsonObject);

			referenceDataMatcher.setReference("Standard-Ref", searchId, new Reference(organization.getId()));

			referenceDataMatcher.setReference("Standard-Ref", searchId + ".oid", new Reference(organization.getIdentifier().get(0).getValue()));
		}else if(baseResource.fhirType().equals("Patient")){
			Patient patient = (Patient) baseResource;
			referenceDataMatcher.setReference("Standard-Ref", patient.getIdPart() , new Reference(patient.getId()));

		}else if(baseResource.fhirType().equals("Practitioner")){
			Practitioner practitioner = (Practitioner) baseResource;
			referenceDataMatcher.setReference("Standard-Ref", practitioner.getId(), new Reference(practitioner.getId()));

		}else if(baseResource.fhirType().equals("PractitionerRole")){
			PractitionerRole practitionerRole = (PractitionerRole) baseResource;
			referenceDataMatcher.setReference("Standard-Ref", practitionerRole.getId(), new Reference(practitionerRole.getId()));

		}else if(baseResource.fhirType().equals("Encounter")){
			Encounter encounter = (Encounter) baseResource;
			LinkedHashMap<String, String> identifierSet = new LinkedHashMap<>();
			identifierSet.put("inst_cd", jsonObject.getString("inst_cd"));
			identifierSet.put("pid", jsonObject.getString("pid"));
			identifierSet.put("ord_dd", jsonObject.getString("ord_dd"));
			identifierSet.put("ord_dept_cd", jsonObject.getString("ord_dept_cd"));
			identifierSet.put("ord_type_cd", jsonObject.getString("ord_type_cd"));
			identifierSet.put("ord_dr_id", jsonObject.getString("ord_dr_id"));
			identifierSet.put("cret_no", jsonObject.getString("cret_no"));

			Map<String, Reference> encounterIncludedRefSet = new HashMap<>();
			encounterIncludedRefSet.put("Organization_id", new Reference(jsonObject.getString("Organization_id")));
			encounterIncludedRefSet.put("Organization_oid", new Reference(jsonObject.getString("Organization_oid")));
			encounterIncludedRefSet.put("Patient_id", new Reference(jsonObject.getString("Patient_id")));
			encounterIncludedRefSet.put("Practitioner_id", new Reference(jsonObject.getString("Practitioner_id")));
			encounterIncludedRefSet.put("PractitionerRole_id", new Reference(jsonObject.getString("PractitionerRole_id")));

			encounterIncludedRefSet.put("Encounter_id", new Reference(encounter.getId()));
			referenceDataMatcher.inputMappingData(encounter.getId(), identifierSet, encounterIncludedRefSet);
		}
		return true;
	}

	private void loggingInDebugMode(String arg){
		if(transformDataOperationConfigProperties.isTransforLogging()){
			ourLog.info(arg);
		}
	}
}

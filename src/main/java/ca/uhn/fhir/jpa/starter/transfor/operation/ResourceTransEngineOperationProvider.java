package ca.uhn.fhir.jpa.starter.transfor.operation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.provider.BaseJpaProvider;
import ca.uhn.fhir.jpa.starter.transfor.base.core.TransformEngine;
import ca.uhn.fhir.jpa.starter.transfor.code.ResourceReferenceCode;
import ca.uhn.fhir.jpa.starter.transfor.config.TransformDataOperationConfigProperties;
import ca.uhn.fhir.jpa.starter.transfor.dto.base.ReferenceDataMatcher;
import ca.uhn.fhir.jpa.starter.transfor.util.TransformUtil;
import ca.uhn.fhir.jpa.starter.validation.config.CustomValidationRemoteConfigProperties;
import ca.uhn.fhir.rest.annotation.Operation;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
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
 */
public class ResourceTransEngineOperationProvider extends BaseJpaProvider {

	private static final Logger ourLog = LoggerFactory.getLogger(ResourceTransEngineOperationProvider.class);

	ReferenceDataMatcher referenceDataMatcher = new ReferenceDataMatcher();

	private TransformEngine transformEngine;

	private TransformUtil transformUtil;

	private FhirContext fn;

	public ResourceTransEngineOperationProvider(FhirContext fn){
		this.fn = fn;
		transformEngine = new TransformEngine(customValidationRemoteConfigProperties);
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

			Queue<Map.Entry<String, JsonElement>> sortedQueue = transformUtil.sortingCreateResourceArgument(jsonObject);

			// 실질적인 변환 부분
			// 해당 영역부터 개별 대상자로 한정
			// 1. reference 구성
			Map<String, String> noSearchArg = new HashMap<>();
			noSearchArg.put("Don't Search Main Volumns", "9999999");
			referenceDataMatcher.inputMappingData("Standard-Ref", noSearchArg, new HashMap<>());

			// 2. 생성 시작
			while(sortedQueue.size() != 0){
				Map.Entry<String, JsonElement> entry = sortedQueue.poll();
				System.out.println(entry);
				this.createResource(entry);
			}

		}catch(Exception e){
			e.printStackTrace();
			retMessage = e.getMessage();
		}finally{
			theResponse.setContentType("text/plain");
			theResponse.getWriter().write(retMessage);
			theResponse.getWriter().close();
		}
	}

	private List<IBaseResource> createResource(Map.Entry<String, JsonElement> entry){
		List<IBaseResource> retResourceList = new ArrayList<>();
		JsonElement elements = entry.getValue();
		JsonArray jsonArray = elements.getAsJsonArray();

		for(int eachRowCount = 0; jsonArray.size() > eachRowCount; eachRowCount++){
			JsonObject eachRowJsonObj = jsonArray.get(eachRowCount).getAsJsonObject();
			try {
				// 각 오브젝트별 동작 수행 시작
				// Google Gson(Parsing Only) -> JSONObject(Handle operation)
				JSONObject sourceObject = new JSONObject(eachRowJsonObj.toString());

				// 1. 매핑 전 사전준비
				// 맵 조회
				String mapType = sourceObject.getString("mapType");
				String mapScript = "";
				if(mapType == null){
					ourLog.error("[ERR] 해당 리소스에 MapType이 없습니다.");
					throw new IllegalArgumentException("[ERR] 해당 리소스에 MapType이 없습니다.");
				}else{
					mapScript = TransformUtil.getMap(mapType);
				}

				// 리소스별 레퍼런스 추가
				// Practitioner, PractitionerRole, Organization 은 reference 가 Base에 기반한다
				ResourceReferenceCode referenceCode = ResourceReferenceCode.searchResourceReferenceCodeWithContainResName(mapType);
				if(referenceCode.getBaseType().equals("Basement")){
					// Basement 등록
					if(referenceCode.getResourceName().equals("Organization")){
						// Organization 은 어떠한 ref도 필요없음.
					}else if(referenceCode.getResourceName().equals("PractitionerRole")){
						// PractitionerRole 은 Practitioner 가 필요하다.
						String organizationId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get("Organization").getReference();
						sourceObject.put("Organization_id", organizationId);
						String practitionerId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get("PRCT." + organizationId + "." + sourceObject.get("ord_dr_id")).getReference();
						sourceObject.put("Practitioner_id", practitionerId);
					}else{
						String organizationId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get("Organization").getReference();
						sourceObject.put("Organization_id", organizationId);
					}
				}else if(referenceCode.getBaseType().equals("Header")){
					// Header 기준으로 등록
					String organizationId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get("Organization").getReference();
					String patientId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get("PAT." + organizationId + "." + sourceObject.get("pid")).getReference();
					String practitionerRoleId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get("PROL." + organizationId + "." + sourceObject.get("ord_dr_id")).getReference();
					String practitionerId = referenceDataMatcher.getMappingData().get("Standard-Ref").getReferenceList().get("PRCT." + organizationId + "." + sourceObject.get("ord_dr_id")).getReference();

				}else if(referenceCode.getBaseType().equals("Others")){
					// 리소스 서치
				}

				// Encounter는 reference가 Patient, PractitionerRole 등의 기본구조에서 파생한다

				// 기타 모든 리소스는 reference가 그 외 리소스에서 파생한다.

				// 2. 매핑 수행
				// Resource 생성
				IBaseResource resource = transformEngine.transformDataToResource(mapScript, sourceObject);

				// Resource 별 Reference 등록
				if(resource.fhirType().equals("Organization")){
					Organization organization = (Organization) resource;
					referenceDataMatcher.setReference("Standard-Ref", "Organization", new Reference(organization.getId()));
					referenceDataMatcher.setReference("Standard-Ref", "Organization-oid", new Reference(organization.getIdentifier().get(0).getValue()));
				}else if(resource.fhirType().equals("Patient")){
					Patient patient = (Patient) resource;
					referenceDataMatcher.setReference("Standard-Ref", "Patient" , new Reference(patient.getId()));
				}

				// 3. 생성

				retResourceList.add(resource);
			}catch (JSONException e){
			}
		}


		return retResourceList;
	}


}

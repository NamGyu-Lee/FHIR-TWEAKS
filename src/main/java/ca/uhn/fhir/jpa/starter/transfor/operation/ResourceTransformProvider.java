package ca.uhn.fhir.jpa.starter.transfor.operation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.provider.BaseJpaProvider;
import ca.uhn.fhir.jpa.starter.transfor.base.code.ErrorHandleType;
import ca.uhn.fhir.jpa.starter.transfor.base.code.RuleType;
import ca.uhn.fhir.jpa.starter.transfor.base.code.TransactionType;
import ca.uhn.fhir.jpa.starter.transfor.base.core.MetaEngine;
import ca.uhn.fhir.jpa.starter.transfor.base.core.TransformEngine;
import ca.uhn.fhir.jpa.starter.transfor.base.map.MetaRule;
import ca.uhn.fhir.jpa.starter.transfor.base.map.RuleNode;
import ca.uhn.fhir.jpa.starter.transfor.base.reference.structure.ReferenceCacheHandler;
import ca.uhn.fhir.jpa.starter.transfor.base.util.MapperUtils;
import ca.uhn.fhir.jpa.starter.transfor.config.TransformDataOperationConfigProperties;
import ca.uhn.fhir.jpa.starter.transfor.dto.comm.ResponseDto;
import ca.uhn.fhir.jpa.starter.transfor.operation.code.ResponseStateCode;
import ca.uhn.fhir.jpa.starter.transfor.util.TransformUtil;
import ca.uhn.fhir.jpa.starter.validation.config.CustomValidationRemoteConfigProperties;
import ca.uhn.fhir.rest.annotation.Operation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 *  2023. 12. 15. Resource의 Transform
 *   Reference, Data 기조로 구성을 나누어 재구성.
 *
 *  2024. 02. 28. array 처리를 위한 작업 시작. - ver 4.0
 *  ver 4.0
 */
public class ResourceTransformProvider extends BaseJpaProvider {
	private static final Logger ourLog = LoggerFactory.getLogger(ResourceTransformProvider.class);

	private TransformEngine transformEngine;

	private MetaEngine metaEngine;

	private TransformUtil transformUtil;

	private FhirContext fn;

	private ReferenceCacheHandler referenceCacheHandler;

	private TransformDataOperationConfigProperties transformDataOperationConfigProperties;

	@Autowired
	@Primary
	void setTransformDataOperationConfigProperties(TransformDataOperationConfigProperties transformDataOperationConfigProperties){
		this.transformDataOperationConfigProperties = transformDataOperationConfigProperties;
		transformUtil = new TransformUtil(transformDataOperationConfigProperties);
		referenceCacheHandler = new ReferenceCacheHandler();
		metaEngine = new MetaEngine(fn, transformDataOperationConfigProperties, referenceCacheHandler);
	}

	private CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties;

	@Autowired
	@Primary
	void setCustomValidationRemoteConfigProperties(CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties){
		this.customValidationRemoteConfigProperties = customValidationRemoteConfigProperties;
		transformEngine = new TransformEngine(this.getContext(), customValidationRemoteConfigProperties);
	}

	public ResourceTransformProvider(FhirContext fn){
		this.fn = fn;
	}

	@Operation(
		name="$tranform-resource-basic",
		idempotent = false,
		manualRequest = true,
		manualResponse = true
	)
	public void transforResourceStandardService(HttpServletRequest theServletRequest, HttpServletResponse theResponse) throws IOException {
		String retMessage = "-";
		ourLog.info(" > Create Engine, Reference Engine Based Data Transfor initalized.. ");

		try{
			byte[] bytes = IOUtils.toByteArray(theServletRequest.getInputStream());
			String bodyData = new String(bytes);
			JsonParser jsonParser = new JsonParser();
			JsonElement jsonElement = jsonParser.parse(bodyData);
			JsonObject jsonObject = jsonElement.getAsJsonObject();

			// 1. sorting
			Queue<Map.Entry<String, JsonElement>> sortedQueue = transformUtil.sortingCreateResourceArgument(jsonObject);

			// 2. 데이터 생성
			Bundle bundle = new Bundle();
			while(sortedQueue.size() != 0){
				Map.Entry<String, JsonElement> entry = sortedQueue.poll();
				List<IBaseResource> baseResourceList = this.createResource(entry);
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

		}catch(Exception e) {
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

	private List<IBaseResource> createResource(Map.Entry<String, JsonElement> entry){
		List<IBaseResource> retResourceList = new ArrayList<>();
		JsonElement elements = entry.getValue();
		JsonArray jsonArray = elements.getAsJsonArray();

		// 1. 리소스 병합 수행
		// 리소스 생성별 맵 구성
		// Resource : MapType 을 1:1로 고정
		JsonObject searchFirstSourceData = jsonArray.get(0).getAsJsonObject();
		String mapScript = "";
		String mapType = "";
		try {
			JSONObject sourceObject = new JSONObject(searchFirstSourceData.toString());
			mapType = sourceObject.getString("map_type");
			if(mapType.isEmpty() || mapType.isBlank()){
				ourLog.error("[ERR] 해당 리소스에 MapType이 없습니다.");
				throw new IllegalArgumentException("[ERR] 해당 리소스에 MapType이 없습니다.");
			}else{
				mapScript = TransformUtil.getMap(mapType);
			}

		}catch(JSONException e){
			ourLog.error("다음과같은 source JSON이 오류를 발생시켰습니다.");
			ourLog.error(searchFirstSourceData.toString());
			throw new IllegalArgumentException("[ERR] Source 데이터의 맵을 조회하는 시점에서 JSONException 오류가 발생하였습니다.");
		}

		// 2.1. metaRule 구성
		MetaRule metaRule = metaEngine.getMetaData(mapScript);
		Set<String> keySet = metaRule.getCacheDataKey();
		Set<String> mergeDataKeySet = metaRule.getMergeDataKey();

		// 2.2. metaRule 기반의 Source 데이터 Merge 준비
		List<JsonElement> jsonElementList = new ArrayList<>();
		for(int eachRowCount = 0; jsonArray.size() > eachRowCount; eachRowCount++){
			jsonElementList.add(jsonArray.get(eachRowCount));
		}

		// 2.3.2. Source 의 Merge 수행
		List<JsonObject> sourceDataJsonList = TransformUtil.mergeJsonObjectPattern(keySet, mergeDataKeySet, jsonElementList);

		// 2.4. 데이터 조회
		for(JsonObject eachRowJsonObj : sourceDataJsonList){
			try {
				// 각 오브젝트별 동작 수행 시작
				JSONObject sourceObject = new JSONObject(eachRowJsonObj.toString());
				try {
					 // 4.1. 매 회별 맵 구성
					List<RuleNode> ruleNodeList = transformEngine.createRuleNodeTree(mapScript);
					if(mapScript == "" || mapScript == null || mapType == "" || mapType == null){
						throw new IllegalArgumentException("[ERR] Map이 조회되지 않았습니다.");
					}

					// 병합 활용 맵 재생성
					for(int j = 0; ruleNodeList.size() > j; j++){
						ruleNodeList.set(j, MapperUtils.createTreeForArrayWithRecursive(metaRule, ruleNodeList.get(j), sourceObject));
					}

					// 캐시값 조회 후 추가
					metaEngine.setReference(metaRule, sourceObject);

					// 2.4.1. FHIR 데이터 생성
					IBaseResource resource = transformEngine.transformDataToResource(ruleNodeList, sourceObject);

					// 2.4.2. 로깅
					loggingInDebugMode(" > [DEV] Created THis Resource : " + this.getContext().newJsonParser().encodeResourceToString(resource));
					retResourceList.add(resource);

					// 2.4.3. 캐시 처리
					if(metaRule.getCacheDataKey().size() != 0){
						metaEngine.putCacheResource(metaRule, sourceObject, resource, null);
					}
				}catch(Exception e){
					e.printStackTrace();
					if(metaRule.getErrorHandleType().equals(ErrorHandleType.EXCEPTION)){
						throw new IllegalArgumentException("[ERR] 데이터 형변환 과정에서 오류가 발생하였습니다.");
					}else if(metaRule.getErrorHandleType().equals(ErrorHandleType.WARNING)){
						ourLog.warn("[WARN] 데이터 형변환 과정에서 오류가 발생하였습니다. " + e.getMessage());
					}else{
						loggingInDebugMode("[INFO] 데이터 형변환 과정에서 오류가 발생하였습니다. " + e.getMessage());
					}
				}
			}catch (JSONException e){
				e.printStackTrace();
			}
		}
		return retResourceList;
	}

	private void loggingInDebugMode(String arg){
		if(transformDataOperationConfigProperties.isTransforLogging()){
			ourLog.info(arg);
		}
	}
}
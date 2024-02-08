package ca.uhn.fhir.jpa.starter.transfor.operation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.provider.BaseJpaProvider;
import ca.uhn.fhir.jpa.starter.transfor.base.code.ErrorHandleType;
import ca.uhn.fhir.jpa.starter.transfor.base.core.MetaEngine;
import ca.uhn.fhir.jpa.starter.transfor.base.core.TransformEngine;
import ca.uhn.fhir.jpa.starter.transfor.base.map.MetaRule;
import ca.uhn.fhir.jpa.starter.transfor.base.reference.structure.ReferenceCacheHandler;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 *  2023. 12. 15. Resource의 Transform
 *   Reference, Data 기조로 구성을 나누어 재구성.
 *
 *
 *  ver 3.0
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
	void setTransformDataOperationConfigProperties(TransformDataOperationConfigProperties transformDataOperationConfigProperties){
		this.transformDataOperationConfigProperties = transformDataOperationConfigProperties;
		transformUtil = new TransformUtil(transformDataOperationConfigProperties);
	}

	private CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties;
	@Autowired
	void setCustomValidationRemoteConfigProperties(CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties){
		this.customValidationRemoteConfigProperties = customValidationRemoteConfigProperties;
	}

	public ResourceTransformProvider(FhirContext fn){
		this.fn = fn;
		referenceCacheHandler = new ReferenceCacheHandler();
		metaEngine = new MetaEngine(fn, transformDataOperationConfigProperties, referenceCacheHandler);
		transformEngine = new TransformEngine(customValidationRemoteConfigProperties);
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
		for(int eachRowCount = 0; jsonArray.size() > eachRowCount; eachRowCount++){
			JsonObject eachRowJsonObj = jsonArray.get(eachRowCount).getAsJsonObject();
			try {
				// 각 오브젝트별 동작 수행 시작
				JSONObject sourceObject = new JSONObject(eachRowJsonObj.toString());

				// 1. 매핑 전 사전준비
				//  1) 맵 조회
				String mapType = sourceObject.getString("maptype");
				String mapScript = "";
				if(mapType == null){
					ourLog.error("[ERR] 해당 리소스에 MapType이 없습니다.");
					continue; // 테스트용.
					//throw new IllegalArgumentException("[ERR] 해당 리소스에 MapType이 없습니다.");
				}else{
					mapScript = TransformUtil.getMap(mapType);
				}

				MetaRule metaRule = metaEngine.getMetaData(mapScript);
				try {
					// 2. 맵단위 메타데이터 조회

					// 3. 리소스별 필요 레퍼런스 추가
					metaEngine.setReference(metaRule, sourceObject);

					// 4. FHIR 데이터 생성
					IBaseResource resource = transformEngine.transformDataToResource(mapScript, sourceObject);

					// 5. 로깅
					loggingInDebugMode(" > [DEV] Created THis Resource : " + this.getContext().newJsonParser().encodeResourceToString(resource));
					retResourceList.add(resource);

					// 6. 캐시 처리
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
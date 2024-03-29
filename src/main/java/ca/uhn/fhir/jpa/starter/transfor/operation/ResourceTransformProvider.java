package ca.uhn.fhir.jpa.starter.transfor.operation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.provider.BaseJpaProvider;
import ca.uhn.fhir.jpa.starter.transfor.base.core.MetaEngine;
import ca.uhn.fhir.jpa.starter.transfor.base.core.TransformEngine;
import ca.uhn.fhir.jpa.starter.transfor.base.core.ValidationEngine;
import ca.uhn.fhir.jpa.starter.transfor.base.map.MetaRule;
import ca.uhn.fhir.jpa.starter.transfor.base.reference.structure.ReferenceCacheHandler;
import ca.uhn.fhir.jpa.starter.transfor.config.TransformDataOperationConfigProperties;
import ca.uhn.fhir.jpa.starter.transfor.dto.comm.ResponseDto;
import ca.uhn.fhir.jpa.starter.transfor.operation.code.ResponseStateCode;
import ca.uhn.fhir.jpa.starter.transfor.service.ResourceTransformTask;
import ca.uhn.fhir.jpa.starter.transfor.util.TransformUtil;
import ca.uhn.fhir.jpa.starter.util.PerformanceChecker;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

	private ValidationEngine validationEngine;

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

	// dev. 동작시간 측정용으로 함수를 구성한다.
	PerformanceChecker timer;

	private CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties;

	@Autowired
	@Primary
	void setCustomValidationRemoteConfigProperties(CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties){
		this.customValidationRemoteConfigProperties = customValidationRemoteConfigProperties;
		transformEngine = new TransformEngine(this.getContext(), customValidationRemoteConfigProperties);
		validationEngine = new ValidationEngine(this.getContext(), customValidationRemoteConfigProperties);
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
		// debuging
		timer = new PerformanceChecker(transformDataOperationConfigProperties.isDebugPerformanceTrackingTimeEach(), transformDataOperationConfigProperties.isDebugPerformancePrintOperationTimeStack());
		try{
			byte[] bytes = IOUtils.toByteArray(theServletRequest.getInputStream());
			String bodyData = new String(bytes);
			JsonParser jsonParser = new JsonParser();
			JsonElement jsonElement = jsonParser.parse(bodyData);
			JsonObject jsonObject = jsonElement.getAsJsonObject();

			// 1. Sorting
			Queue<Map.Entry<String, JsonElement>> sortedQueue = transformUtil.sortingCreateResourceArgument(jsonObject);

			// 2. 데이터 생성
			Bundle bundle = new Bundle();
			int createResourceCount = 0;
			while(sortedQueue.size() != 0){
				Map.Entry<String, JsonElement> entry = sortedQueue.poll();
				if(entry.getValue().getAsJsonArray().size() <= 0) {
					ourLog.info(" -- 대상자의 " + entry.getKey() + " 데이터가 존재하지 않아 생략됩니다.");
				}else{
					List<IBaseResource> baseResourceList;
					if(transformDataOperationConfigProperties.isThreadEnabled()){
						baseResourceList = this.createResourceMultiThread(entry);
					}else{
						baseResourceList = this.createResourceSingleThread(entry);
					}

					createResourceCount = createResourceCount + baseResourceList.size();
					for (IBaseResource resource : baseResourceList) {
						if(customValidationRemoteConfigProperties.isValidationYn()){
							boolean isSuccessValidation = validationEngine.executeValidation(resource, false);
							if(isSuccessValidation){
								// validation 예외처리.
							}
						}
						Bundle.BundleEntryComponent comp = new Bundle.BundleEntryComponent();
						comp.setResource((Resource) resource);
						bundle.addEntry(comp);
					}
				}
			}

			// 3. 결과 리턴
			String retBundle = fn.newJsonParser().encodeResourceToString(bundle);
			ResponseDto<String> responseDto = ResponseDto.<String>builder().success(ResponseStateCode.OK.getSuccess()).stateCode(ResponseStateCode.OK.getStateCode()).errorReason("-").body(retBundle).createCount(createResourceCount).build();
			ObjectMapper mapper = new ObjectMapper();
			String jsonStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseDto);

			retMessage = jsonStr;
			timer.printAllTimeStack();
			timer.exportStackToExcel("C:\\TransformStandard-Test-Data" + this.getCurrentDateTime() + ".xlsx");

		}catch(Exception e) {
			e.printStackTrace();
			ResponseDto<String> responseDto = ResponseDto.<String>builder().success(ResponseStateCode.BAD_REQUEST.getSuccess()).stateCode(ResponseStateCode.BAD_REQUEST.getStateCode()).errorReason("-").body("-").createCount(0).build();
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
	private List<IBaseResource> createResourceMultiThread(Map.Entry<String, JsonElement> entry) throws Exception{
		List<IBaseResource> retResourceList = new ArrayList<>();
		JsonElement elements = entry.getValue();
		JsonArray jsonArray = elements.getAsJsonArray();

		// 1. 리소스 병합 수행
		// 리소스 생성별 맵 구성
		// Resource : MapType 을 1:1로 고정
		timer.startTimer();
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
				mapScript = TransformUtil.getMap(mapType, transformDataOperationConfigProperties.getTransformMapLocation());
			}

		}catch(JSONException e){
			ourLog.error("다음과같은 source JSON이 오류를 발생시켰습니다.");
			ourLog.error(searchFirstSourceData.toString());
			throw new IllegalArgumentException("[ERR] Source 데이터의 맵을 조회하는 시점에서 JSONException 오류가 발생하였습니다.");
		}
		timer.endTimer("1. 맵 조회");

		// 2.1. metaRule 구성
		timer.startTimer();

		MetaRule metaRule = metaEngine.getMetaData(mapScript);
		Set<String> keySet = metaRule.getCacheDataKey();
		Set<String> mergeDataKeySet = metaRule.getMergeDataKey();

		// 2.2. metaRule 기반의 Source 데이터 Merge 준비
		List<JsonElement> jsonElementList = new ArrayList<>();
		for(int eachRowCount = 0; jsonArray.size() > eachRowCount; eachRowCount++){
			jsonElementList.add(jsonArray.get(eachRowCount));
		}
		timer.endTimer("2. META 구성");

		// 2.3.2. Source 의 Merge 수행
		timer.startTimer();
		List<JsonObject> sourceDataJsonList = TransformUtil.mergeJsonObjectPattern(keySet, mergeDataKeySet, jsonElementList, transformDataOperationConfigProperties.isTransforMergeAllWithNoInsertMergeRule());
		timer.endTimer("3. MERGE 구성");

		// 2.4. 데이터 생성
		// 2024. 03. 21. test. thread 처리
		ExecutorService executor = Executors.newFixedThreadPool(transformDataOperationConfigProperties.getThreadPoolSize());
		List<Future<IBaseResource>> futures = new ArrayList<>();

		for(JsonObject eachRowJsonObj : sourceDataJsonList){
			ResourceTransformTask task = new ResourceTransformTask(mapScript, mapType, eachRowJsonObj, metaRule, transformEngine, metaEngine, timer);
			Future<IBaseResource> resourceFuture = executor.submit(task);
			futures.add(resourceFuture);
		}

		for(Future<IBaseResource> future : futures){
			try {
				// wait callback
				retResourceList.add(future.get());
			}catch(ExecutionException | InterruptedException e){
				throw new IllegalArgumentException("[ERR] Threading 처리과정에서 오류가 발생하였습니다.");
			}
		}
		executor.shutdown();
		return retResourceList;
	}

	private List<IBaseResource> createResourceSingleThread(Map.Entry<String, JsonElement> entry){
		List<IBaseResource> retResourceList = new ArrayList<>();
		JsonElement elements = entry.getValue();
		JsonArray jsonArray = elements.getAsJsonArray();

		// 1. 리소스 병합 수행
		// 리소스 생성별 맵 구성
		// Resource : MapType 을 1:1로 고정
		timer.startTimer();
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
				mapScript = TransformUtil.getMap(mapType, transformDataOperationConfigProperties.getTransformMapLocation());
			}

		}catch(JSONException e){
			ourLog.error("다음과같은 source JSON이 오류를 발생시켰습니다.");
			ourLog.error(searchFirstSourceData.toString());
			throw new IllegalArgumentException("[ERR] Source 데이터의 맵을 조회하는 시점에서 JSONException 오류가 발생하였습니다.");
		}
		timer.endTimer("1. 맵 조회");

		// 2.1. metaRule 구성
		timer.startTimer();

		MetaRule metaRule = metaEngine.getMetaData(mapScript);
		Set<String> keySet = metaRule.getCacheDataKey();
		Set<String> mergeDataKeySet = metaRule.getMergeDataKey();

		// 2.2. metaRule 기반의 Source 데이터 Merge 준비
		List<JsonElement> jsonElementList = new ArrayList<>();
		for(int eachRowCount = 0; jsonArray.size() > eachRowCount; eachRowCount++){
			jsonElementList.add(jsonArray.get(eachRowCount));
		}
		timer.endTimer("2. META 구성");

		// 2.3.2. Source 의 Merge 수행
		timer.startTimer();
		List<JsonObject> sourceDataJsonList = TransformUtil.mergeJsonObjectPattern(keySet, mergeDataKeySet, jsonElementList, transformDataOperationConfigProperties.isTransforMergeAllWithNoInsertMergeRule());
		timer.endTimer("3. MERGE 구성");

		// 2.4. 데이터 생성
		for(JsonObject eachRowJsonObj : sourceDataJsonList){
			ResourceTransformTask task = new ResourceTransformTask(mapScript, mapType, eachRowJsonObj, metaRule, transformEngine, metaEngine, timer);
			retResourceList.add(task.transformResourceEach());
		}
		return retResourceList;
	}

	private void loggingInDebugMode(String arg){
		if(transformDataOperationConfigProperties.isTransforLogging()){
			ourLog.info(arg);
		}
	}

	public static String getCurrentDateTime() {
		SimpleDateFormat formatter = new SimpleDateFormat("yyMMddHHmmss");
		Date now = new Date();
		return formatter.format(now);
	}
}
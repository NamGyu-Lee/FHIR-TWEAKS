package ca.uhn.fhir.jpa.starter.transfor.service;

import ca.uhn.fhir.jpa.starter.transfor.base.code.ErrorHandleType;
import ca.uhn.fhir.jpa.starter.transfor.base.core.MetaEngine;
import ca.uhn.fhir.jpa.starter.transfor.base.core.TransformEngine;
import ca.uhn.fhir.jpa.starter.transfor.base.map.MetaRule;
import ca.uhn.fhir.jpa.starter.transfor.base.map.RuleNode;
import ca.uhn.fhir.jpa.starter.transfor.base.util.MapperUtils;
import ca.uhn.fhir.jpa.starter.util.PerformanceChecker;
import com.google.gson.JsonObject;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.Callable;

public class ResourceTransformTask implements Callable<IBaseResource>{
	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(TransformEngine.class);

	private JsonObject eachRowJsonObj;

	private String mapScript;

	private String mapType;

	private TransformEngine transformEngine;

	private MetaEngine metaEngine;

	private MetaRule metaRule;

	private PerformanceChecker timer;

	public ResourceTransformTask(String mapScript, String mapType, JsonObject eachRowJsonObj, MetaRule metaRule,
	  TransformEngine transformEngine, MetaEngine metaEngine,  PerformanceChecker timer
	) {
		// per
		this.transformEngine = transformEngine;
		this.metaEngine = metaEngine;
		this.timer = timer;

		// each
		this.metaRule = metaRule;
		this.eachRowJsonObj = eachRowJsonObj;
		this.mapScript = mapScript;
		this.mapType = mapType;
	}

	public IBaseResource transformResourceEach(){
		try {
			// 각 오브젝트별 동작 수행 시작
			JSONObject sourceObject = new JSONObject(eachRowJsonObj.toString());
			try {
				// 4.1. 매 회별 맵 구성
				timer.startTimer();
				List<RuleNode> ruleNodeList = transformEngine.createRuleNodeTree(mapScript);
				if(mapScript == "" || mapScript == null || mapType == "" || mapType == null){
					throw new IllegalArgumentException("[ERR] Map이 조회되지 않았습니다.");
				}
				timer.endTimer("3. Resource 요청별 맵 구성");

				timer.startTimer();
				// 병합 활용 맵 재생성
				for(int j = 0; ruleNodeList.size() > j; j++){
					ruleNodeList.set(j, MapperUtils.createTreeForArrayWithRecursive(metaRule, ruleNodeList.get(j), sourceObject));
				}
				timer.endTimer("4. Resource 요청별 맵 재생성");

				// 캐시값 조회 후 추가
				timer.startTimer();
				synchronized (this) {
					metaEngine.setReference(metaRule, sourceObject);
				}
				timer.endTimer("5. Resource 요청별 Cache 조회");

				// 2.4.1. FHIR 데이터 생성
				timer.startTimer();

				IBaseResource resource = transformEngine.transformDataToResource(ruleNodeList, sourceObject);
				timer.endTimer("6. Resource 요청별 FHIR 생성");

				// 2.4.3. 캐시 처리
				timer.startTimer();
				if(metaRule.getCacheDataKey().size() != 0){
					synchronized (this) {
						metaEngine.putCacheResource(metaRule, sourceObject, resource, null);
					}
				}
				timer.endTimer("7. Resource 요청별 FHIR 생성");

				return resource;

			}catch(Exception e){
				e.printStackTrace();
				if(metaRule.getErrorHandleType().equals(ErrorHandleType.EXCEPTION)){
					throw new IllegalArgumentException("[ERR]" + e.getMessage());
				}else if(metaRule.getErrorHandleType().equals(ErrorHandleType.WARNING)){
					ourLog.warn("[WARN] 데이터 형변환 과정에서 오류가 발생하였습니다. " + e.getMessage());
				}
			}
		}catch(JSONException e){
			e.printStackTrace();
			throw new IllegalArgumentException("[ERR] FHIR 데이터 변환 과정에서 오류가 발생하였습니다.");
		}
		throw new IllegalArgumentException("[ERR] FHIR 데이터 변환 과정에서 오류가 발생하였습니다.");
	}

	@Override
	public IBaseResource call() throws Exception {
		return transformResourceEach();
	}
}

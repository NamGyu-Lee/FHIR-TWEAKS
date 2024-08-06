package ca.uhn.fhir.jpa.starter.transfor.base.core;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.starter.transfor.base.code.ErrorHandleType;
import ca.uhn.fhir.jpa.starter.transfor.base.map.MetaRule;
import ca.uhn.fhir.jpa.starter.transfor.base.map.ReferenceNode;
import ca.uhn.fhir.jpa.starter.transfor.base.map.ReferenceParamNode;
import ca.uhn.fhir.jpa.starter.transfor.base.reference.structure.ReferenceCache;
import ca.uhn.fhir.jpa.starter.transfor.base.reference.structure.ReferenceCacheHandler;
import ca.uhn.fhir.jpa.starter.transfor.base.reference.structure.ReferenceDataMatcher;
import ca.uhn.fhir.jpa.starter.transfor.base.util.MapperUtils;
import ca.uhn.fhir.jpa.starter.transfor.config.TransformDataOperationConfigProperties;
import ca.uhn.fhir.rest.param.ReferenceParam;
import io.micrometer.common.lang.Nullable;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Reference;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * 2023. 12. 18. Reference 을 정의해주는 엔진을 정의한다.
 */
public class MetaEngine {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(MetaEngine.class);

	private TransformDataOperationConfigProperties transformDataOperationConfigProperties;

	private FhirContext fhirContext;

	private ReferenceCacheHandler referenceCacheHandler;

	public MetaEngine(FhirContext fhirContext, TransformDataOperationConfigProperties transformDataOperationConfigProperties, ReferenceCacheHandler referenceCacheHandler){
		this.fhirContext = fhirContext;
		this.transformDataOperationConfigProperties = transformDataOperationConfigProperties;
		this.referenceCacheHandler = referenceCacheHandler;
	}

	public MetaRule getMetaData(String mapScript) throws IllegalArgumentException {
		String referenceMapStr = MapperUtils.getSeparateMapScript(mapScript, "meta");
		ourLog.info("referenceMapStr : " + referenceMapStr);

		// Meta데이터 구성
		MetaRule metaRule = MapperUtils.createMetaRule(referenceMapStr);
		return metaRule;
	}

	/* 2023. 12. 18. 소스의 데이터에 레퍼런스들을 사용자 정의에 맞게 작성한다. */
	public synchronized void setReference(MetaRule metaRule, JSONObject source) throws IllegalArgumentException{
		ErrorHandleType metaErrorPolicyType = metaRule.getErrorHandleType();
		try {
			List<ReferenceNode> referenceNodeList = metaRule.getReferenceNodeList();
			for(ReferenceNode refnode : referenceNodeList){
				ErrorHandleType referenceErrorPolicyType = refnode.getErrorHandleType();
				try {
					// 1. 캐시에서 조회
					if(!transformDataOperationConfigProperties.isTransformCacheEnabled()){
						ourLog.info("해당 서버 설정상 Cache 를 활용하지 않는 상태입니다.");
						return;
					}
					ReferenceCache searchedCacheResource = this.findCachedResourceFromReferenceNode(refnode, source);
					if(searchedCacheResource == null){
						if(transformDataOperationConfigProperties.isSearchReferenceinRepoEnabled()){
								// 2. FHIR Resource에서 조회
								String exceptCacheKeyList = "";
								for(ReferenceParamNode eachParamNode : refnode.getReferenceParamNodeList()){
									exceptCacheKeyList =  exceptCacheKeyList + " " + eachParamNode.getCacheTargetStr();
								}
								throw new IllegalArgumentException("[ERR] 해당 리소스의 레퍼런스가 캐시/Repo 내 조회되지 않아 오류가 발생하였습니다. "  + refnode.getTargetResource() + " 의 대하여 " + exceptCacheKeyList);
								// 적재형인 경우 활용
								// this.findResourceInRepo(refnode, source);
						}else{
							if(referenceErrorPolicyType.equals(ErrorHandleType.EXCEPTION)){
								throw new IllegalArgumentException("[ERR] 해당 리소스의 레퍼런스가 캐시/Repo 내 조회되지 않아 오류가 발생하였습니다. "  + refnode.getTargetResource());
							}else if(referenceErrorPolicyType.equals(ErrorHandleType.WARNING)){
								ourLog.warn("[WARN] 해당 리소스의 레퍼런스가 캐시/Repo 내 조회되지 않습니다. " + refnode.getTargetResource());
							}else{
								ourLog.info("[INFO] 해당 리소스의 레퍼런스가 조회되지 않은 상태입니다." + refnode.getTargetResource());
							}
							continue;
						}
					}else{
						// ex Organization_id
						ourLog.info("조회된 Cache ... " + refnode.getTargetResource() + " resource 의 " + searchedCacheResource.getResource().getIdElement().getIdPart());
						source.put(refnode.getTargetResource()+"_id", searchedCacheResource.getResource().getIdElement().getIdPart());
					}

				}catch(Exception e){ // TODO. 광범위한 에러 체커
					e.printStackTrace();
					if("exception".equals(metaErrorPolicyType.getStatus())){
						throw new IllegalArgumentException("[ERR] Reference 를 생성하는 과정에서 오류가 발생하였습니다. " + e.getMessage());
					}else if("warning".equals(metaErrorPolicyType.getStatus())){
						ourLog.warn("[WARN] Reference 생성 과정에서 오류가 발생하였습니다. " + e.getMessage());
						continue;
					}else if("ignore".equals(metaErrorPolicyType.getStatus())){
						ourLog.info("[WARN] Reference 생성 과정에서 오류가 발생하였습니다. " + e.getMessage());
						continue;
					}
				}
			}

		}catch(Exception e){ // TODO. 광범위한 에러 체커
			e.printStackTrace();
			if("exception".equals(metaErrorPolicyType.getStatus())){
				throw new IllegalArgumentException("[ERR] Reference 를 생성하는 과정에서 오류가 발생하였습니다. " + e.getMessage());
			}else if("warning".equals(metaErrorPolicyType.getStatus())){
				ourLog.warn("[WARM] Reference 생성 과정에서 오류가 발생하였습니다. " + e.getMessage());
			}else if("ignore".equals(metaErrorPolicyType.getStatus())){
				ourLog.info("[WARM] Reference 생성 과정에서 오류가 발생하였습니다. " + e.getMessage());
			}
		}
	}

	public ReferenceCache findCachedResourceFromReferenceNode(ReferenceNode referenceNode, JSONObject source) throws IllegalArgumentException{
		// 1. reference 에 데이터 읽기
		List<ReferenceParamNode> paramNodes = referenceNode.getReferenceParamNodeList();

		// 2. reference param 데이터를 활용하여 캐시 조회
		// target : source_data
		try {
			Map<String, String> requestConditionMap = new HashMap<>();
			for (ReferenceParamNode node : paramNodes) {
				requestConditionMap.put(node.getCacheTargetStr(), source.getString(node.getSourceStr()));
			}

		// 3. 데이터 조회
		return referenceCacheHandler.searchCache(referenceNode.getTargetResource(), requestConditionMap);

		}catch(JSONException e){
			throw new IllegalArgumentException("[ERR] 캐시에서 데이터를 가져오려고 조회셋을 만드는 과정에서 오류가 발생하였습니다. " + e.getMessage() );
		}
	}
   public ReferenceCache findResourceInRepo(ReferenceNode referenceNode, JSONObject source) throws IllegalArgumentException{
		// 1. reference 에 데이터 읽기
		List<ReferenceParamNode> paramNodes = referenceNode.getReferenceParamNodeList();
		try{
			// 2. 데이터 조회 조건 정의
			Set<String> searchKey = new HashSet<>();
			for(ReferenceParamNode eachParamnode : paramNodes){
				if("-".equals(eachParamnode.getFhirTargetStr())){
					// 해당 키는 활용하지 않음
				}else{
					searchKey.add(eachParamnode.getFhirTargetStr());
				}
			}

			// 3. FHIR 데이터 조회
			// 미구현
			// 4. cache 에 조회한 FHIR 데이터 넣기
			// 미구현
		}catch(IllegalArgumentException e){

		}
		return null;
	}

	public synchronized boolean putCacheResource(MetaRule metaRule, JSONObject source, @Nullable IBaseResource createdResource, @Nullable Reference createdReference) throws IllegalArgumentException{
		try {
			ourLog.info("---- Meta정보중 Cache Resource 를 적재합니다. ----");
			if(!transformDataOperationConfigProperties.isTransformCacheEnabled()){
				ourLog.info("해당 서버 설정상 Cache 를 활용하지 않는 상태입니다.");
				return true;
			}
			ourLog.debug("현재 적재된 크기 : " + metaRule.getCacheDataKey().size());

			// 1. Cache를 위한 고유 키 식별자 조회 및 정의
			Map<String, String> bindingKeyMap = new HashMap<>();
			for (String eachKey : metaRule.getCacheDataKey()) {
				bindingKeyMap.put(eachKey, source.getString(eachKey));
			}

			// 2. 이전 생성여부 확인
			if(referenceCacheHandler.searchCache(createdResource.fhirType(), bindingKeyMap) != null){
				ourLog.info("해당 데이터는 이미 존재하여 적재하지 않습니다.");
				ourLog.debug("    ㄴ 현재 적재된 리소스 종류 : ");
				for(String ref : referenceCacheHandler.getCacheMap().keySet()){
					ourLog.debug("          > :" + ref);
				}
				//throw new IllegalArgumentException("[ERR] 데이터를 저장하는 과정에서 중복된 데이터가 조회되어 오류가 발생하였습니다. ");
			}else{
				// 3. 생성
				ourLog.info("해당 데이터를 적재하는데에 성공하였습니다.  종류 : " + createdResource.fhirType() + " , 적재 데이터 : " + bindingKeyMap + ", reference : " + createdReference);
				ReferenceCache cache = new ReferenceCache();
				cache.setResource(createdResource);
				cache.setReference(createdReference);
				cache.setKeyMap(bindingKeyMap);
				referenceCacheHandler.putCache(createdResource.fhirType(), cache);

				ourLog.debug("    ㄴ 현재 적재된 리소스 종류 : ");
				for(String ref : referenceCacheHandler.getCacheMap().keySet()){
					ourLog.debug("          > :" + ref);
				}
			}
			ourLog.info("-------------------------------");
			return true;

		}catch(Exception e){
			e.printStackTrace();
			throw new IllegalArgumentException("[ERR] 캐시에서 데이터를 가져오려고 조회셋을 만드는 과정에서 오류가 발생하였습니다. " + e.getMessage() );
		}
	}
}

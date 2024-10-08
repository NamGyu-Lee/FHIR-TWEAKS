package ca.uhn.fhir.jpa.starter.transfor.base.core;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.starter.transfor.base.code.RuleType;
import ca.uhn.fhir.jpa.starter.transfor.base.code.TransactionType;
import ca.uhn.fhir.jpa.starter.transfor.base.map.ActivateTransNode;
import ca.uhn.fhir.jpa.starter.transfor.base.map.RuleNode;
import ca.uhn.fhir.jpa.starter.transfor.base.util.MapperUtils;
import ca.uhn.fhir.jpa.starter.transfor.base.util.RuleUtils;
import ca.uhn.fhir.jpa.starter.transfor.code.ResourceNameSummaryCode;
import ca.uhn.fhir.jpa.starter.transfor.util.TransformUtil;
import ca.uhn.fhir.jpa.starter.validation.config.CustomValidationRemoteConfigProperties;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 2023. 11. 20.
 * 특정 언어구조를 활용하여 데이터를 치환하는 로직을 구현한다.
 */
public class TransformEngine{

	TranslationEngine translationEngine;

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(TransformEngine.class);

	public TransformEngine(FhirContext context, CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties){
		translationEngine = new TranslationEngine(context, customValidationRemoteConfigProperties);
	}

	// 룰 실행부분
	public JSONObject executeRule(RuleNode ruleNode, JSONObject source) throws JSONException {
		//  1. 룰 동작
		JSONObject target = new JSONObject();
		if (ruleNode.getRuleType().equals(RuleType.CREATE)){
			if(ruleNode.getTransactionType().equals(TransactionType.CREATE)){
				String targetText = ruleNode.getRule();
				target.putOpt(targetText, new JSONObject());
			}else if(ruleNode.getTransactionType().equals(TransactionType.CREATE_SINGLESTRING)){
				target.put(ruleNode.getRule(), ruleNode.getRule().replaceAll("'", ""));
				return target;
			}
		} else if (ruleNode.getRuleType().equals(RuleType.TRANS)) {
			if(ruleNode.getTransactionType().equals(TransactionType.COPY)){
				if(source.get(ruleNode.getSourceReferenceNm()) == null){
					// source에 데이터가 없는 경우.
					throw new IllegalArgumentException("소스의 데이터에 " + ruleNode.getSourceReferenceNm() + " 가 없습니다.");
				}else{
					target.put(ruleNode.getTargetElementNm(), source.get(ruleNode.getSourceReferenceNm().trim()));
				}
			}else if(ruleNode.getTransactionType().equals(TransactionType.COPY_STRING)){
				target.put(ruleNode.getTargetElementNm(), ruleNode.getSourceReferenceNm().replaceAll("'", ""));
			}else if(ruleNode.getTransactionType().equals(TransactionType.TRANSLATION)){
				List<String> argumentParam = MapperUtils.extractMultipleValues(ruleNode.getSourceReferenceNm());
				if(source.get(argumentParam.get(0)) == null){
					// source에 데이터가 없는 경우.
					throw new IllegalArgumentException("소스의 데이터에 " + argumentParam.get(0) + " 가 없습니다.");
				}else{
					String referenceValue = (String) source.get(argumentParam.get(0).trim());
					String exchangedCodeValue = translationEngine.translateCode(referenceValue, argumentParam.get(1), argumentParam.get(2), argumentParam.get(3));
					target.put(ruleNode.getTargetElementNm(), exchangedCodeValue);
				}
			}else if(ruleNode.getTransactionType().equals(TransactionType.COPY_WITH_DEFAULT)){
				List<String> argumentParam = MapperUtils.extractMultipleValues(ruleNode.getSourceReferenceNm());
				boolean isNullData;
				try{
					if(source.get(argumentParam.get(0)) == null || source.get(argumentParam.get(0)) == ""){
						isNullData = true;
					}else{
						isNullData = false;
					}
				}catch(JSONException e){
					isNullData = true;
				}

				if(isNullData){
					if(argumentParam.get(1).contains("'")){
						target.put(ruleNode.getTargetElementNm(), argumentParam.get(1).replaceAll("'", ""));
					}else{
						target.put(ruleNode.getTargetElementNm(), source.get(argumentParam.get(1)));
					}
				}else{
					target.put(ruleNode.getTargetElementNm(), source.get(argumentParam.get(0)));
				}
			}else if(ruleNode.getTransactionType().equals(TransactionType.SPLIT)){
				List<String> argumentParam = MapperUtils.extractMultipleValues(ruleNode.getSourceReferenceNm());
				if(argumentParam.size() == 3){
					target.put(ruleNode.getTargetElementNm(), ((String)source.get(argumentParam.get(0))).substring(Integer.parseInt(argumentParam.get(1)), Integer.parseInt(argumentParam.get(2))));
				}else if(argumentParam.size() == 2){
					target.put(ruleNode.getTargetElementNm(), ((String)source.get(argumentParam.get(0))).substring(Integer.parseInt(argumentParam.get(1))));
				}else{
					ourLog.error("Execute Rule 중에 예상치 못한 함수가 들어왔습니다. Split은 반드시 파라미터가 2개 혹은 3개여야합니다.");
					ourLog.error("  ㄴ Rule : " + ruleNode.getSourceReferenceNm());
					ourLog.error("  ㄴ 파라미터 크기 : " + argumentParam.size());
					throw new JSONException("Execute Rule 중에 예상치 못한 함수가 들어왔습니다. Split은 반드시 파라미터가 2개 혹은 3개여야합니다.");
				}
			}else if(ruleNode.getTransactionType().equals(TransactionType.MERGE)){
				List<String> argumentParam = MapperUtils.extractMultipleValues(ruleNode.getSourceReferenceNm());

				String mergedStr = "";
				for(String eachText : argumentParam){
					if(eachText.matches("^'.*'$")){
						mergedStr = mergedStr + eachText.replaceAll("'", "");
					}else{
						// SOURCE에서 특정 위치를 지정해줘야한다. 
						// A.가.a.v1 같이
						mergedStr = mergedStr + TransformUtil.getNestedValueInJson(source, eachText);
					}
				}

				target.put(ruleNode.getTargetElementNm(), mergedStr);
			}else if(ruleNode.getTransactionType().equals(TransactionType.UUID)){
				target.put(ruleNode.getTargetElementNm(), UUID.randomUUID());
			}else if(ruleNode.getTransactionType().equals(TransactionType.DATE)){
				List<String> argumentParam = MapperUtils.extractMultipleValues(ruleNode.getSourceReferenceNm());
				if(argumentParam.size() != 3){
					throw new IllegalArgumentException(" 룰 " + ruleNode.getRule() + " 의 파라미터가 누락되어 컨버전에 실패하였습니다.");
				}
				String sourceDate = source.getString(argumentParam.get(0));
				String sourceDateType = argumentParam.get(1).replaceAll("'", "");
				String targetDateType = argumentParam.get(2).replaceAll("'", "");
				String convertedDate = TransformUtil.convertDateTimeSourceToTarget(sourceDate, sourceDateType, targetDateType);

				target.put(ruleNode.getTargetElementNm(), convertedDate);
			}else if(ruleNode.getTransactionType().equals(TransactionType.CASE)){
				List<String> argumentParam = MapperUtils.extractMultipleValues(ruleNode.getSourceReferenceNm());
				String argument = source.getString(argumentParam.get(0));
				int argumentSize= argumentParam.size();
				if(argumentSize % 2 != 1 || argumentSize == 1){
					ourLog.error("[ERR] CASE문은 항상 샘플-조건-결과 순으로 나열되어야합니다.");
					ourLog.error("  ㄴ Rule : " + ruleNode.getSourceReferenceNm());
					ourLog.error("  ㄴ source : " + argument);
					throw new IllegalArgumentException("[ERR] CASE문은 항상 샘플-조건-결과 순으로 나열되어야합니다.");
				}

				boolean hasAnswer = false;
				for(int arg = 1; argumentSize > arg; arg=arg+2){
					if(argumentParam.get(arg).replaceAll("'", "").equals(argument)){
						if(argumentParam.get(arg+1).contains("'")){
							target.put(ruleNode.getTargetElementNm(), argumentParam.get(arg+1).replaceAll("'", "") );
						}else{
							target.put(ruleNode.getTargetElementNm(), source.get(argumentParam.get(arg+1)));
						}
						hasAnswer = true;
						break;
					}
				}

				if(!hasAnswer){
					ourLog.error("[ERR] CASE 문에 정의되지 않은 값이 입력되어 오류가 발생하였습니다.");
					ourLog.error("  ㄴ Rule : " + ruleNode.getSourceReferenceNm());
					ourLog.error("  ㄴ source : " + argument);
					throw new IllegalArgumentException("[ERR] CASE문에 정의되지 않은 값이 입력되어 오류가 발생하였습니다.. ");
				}

			}else if(ruleNode.getTransactionType().equals(TransactionType.TYPE)){ // TYPE(A, NUMBER, B, C)
				List<String> argumentParam = MapperUtils.extractMultipleValues(ruleNode.getSourceReferenceNm());
				String argument = source.getString(argumentParam.get(0));
				if(argumentParam.size() != 4){
					ourLog.error("[ERR] TYPE 문에 정의되지 않은 값이 입력되어 오류가 발생하였습니다.");
					ourLog.error("  ㄴ Rule : " + ruleNode.getSourceReferenceNm());
					ourLog.error("  ㄴ source : " + argument);
					throw new IllegalArgumentException("[ERR] TYPE 문에 정의되지 않은 값이 입력되어 오류가 발생하였습니다.");
				}else{
					String eachCondition = argumentParam.get(1);
					String yesResult = argumentParam.get(2);
					String noResult = argumentParam.get(3);
					if(eachCondition.equals("'NUMBER'")){
						try{
							if(!argument.isBlank() || !argument.isEmpty()){
								Float.parseFloat(argument);
								Integer.parseInt(argument);

								if(yesResult.contains("'")){
									target.put(ruleNode.getTargetElementNm(), yesResult.replaceAll("'", "") );
								}else{
									target.put(ruleNode.getTargetElementNm(), source.get(yesResult));
								}
							}else{
								throw new NumberFormatException("값이 비었습니다.");
							}
						}catch(NumberFormatException e){
							if(noResult.contains("'")){
								target.put(ruleNode.getTargetElementNm(), noResult.replaceAll("'", "") );
							}else{
								target.put(ruleNode.getTargetElementNm(), source.get(noResult));
							}
						}
					}else{
						throw new IllegalArgumentException("[ERR] 현재 TYPE 명령어는 NUMBER(INT, FLOAT 검증)만 지원하고 있습니다.");
					}
				}
			}
		} else if (ruleNode.getRuleType().equals(RuleType.CREATE_ARRAY)){
			String targetText = RuleUtils.getArrayTypeObjectNameTarget(ruleNode.getRule());
			target.putOpt(targetText, new JSONArray());
		}

		// 2. 룰 중에 key 값은 따로 모아놓기
		if(ruleNode.isIdentifierNode()){
			String answer = target.getString(ruleNode.getTargetElementNm());
			setIdentifierGenerator(source.getString("resource_type"), ruleNode.getSourceReferenceNm(), answer);
		}

		return target;
	}

	/**
	 *  배열형 구조의 데이터 결과의 대한 병합 함수
	 * @param activateTransNode the activate trans node
	 * @return the activate trans node
	 * @throws JSONException the json exception
	 */
	public ActivateTransNode recursiveActTransNode(ActivateTransNode activateTransNode) throws JSONException{
		// execute
		RuleNode ruleNode = activateTransNode.getRuleNode();
		JSONObject source = activateTransNode.getSource();
		JSONObject target = executeRule(ruleNode, source);
		activateTransNode.setTarget(target);

		// 하위 없으면 반환.
		if(activateTransNode.getRuleNode().getChildren().size() == 0){
			ourLog.info("--------------------------------=END NODE=--------------------------");
			ourLog.info(" ㄴ Rule : " + ruleNode.getRule() + " / " + ruleNode.getSourceReferenceNm() + " to " + ruleNode.getTargetElementNm());
			ourLog.info(" ㄴ target Data : " + target);

			return activateTransNode;
		}else{
			// recursive devide and conquar
			// target = target(seperation + seperation)
			MultiValueMap<String, JSONObject> retTargetList = new LinkedMultiValueMap<>();
			for(RuleNode childNode : ruleNode.getChildren()){
				// 1. 하위 룰 조회.
				ActivateTransNode subRuleNode = new ActivateTransNode();
				subRuleNode.setRuleNode(childNode);
				subRuleNode.setSource(source);
				subRuleNode.setTarget(null); // 최종결과시에만 활용
				ActivateTransNode ret = recursiveActTransNode(subRuleNode);
				retTargetList.add(childNode.getTargetElementNm(), ret.getTarget());
			}

			// 2.2 병합
			// 2023. 11. 20. Array의 대한 패턴 검증 완료.
			JSONObject retTarget = new JSONObject();
			boolean canBeMerged = false;
			if(ruleNode.getRuleType().equals(RuleType.CREATE_ARRAY)){
				ourLog.info("--------------------------------=ARRAY=--------------------------");
				ourLog.info(" ㄴ Rule : " + ruleNode.getRule());
				ourLog.info(" ㄴ Merged Node Data : " + retTargetList);

				// 2.2.1. array 인 경우
				JSONArray array = new JSONArray();
				JSONObject arrayMergeObject = new JSONObject();
				List<List<JSONObject>> sameLevelGrapObject = new LinkedList<>();
				for (String retKey : retTargetList.keySet()){
					if(retKey.matches(TransactionType.CREATE_SINGLESTRING.getPattern().pattern())){
						// [{'http://abc':'http://abc'}] -> ['http://abc']
						array.put(retTargetList.get(retKey).get(0).get(retKey));
						canBeMerged = true;
					}else{
						// [{"a":"1", "b":"2"}, {"a":"1", "b":"2"}]
						if(retTargetList.get(retKey).size()>=2 && ruleNode.getRuleType().equals(RuleType.CREATE_ARRAY)){
							List<JSONObject> jsonObjectList = new LinkedList<>();
							for(JSONObject eachResources : retTargetList.get(retKey)){
								jsonObjectList.add(eachResources);
							}
							sameLevelGrapObject.add(jsonObjectList);
							canBeMerged = true;
						}else{
							// {"a":"1"},{"b":"2"} -> {"a":"1", "b":"2"}
							RuleUtils.mergeJsonObjects(arrayMergeObject, retTargetList.get(retKey).get(0));
							canBeMerged = false;
						}
					}
				}

				// [{"a":"1", "b":"2"}, {"a":"1", "b":"2"}] 의 병합
				if(sameLevelGrapObject.size() >= 2){
					ourLog.info(" ㄴ 같은 레벨에 여러개의 Key 데이터 구조의 대한 병합 수행 시작...");
					int maxSize = 0;
					for (List<JSONObject> innerList : sameLevelGrapObject) {
						if (innerList.size() > maxSize) {
							maxSize = innerList.size();
						}
					}

					ourLog.info("   -> 병합해야할 갯수 : " + maxSize);
					ourLog.info("   -> 병합대상 : " + sameLevelGrapObject);
					JSONArray mergeArray = new JSONArray();
					for (int i = 0; i < maxSize; i++) {
						JSONObject mergeObject = new JSONObject();
						for (List<JSONObject> i1 : sameLevelGrapObject) {
							if(i1.size() > i){
								RuleUtils.mergeJsonObjects(mergeObject, i1.get(i));
							}
						}
						mergeArray.put(mergeObject);
					}
					ourLog.info(" ㄴ 병합 결과... : " + mergeArray);
					array = mergeArray;
				}else if(sameLevelGrapObject.size() == 1){
					ourLog.info(" ㄴ 같은 레벨의 단일 Key Array의 경우, 구성별 처리");
					ourLog.info("   -> 병합대상 : " + sameLevelGrapObject);
					List<JSONObject> mergeList = sameLevelGrapObject.get(0);
					JSONArray mergeArray = new JSONArray();
					for(JSONObject eachArrayObject : mergeList){
						mergeArray.put(eachArrayObject);
					}
					array = mergeArray;
				}

				// 3. 배열형 반영
				if(canBeMerged){
						retTarget.put(RuleUtils.getArrayTypeObjectNameTarget(ruleNode.getRule()), array);
				}else{
					array.put(arrayMergeObject);
					retTarget.put(RuleUtils.getArrayTypeObjectNameTarget(ruleNode.getRule()), array);
				}
				activateTransNode.setTarget(retTarget);

			}else {
				ourLog.info("--------------------------------=SINGLE=--------------------------");
				ourLog.info(" ㄴ Rule : " + ruleNode.getRule());
				ourLog.info(" ㄴ Merged Node Data : " + retTargetList);

				// 2.2.2. 단일패턴
				JSONObject mergeJsonObj = new JSONObject();
				for (String retKey : retTargetList.keySet()) {
					// array 의 선언을 위해 구현된 룰은 동작시키지않음.
					if(retKey.contains("(") && retKey.contains(")")){
						mergeJsonObj = retTargetList.get(retKey).get(0);
					}else{
						RuleUtils.mergeJsonObjects(mergeJsonObj, retTargetList.get(retKey).get(0));
					}
				}
				retTarget = mergeJsonObj;

				// 3. 단일형 반영
				target.put(ruleNode.getTargetElementNm(), retTarget);

				activateTransNode.setTarget(target);
			}

			return activateTransNode;
		}
	}

	/** 2024. 02. 27. 분개
	 * Rule Node 를 생성해준다.
	 *
	 * @param map the map
	 * @return the list
	 */
	public List<RuleNode> createRuleNodeTree(String map){
		String script = MapperUtils.getSeparateMapScript(map, "transform");
		// 1. 트리 생성
		List<RuleNode> ruleNodeList = MapperUtils.createTree(script);
		// logging
		for(RuleNode eachRuleNode : ruleNodeList){
			// DFS 기반 맵서칭
			MapperUtils.printRuleAndRuleTypeInNodeTree(eachRuleNode);
		}
		return ruleNodeList;
	}

	public IBaseResource transformDataToResource(List<RuleNode> nonArrayedRuleNodeList, @NonNull JSONObject source){
		ourLog.info("-------------------------------------------------------------- 데이터 생성 시작...");
		
		// logging
		for(RuleNode eachRuleNode : nonArrayedRuleNodeList){
			// DFS 기반 맵서칭
			MapperUtils.printRuleAndRuleTypeInNodeTree(eachRuleNode);
		}
		
		// 2. 변환 수행
		JSONObject targetObject = new JSONObject();
		Set<String> namedKeySet = new LinkedHashSet<>();
		try {
			JSONObject retJsonObject = new JSONObject();
			JSONObject sourceObj = source;
			for (RuleNode eachRuleNode : nonArrayedRuleNodeList) {
				ActivateTransNode activateTransNode = new ActivateTransNode();
				activateTransNode.setRuleNode(eachRuleNode);
				activateTransNode.setSource(sourceObj);
				activateTransNode.setTarget(targetObject);
				ActivateTransNode ret = recursiveActTransNode(activateTransNode);

				// 룰 병합
				// 1. 같은 룰이 두개 이상 들어오면 Array처리하기
				if(retJsonObject.length() != 0){
					// 추가하려는 JSON 의 최상위 노드 조회
					String inputDataHeaderKey = (String)ret.getTarget().keys().next();
					ourLog.debug( " Already Contain Key Set ::: " + namedKeySet.toString());
					ourLog.debug( " Search the key >>>> " + inputDataHeaderKey);
					if(namedKeySet.contains(inputDataHeaderKey)){
						// 이미 수행한 적이 있는 Rule이 반복된다면 배열화
						ourLog.debug( " >>>> " + inputDataHeaderKey + " is Already Contained Then ");
						Object jsonObject = retJsonObject.get(inputDataHeaderKey);
						JSONArray jsonArray = new JSONArray();
						if(jsonObject.getClass().equals(JSONArray.class)){
							// 이미 배열화 된 경우
							ourLog.debug( " >>>> " + inputDataHeaderKey + " is Already Arrayed");
							jsonArray = (JSONArray)jsonObject;
							jsonArray.put(ret.getTarget().get(inputDataHeaderKey));
						}else{
							// 최초 배열화
							ourLog.debug( " >>>> " + inputDataHeaderKey + " is to be Array");
							jsonArray.put(jsonObject);
							jsonArray.put(ret.getTarget().get(inputDataHeaderKey));
						}

						JSONObject retObject = new JSONObject();
						retObject.put(inputDataHeaderKey, jsonArray);
						RuleUtils.mergeJsonObjects(retJsonObject, retObject);
					}else{
						ourLog.debug(" - can not found the key " + inputDataHeaderKey + " then insert this.");
						namedKeySet.add(inputDataHeaderKey);
						RuleUtils.mergeJsonObjects(retJsonObject, ret.getTarget());
					}
				}else{
					String inputDataHeaderKey = (String)ret.getTarget().keys().next();
					namedKeySet.add(inputDataHeaderKey);
					RuleUtils.mergeJsonObjects(retJsonObject, ret.getTarget());
				}
			}

			// TODO. 작업 완료 시 debug로 내리기.
			ourLog.info("생성 결과 : " + retJsonObject);

			FhirContext context = new FhirContext(FhirVersionEnum.R4);
			IBaseResource resource = context.newJsonParser().parseResource(retJsonObject.toString());

			ourLog.info("-------------------------------------------------------------- FHIR Resource 키 없는 경우 identifier 활용 id값 생성..");
			// 3. 변환의 대한 키 생성
			// 키가 없는 경우에만 활용하기
			// resource.setId(generatorIdentifierForResource(resource.fhirType()));

			ourLog.info("FHIR Resource 변환 결과 : " + resource.toString());
			ourLog.info("-------------------------------------------------------------- 데이터 생성 종료...");

			return resource;
		}catch(Exception e){
			e.printStackTrace();
			throw new IllegalArgumentException("데이터를 변환하는 과정에서 오류가 발생하였습니다.");
		}
	}

	// 2023. 11. 28. 키관리의 유동화 처리
	class IdentifierGeneratorForResource{

		private LinkedHashMap<String, String> identifierMap;

		public IdentifierGeneratorForResource(){
			identifierMap = new LinkedHashMap<>();
		}

		public void addKeyAndValue(String identifierName, String value){
			identifierMap.put(identifierName, value);
		}

		public LinkedHashMap<String, String> getIdentifierMap() {
			return identifierMap;
		}
	}

	Map<String, IdentifierGeneratorForResource> keyGeneratorForResourceList = new HashMap<>();

	// ID를 각 Node별로 순회 중 구성한다
	public void setIdentifierGenerator(String resourceType, String identifierName, String value){
		IdentifierGeneratorForResource targetRes = keyGeneratorForResourceList.get(resourceType);
		if(targetRes == null){
			targetRes = new IdentifierGeneratorForResource();
		}
		targetRes.addKeyAndValue(identifierName, value);
		keyGeneratorForResourceList.put(resourceType, targetRes);
	}

	// 리소스의 ID를 생성하는 조건을 반환한다.
	public Set<String> getResourceIdentifierSet(String resourceType){
		return keyGeneratorForResourceList.get(resourceType).getIdentifierMap().keySet();
	}

	// 리소스의 ID를 생성한다.
	public String generatorIdentifierForResource(String resourceType){
		LinkedHashMap<String, String> map = keyGeneratorForResourceList.get(resourceType).getIdentifierMap();
		String retIdentifier = ResourceNameSummaryCode.findSummaryName(resourceType).getSummaryName();
		for (String keyElement : map.keySet()) {
			if(StringUtils.isBlank(retIdentifier)){
				retIdentifier = map.get(keyElement);
			}else{
				retIdentifier = retIdentifier + "." + map.get(keyElement);
			}
		}

		return retIdentifier;
	}
}

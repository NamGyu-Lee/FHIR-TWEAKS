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

	public TransformEngine(CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties){
		translationEngine = new TranslationEngine(customValidationRemoteConfigProperties);
	}

	// 룰 실행부분
	public JSONObject executeRule(RuleNode ruleNode, JSONObject source) throws JSONException {
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

				}else{
					target.put(ruleNode.getTargetElementNm(), source.get(ruleNode.getSourceReferenceNm()));
				}
			}else if(ruleNode.getTransactionType().equals(TransactionType.COPY_STRING)){
				target.put(ruleNode.getTargetElementNm(), ruleNode.getSourceReferenceNm().replaceAll("'", ""));
			}else if(ruleNode.getTransactionType().equals(TransactionType.TRANSLATION)){
				List<String> argumentParam = this.extractMultipleValues(ruleNode.getSourceReferenceNm());
				String exchangedCodeValue = translationEngine.translateCode(argumentParam.get(0), argumentParam.get(1));
				target.put(ruleNode.getTargetElementNm(), exchangedCodeValue);
			}else if(ruleNode.getTransactionType().equals(TransactionType.COPY_WITH_DEFAULT)){
				List<String> argumentParam = this.extractMultipleValues(ruleNode.getSourceReferenceNm());
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
					target.put(ruleNode.getTargetElementNm(), argumentParam.get(1).replaceAll("'", ""));
				}else{
					target.put(ruleNode.getTargetElementNm(), source.get(argumentParam.get(0)));
				}
			}else if(ruleNode.getTransactionType().equals(TransactionType.SPLIT)){
				List<String> argumentParam = this.extractMultipleValues(ruleNode.getSourceReferenceNm());
				if(argumentParam.size() == 3){
					target.put(ruleNode.getTargetElementNm(), ((String)source.get(argumentParam.get(0))).substring(Integer.parseInt(argumentParam.get(1)), Integer.parseInt(argumentParam.get(2))));
				}else if(argumentParam.size() == 2){
					target.put(ruleNode.getTargetElementNm(), ((String)source.get(argumentParam.get(0))).substring(Integer.parseInt(argumentParam.get(1))));
				}else{
					throw new JSONException("Execute Rule 중에 예상치 못한 함수가 들어왔습니다. Split은 반드시 파라미터가 2개 혹은 3개여야합니다.");
				}
			}else if(ruleNode.getTransactionType().equals(TransactionType.MERGE)){
				System.out.println("ruleNode.getSourceReferenceNm() : " + ruleNode.getSourceReferenceNm());
				List<String> argumentParam = this.extractMultipleValues(ruleNode.getSourceReferenceNm());
				System.out.println(" MERGE RULE !!! argumentParam : " + argumentParam.size());

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
				System.out.println(" vVAL : " + mergedStr);

				target.put(ruleNode.getTargetElementNm(), mergedStr);
			}
		} else if (ruleNode.getRuleType().equals(RuleType.CREATE_ARRAY)){
			String targetText = RuleUtils.getArrayTypeObjectNameTarget(ruleNode.getRule());
			target.putOpt(targetText, new JSONArray());
		}
		return target;
	}

	// 룰을 보유한 노드의 Recursive부분
	// TODO. 2023. 11. 20. Array 로 Source 가 여러개 들어오는 경우의 대하여 처리하기.
	public ActivateTransNode recursiveActTransNode(ActivateTransNode activateTransNode) throws JSONException {
		// execute
		RuleNode ruleNode = activateTransNode.getRuleNode();
		JSONObject source = activateTransNode.getSource();
		JSONObject target = executeRule(ruleNode, source);
		activateTransNode.setTarget(target);

		// 하위 없으면 반환.
		if(activateTransNode.getRuleNode().getChildren().size() == 0){
			ourLog.info("--------------------------------=END NODE=--------------------------");
			ourLog.info(" ㄴ Rule : " + ruleNode.getRule());
			ourLog.info(" ㄴ target Data : " + target);

			return activateTransNode;
		}else{
			// recursive
			// target = target(seperation + seperation)
			MultiValueMap<String, JSONObject> retTargetList = new LinkedMultiValueMap<>();
			for(RuleNode childNode : ruleNode.getChildren()){
				//System.out.println(" -- Parent Rule : " + ruleNode.getRule());
				//System.out.println(" -- Child Rule : " + childNode.getRule());
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
				if(sameLevelGrapObject.size() > 2){
					ourLog.info(" ㄴ 같은 레벨에 여러개의 Key 데이터가 존재하여 병합 수행 시작...");
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

	// 실제 동작함수
	public IBaseResource transformDataToResource(String map, @NonNull JSONObject source){
		String script = map;

		// 1. 트리 생성
		List<RuleNode> ruleNodeList = MapperUtils.createTree(script);
		for(RuleNode eachRuleNode : ruleNodeList){
			// DFS 기반 맵서칭
			MapperUtils.printRuleAndRuleTypeInNodeTree(eachRuleNode);
		}

		// 2. 변환 수행
		JSONObject targetObject = new JSONObject();
		Set<String> namedKeySet = new LinkedHashSet<>();
		try {
			JSONObject retJsonObject = new JSONObject();
			JSONObject sourceObj = source;
			for (RuleNode eachRuleNode : ruleNodeList) {
				ActivateTransNode activateTransNode = new ActivateTransNode();
				activateTransNode.setRuleNode(eachRuleNode);
				activateTransNode.setSource(sourceObj);
				activateTransNode.setTarget(targetObject);
				ActivateTransNode ret = recursiveActTransNode(activateTransNode);
				//System.out.println(" ▶ Active Result Per Rules : " + ret.getTarget().toString());

				// mergeRule
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

			ourLog.info(" Operation Result : " + retJsonObject.toString());

			FhirContext context = new FhirContext(FhirVersionEnum.R4);
			IBaseResource resource = context.newJsonParser().parseResource(retJsonObject.toString());

			// 3. 변환의 대한 키 생성
			LinkedHashSet<String> identifierSet = MapperUtils.createIdentifierMap(ruleNodeList);
			if(identifierSet.size() >= 1 && ResourceNameSummaryCode.isCanbeSummaryName(resource.fhirType())){
				String id = TransformUtil.createResourceId(resource.fhirType(), identifierSet, sourceObj);
				System.out.println(">>>>>>>>>>>>>>>>> CREATE ID : " + id);
				resource.setId(id);
			}

			return resource;
		}catch(org.json.JSONException e){
			e.printStackTrace();
			throw new IllegalArgumentException("데이터를 변환하는 과정에서 오류가 발생하였습니다.");
		}
	}

	public List<String> extractMultipleValues(String input) {
		List<String> values = new ArrayList<>();
		Pattern pattern = Pattern.compile("\\w+\\((.*)\\)");
		Matcher matcher = pattern.matcher(input);

		if (matcher.find()) {
			String[] args = matcher.group(1).split(",\\s*");
			for (String arg : args) {
				values.add(arg.trim());
			}
		}
		return values;
	}

}

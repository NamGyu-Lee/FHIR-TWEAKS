package ca.uhn.fhir.jpa.starter.transfor.base.core;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.starter.common.StarterJpaConfig;
import ca.uhn.fhir.jpa.starter.transfor.base.code.RuleType;
import ca.uhn.fhir.jpa.starter.transfor.base.code.TransactionType;
import ca.uhn.fhir.jpa.starter.transfor.base.map.ActivateTransNode;
import ca.uhn.fhir.jpa.starter.transfor.base.map.RuleNode;
import ca.uhn.fhir.jpa.starter.transfor.base.util.MapperUtils;
import ca.uhn.fhir.jpa.starter.transfor.base.util.RuleUtils;
import lombok.NonNull;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Patient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;

/** 2023. 11. 20.
 * 특정 언어구조를 활용하여 데이터를 치환하는 로직을 구현한다.
 */
public class TransformEngine{

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(TransformEngine.class);

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

			for (String retKey : retTargetList.keySet()) {
				System.out.println(ruleNode.getRule() + " HAS " + "INNER :: " + retKey + "    -> " + retTargetList.get(retKey));
			}

			// 2.2 병합
			// 2023. 11. 20. Array의 대한 패턴 검증 완료.
			JSONObject retTarget = new JSONObject();
			boolean canBeMerged = false;
			if(ruleNode.getRuleType().equals(RuleType.CREATE_ARRAY)){

				System.out.println("\n--------------------------------=ARRAY=--------------------------");

				// 2.2.1. array 인 경우
				JSONArray array = new JSONArray();
				JSONObject obj = new JSONObject();
				List<List<JSONObject>> sameLevelGrapObject = new LinkedList<>();
				for (String retKey : retTargetList.keySet()){
					System.out.println(retKey + " /  array          -> " + retTargetList.get(retKey));
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
							RuleUtils.mergeJsonObjects(obj, retTargetList.get(retKey).get(0));
							canBeMerged = false;
						}
					}
				}
				
				// [{"a":"1", "b":"2"}, {"a":"1", "b":"2"}] 의 병합
				if(sameLevelGrapObject.size() > 2){
					int maxSize = 0;
					for (List<JSONObject> innerList : sameLevelGrapObject) {
						if (innerList.size() > maxSize) {
							maxSize = innerList.size();
						}
					}

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
					array = mergeArray;
				}

				// 3. 배열형 반영
				if(canBeMerged){
						retTarget.put(RuleUtils.getArrayTypeObjectNameTarget(ruleNode.getRule()), array);
				}else{
					array.put(obj);
					retTarget.put(RuleUtils.getArrayTypeObjectNameTarget(ruleNode.getRule()), array);
				}
				activateTransNode.setTarget(retTarget);

			}else {
				System.out.println("\n--------------------------------=SINGLE=--------------------------");

				// 2.2.2. 단일패턴
				JSONObject mergeJsonObj = new JSONObject();
				for (String retKey : retTargetList.keySet()) {
					System.out.println(" > " + retKey + " then single -> " + retTargetList.get(retKey));
					// array 의 선언을 위해 구현된 룰은 동작시키지않음.
					if(retKey.contains("(") && retKey.contains(")")){
						mergeJsonObj = retTargetList.get(retKey).get(0);
					}else{
						RuleUtils.mergeJsonObjects(mergeJsonObj, retTargetList.get(retKey).get(0));
					}
				}

				retTarget = mergeJsonObj;
				System.out.println(" > retTarget : " + retTarget);
				// 3. 단일형 반영

				target.put(ruleNode.getTargetElementNm(), retTarget);
				System.out.println(" > target : " + target);

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
		JSONObject targetObject = new JSONObject();
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

				RuleUtils.mergeJsonObjects(retJsonObject, ret.getTarget());
			}

			System.out.println(retJsonObject);

			FhirContext context = new FhirContext(FhirVersionEnum.R4);
			IBaseResource resource = context.newJsonParser().parseResource(retJsonObject.toString());

			return resource;
		}catch(org.json.JSONException e){
			// 임시
			e.printStackTrace();
			throw new IllegalArgumentException("데이터를 변환하는 과정에서 오류가 발생하였습니다.");
		}
	}

}

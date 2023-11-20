package ca.uhn.fhir.jpa.starter.transfor.base.core;

import ca.uhn.fhir.jpa.starter.transfor.base.code.RuleType;
import ca.uhn.fhir.jpa.starter.transfor.base.code.TransactionType;
import ca.uhn.fhir.jpa.starter.transfor.base.map.ActivateTransNode;
import ca.uhn.fhir.jpa.starter.transfor.base.map.RuleNode;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** 2023. 11. 20.
 * 특정 언어구조를 활용하여 데이터를 치환하는 로직을 구현한다.
 */
public class TransformEngine {

	// 룰 실행부분
	public JSONObject executeRule(RuleNode ruleNode, JSONObject source) throws JSONException {
		JSONObject target = new JSONObject();
		if (ruleNode.getRuleType().equals(RuleType.CREATE)){
			String targetText = ruleNode.getRule();
			target.putOpt(targetText, new JSONObject());
		} else if (ruleNode.getRuleType().equals(RuleType.TRANS)) {
			if(ruleNode.getTransactionType().equals(TransactionType.COPY)){
				target.put(ruleNode.getTargetElementNm(), source.get(ruleNode.getSourceReferenceNm()));
			}
		}
		return target;
	}

	// 룰을 보유한 노드의 Recursive부분
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
			Map<String, JSONObject> retTargetList = new LinkedHashMap<>();
			for(RuleNode childNode : ruleNode.getChildren()){
				System.out.println(" -- Parent Rule : " + ruleNode.getRule());
				System.out.println(" -- Child Rule : " + childNode.getRule());
				// 1. 하위 룰 조회.
				ActivateTransNode subRuleNode = new ActivateTransNode();
				subRuleNode.setRuleNode(childNode);
				subRuleNode.setSource(source);
				subRuleNode.setTarget(null); // 미활용
				ActivateTransNode ret = recursiveActTransNode(subRuleNode);

				retTargetList.put(childNode.getTargetElementNm(), ret.getTarget());
			}

			// 2.2 병합
			JSONObject retTarget = new JSONObject();
			for(String retKey : retTargetList.keySet()){
				System.out.println("          -> " + retTargetList.get(retKey));
				retTarget.put(retKey, retTargetList.get(retKey).get(retKey));
			}
			//System.out.println("  - 병합 완료 : " + retTarget.toString());

			// 3. 반영
			target.put(ruleNode.getTargetElementNm(), retTarget);
			activateTransNode.setTarget(target);

			return activateTransNode;
		}
	}
}

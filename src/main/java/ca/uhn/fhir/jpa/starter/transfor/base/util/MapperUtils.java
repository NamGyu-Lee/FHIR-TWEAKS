package ca.uhn.fhir.jpa.starter.transfor.base.util;

import ca.uhn.fhir.jpa.starter.transfor.base.code.RuleType;
import ca.uhn.fhir.jpa.starter.transfor.base.code.TransactionType;
import ca.uhn.fhir.jpa.starter.transfor.base.map.RuleNode;

import java.util.*;

/** StructureMap 을 대체 할 수 있는 Map 을 구성하기위한 기준정의를 Node로 수행
 *  각 Node는 하나의 룰을 관리.
 */
public class MapperUtils {

	public static int getLevel(String line) {
		int count = 0;
		for (char c : line.toCharArray()) {
			if (c != ' ') break;
			count++;
		}
		return count;
	}

	/**
	 * 룰을 가져와서 N진 트리로 재구성해준다.
	 *
	 * @param script the script
	 * @return the list
	 */
	public static List<RuleNode> createTree(String script) {
		List<RuleNode> ruleNodes = new ArrayList<>();
		RuleNode currentParent = null;
		RuleNode beforeNode = null;
		int currentLevel = 0;

		for (String line : script.split("\n")) {
			int level = getLevel(line);
			// Line 처리
			line = line.replace("*", "");
			line = line.replaceAll("\\s*:\\s*", ":");

			// ID 생성용 키값(혹은 조합키값) 인지 검증.
			// id = (KEY)id -> id = id, isIdentifierNode = true;
			boolean isIdentifierNode = false;
			if(line.contains("(KEY)")){
				line = line.replaceAll("\\(KEY\\)", "");
				isIdentifierNode = true;
			}else{
				isIdentifierNode = false;
			}

			// 맵 구조화
			RuleNode ruleNode = new RuleNode(null, line.trim(), level, isIdentifierNode);
			
			if (level == currentLevel && currentParent != null) {
				ruleNode.setParent(currentParent);
				currentParent.addChild(ruleNode);
			}else if(level > currentLevel) {
				ruleNode.setParent(beforeNode);
				beforeNode.addChild(ruleNode);
				currentParent = beforeNode;
				currentLevel = level;
			}else if(level < currentLevel && level != 0 && currentLevel != 0){
				int differLevelCount = currentLevel - level + 1;
				while(differLevelCount != 0){
					beforeNode = beforeNode.getParent();
					differLevelCount--;
				}
				ruleNode.setParent(beforeNode);
				beforeNode.addChild(ruleNode);
				currentParent = beforeNode;
				currentLevel = level;
			}else {
				// 가장 첫노드는 자기 자신을 가르킴
				ruleNode.setParent(ruleNode);
				ruleNodes.add(ruleNode);
				currentParent = null;
				currentLevel = level;
			}

			beforeNode = ruleNode;
		}
		return ruleNodes;
	}

	public static void printRuleTextInNodeTree(int startLevel, RuleNode nd){
		System.out.print(startLevel);
		for(int i = 0; startLevel > i; i++){
			System.out.print(" ");
		}
		System.out.print(" > " + nd.getRule());
		System.out.println();

		if(nd.getChildren().size() == 0){
			return;
		}else{
			for(RuleNode childRuleNode : nd.getChildren()){
				printRuleTextInNodeTree(startLevel + 1, childRuleNode);
			}
		}
	}

	public static void printRuleAndRuleTypeInNodeTree(RuleNode nd){
		for(int i = 0; nd.getLevel() > i; i++){
			System.out.print(" ");
		}
		System.out.print(" > " + nd.getRuleType() + " : " + nd.getTransactionType() + " / " + nd.getRule());
		System.out.println();

		if(nd.getChildren().size() == 0){
			return;
		}else{
			for(RuleNode childRuleNode : nd.getChildren()){
				printRuleAndRuleTypeInNodeTree(childRuleNode);
			}
		}
	}
}

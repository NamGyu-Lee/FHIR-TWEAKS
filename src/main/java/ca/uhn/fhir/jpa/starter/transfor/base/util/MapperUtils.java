package ca.uhn.fhir.jpa.starter.transfor.base.util;

import ca.uhn.fhir.jpa.starter.transfor.base.code.ErrorHandleType;
import ca.uhn.fhir.jpa.starter.transfor.base.map.MetaRule;
import ca.uhn.fhir.jpa.starter.transfor.base.map.ReferenceNode;
import ca.uhn.fhir.jpa.starter.transfor.base.map.ReferenceParamNode;
import ca.uhn.fhir.jpa.starter.transfor.base.map.RuleNode;
import org.checkerframework.common.value.qual.IntRange;
import org.jetbrains.annotations.Range;

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

	/**
	 * 2023. 12. 18. Reference Node를 구성한다.
	 *
	 * @param script the script
	 * @return the list
	 */
	public static MetaRule createMetaRule(String script) throws IllegalArgumentException {
		MetaRule metaRule = new MetaRule();

		// reference 용
		List<ReferenceParamNode> referenceParamNodeList = new ArrayList<>();
		List<ReferenceNode> referenceNodeList = new ArrayList<>();
		ReferenceNode currentReferenceNode = null;

		Iterator<String> iterator = Arrays.asList(script.split("\n")).iterator();
		while(iterator.hasNext()) {
			String line = iterator.next();
			int level = getLevel(line);
			line.replace("*", "");
			line = line.replaceAll("\\s*=\\s*", "=");
			line = line.trim();

			if(line.contains("error_policy=")) {
				metaRule.setErrorHandleType(ErrorHandleType.searchErrorHandleType(line.split("=")[1].trim()));
			}else if(line.contains("cacheKey")){
				String cacheKeySingleStr = line.split("=")[1].trim();
				String[] cacheKeyArray = cacheKeySingleStr.split(",");
				Set<String> cacheKeySet = new HashSet<>();
				for(String arg : cacheKeyArray){
					cacheKeySet.add(arg.trim());
				}
				metaRule.setCacheDataKey(cacheKeySet);
			}else if(line.contains("referenceResource")){
				if(currentReferenceNode != null) {
					currentReferenceNode.setReferenceParamNodeList(referenceParamNodeList);
					referenceNodeList.add(currentReferenceNode);
				}
				currentReferenceNode = new ReferenceNode();
				referenceParamNodeList = new ArrayList<>();
			}else if(line.contains("target=")){
				currentReferenceNode.setTargetResource(line.split("=")[1].trim());
			}else if(line.contains("error_policy=")){
				currentReferenceNode.setErrorHandleType(ErrorHandleType.searchErrorHandleType(line.split("=")[1]));
			}else if(line.contains("->")){
				referenceParamNodeList.add(createReferenceParamNode(line));
			}
			if(!iterator.hasNext() && currentReferenceNode != null){
				currentReferenceNode.setReferenceParamNodeList(referenceParamNodeList);
				referenceNodeList.add(currentReferenceNode);
			}
		}
		metaRule.setReferenceNodeList(referenceNodeList);
		return metaRule;
	}

	public static ReferenceParamNode createReferenceParamNode(String scriptLine) throws IllegalArgumentException{
		String[] parts = scriptLine.split("->|::");
		ReferenceParamNode refNode = new ReferenceParamNode();
		refNode.setSourceStr(parts[0].replace("*", "").trim());
		refNode.setCacheTargetStr(parts[1].trim());
		refNode.setFhirTargetStr(parts[2].trim());
		return refNode;
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

	/**
	 *  사용자가 작성한 맵의 요청한 타입에 맞춰 반환한다.
	 *  typeToScript = meta, transform
	 *
	 * @param arg          the arg
	 * @param typeToScript the type to script
	 * @return the separate map script
	 * @throws IllegalArgumentException the illegal argument exception
	 */
	public static String getSeparateMapScript(String arg, String typeToScript) throws IllegalArgumentException{
		String delimiter = "-{5,}"; // 5개 이상의 하이픈에 대한 정규 표현식
		String[] parts = arg.split(delimiter, 2); // 최대 두 부분으로 분할
		if(parts.length != 2){
			new IllegalArgumentException("[ERR] 해당 맵파일 정의에 오류가 있습니다. Map의 Reference 와 Data 영역은 -가 5개 이상으로 나누어 정의되어있어야합니다.");
		}

		if("meta".equals(typeToScript)){
			return parts[0];
		}else{
			return parts[1].replaceAll("^\\n+", "");
		}
	}
}

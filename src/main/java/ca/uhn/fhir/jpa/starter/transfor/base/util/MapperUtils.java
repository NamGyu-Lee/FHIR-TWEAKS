package ca.uhn.fhir.jpa.starter.transfor.base.util;

import ca.uhn.fhir.jpa.starter.transfor.base.code.ErrorHandleType;
import ca.uhn.fhir.jpa.starter.transfor.base.code.RuleType;
import ca.uhn.fhir.jpa.starter.transfor.base.code.TransactionType;
import ca.uhn.fhir.jpa.starter.transfor.base.core.TransformEngine;
import ca.uhn.fhir.jpa.starter.transfor.base.map.MetaRule;
import ca.uhn.fhir.jpa.starter.transfor.base.map.ReferenceNode;
import ca.uhn.fhir.jpa.starter.transfor.base.map.ReferenceParamNode;
import ca.uhn.fhir.jpa.starter.transfor.base.map.RuleNode;
import lombok.NonNull;
import org.checkerframework.common.value.qual.IntRange;
import org.h2.bnf.Rule;
import org.jetbrains.annotations.Range;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** StructureMap 을 대체 할 수 있는 Map 을 구성하기위한 기준정의를 Node로 수행
 *  각 Node는 하나의 룰을 관리.
 */
public class MapperUtils {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(MapperUtils.class);

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

			boolean isMergedNode = false;
			if(line.contains("(MERGE)")){
				line = line.replaceAll("\\(MERGE\\)", "");
				isMergedNode = true;
			}else{
				isMergedNode = false;
			}

			// 맵 구조화
			RuleNode ruleNode = new RuleNode(null, line.trim(), level, isIdentifierNode, isMergedNode);
			
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
			}else{
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

		ourLog.info("--- MetaRule 정보를 생성합니다. ---");
		ourLog.debug("ㄴ 생성 스크립트 : \n" + script);
		ourLog.debug("------------------------------");
		Iterator<String> iterator = Arrays.asList(script.split("\n")).iterator();
		while(iterator.hasNext()) {
			String line = iterator.next();
			int level = getLevel(line);
			line.replace("*", "");
			line = line.replaceAll("\\s*=\\s*", "=");
			line = line.trim();

			if(line.contains("error_policy")) {
				metaRule.setErrorHandleType(ErrorHandleType.searchErrorHandleType(line.split("=")[1].trim()));
			}else if(line.contains("cacheKey")){
				String cacheKeySingleStr = line.split("=")[1].trim();
				String[] cacheKeyArray = cacheKeySingleStr.split(",");
				Set<String> cacheKeySet = new HashSet<>();
				for(String arg : cacheKeyArray){
					cacheKeySet.add(arg.trim());
				}
				metaRule.setCacheDataKey(cacheKeySet);
			}else if(line.contains("mergeKey")){
				Set<String> mergeKeySet = new HashSet<>();
				if(line.split("=").length <= 1){
					// 데이터가 없는 경우 미활용
					ourLog.info(" ... 해당 맵은 mergeKey를 활용하지 않습니다.");
				}else{
					String mergeKeySingleStr = line.split("=")[1].trim();
					String[] mergeKeyArray = mergeKeySingleStr.split(",");
					for(String arg : mergeKeyArray){
						mergeKeySet.add(arg.trim());
					}
					ourLog.info(" ... 해당 맵의 merge Key size : " + mergeKeySet.size());
				}
				metaRule.setMergeDataKey(mergeKeySet);
			}else if(line.contains("referenceResource")){
				if(currentReferenceNode != null) {
					currentReferenceNode.setReferenceParamNodeList(referenceParamNodeList);
					referenceNodeList.add(currentReferenceNode);
				}
				currentReferenceNode = new ReferenceNode();
				referenceParamNodeList = new ArrayList<>();
			}else if(line.contains("target")){
				currentReferenceNode.setTargetResource(line.split("=")[1].trim());
			}else if(line.contains("depend_policy")){
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

		ourLog.info("---- created RuleNode information... ----");
		ourLog.info("keySet Size : " + metaRule.getCacheDataKey().size());
		ourLog.info("MergeKeySet Size : " + metaRule.getMergeDataKey().size());
		ourLog.info("ReferenceNode Size : " + metaRule.getReferenceNodeList().size());
		ourLog.info("-----------------------------------------");
		ourLog.info("--- MetaRule 정보를 생성 완료 하였습니다. ---");
		return metaRule;
	}

	/**
	 * Create reference param node Using reference param node.
	 *
	 * @param scriptLine the script line
	 * @return the reference param node
	 * @throws IllegalArgumentException the illegal argument exception
	 */
	public static ReferenceParamNode createReferenceParamNode(String scriptLine) throws IllegalArgumentException{
		String[] parts = scriptLine.split("->|::");
		ReferenceParamNode refNode = new ReferenceParamNode();
		refNode.setSourceStr(parts[0].replace("*", "").trim());
		refNode.setCacheTargetStr(parts[1].trim());
		refNode.setFhirTargetStr(parts[2].trim());
		return refNode;
	}

	public static void printRuleAndRuleTypeInNodeTree(RuleNode nd){
		String loggingStr = "";
		for(int i = 0; nd.getLevel() > i; i++){
			loggingStr = loggingStr + " ";
		}
		ourLog.info(loggingStr + " > " + nd.getRuleType() + " : " + nd.getTransactionType() + " / " + nd.getRule() + "  || source : " + nd.getSourceReferenceNm() + " || target : "+ nd.getTargetElementNm());

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

	/**
	 * 이진트리를 recursive 하게 수행하면서 병합되어있는 행의 source 정보에 대하여
	 * map에 반영시켜준다.
	 *
	 * @param ruleNode the rule node
	 * @param source   the source
	 * @return the rule node
	 */
	public static RuleNode createTreeForArrayWithRecursive(MetaRule metaRule, RuleNode ruleNode, JSONObject source) throws JSONException {
		List<RuleNode> childNodeList = ruleNode.getChildren();
		MapperUtils.printRuleAndRuleTypeInNodeTree(ruleNode);

		ourLog.info("---- Array 구조의 병합 Row의 대한 처리를 위해 Tree를 재구성합니다. ----");
		ourLog.info(" ㄴ 병합된 Source Data의 Row 크기 : " + source.getInt("merged_row_count"));
		ourLog.info(" ㄴ 병합에 활용된 Key Column Name : " + metaRule.getCacheDataKey());
		ourLog.info(" ㄴ 병합한 Column Name : " + metaRule.getMergeDataKey());
		ourLog.debug(" 동작 수행... : ");
		int mergeCount = source.getInt("merged_row_count");
		if(mergeCount <= 0){
			ourLog.info("미병합 데이터의 대한 동작 없음.");
			ourLog.info("-------------------------------------------------------");
			return ruleNode;
		}
		if(ruleNode.isMergedNode()){
			// <MERGE> : 하위 행 일괄 복사 시키기
			ourLog.debug(" ㄴ 해당" + ruleNode.getRule() + " 은 Merge Node 입니다.");
			RuleNode copyMainRule = ruleNode.copyNode();
			List<RuleNode> childCopyNodeList = new ArrayList<>();
			for(int i = 0; mergeCount > i ; i++){
				RuleNode copyedNode = MapperUtils.fullCopyRuleNode(ruleNode);
				copyedNode = exchangeReferenceNameForArray(copyedNode, i+1, metaRule.getMergeDataKey()); // 1번행부터 시작 _1 ~ _n
				childCopyNodeList.addAll(copyedNode.getChildren());
				ourLog.debug("  ㄴ 이 노드를 복사하였음. " + i);
				// test
				MapperUtils.printRuleAndRuleTypeInNodeTree(copyedNode);
			}
			copyMainRule.setChildren(childCopyNodeList);
			return copyMainRule;
		}else{
			ourLog.debug(" ㄴ 해당" + ruleNode.getRule() + " 은 Merge Node 가 아닙니다.");
			int childNodeSize = childNodeList.size();
			List<RuleNode> newChildNodeList = new ArrayList<>();
			// 다른 것들은 하위 탐색하면서 대상이면 해당 노드 아래를 N행만큼 카피하기
			for(int i = 0; childNodeSize > i; i++){
				if(childNodeList.get(i).getRuleType().equals(RuleType.TRANS)){
					String childRuleSourceReference = childNodeList.get(i).getSourceReferenceNm();
					boolean isExchageTarget = false;
					for(String eachMergeKey : metaRule.getMergeDataKey()){
						if(childRuleSourceReference.contains(eachMergeKey)){
							isExchageTarget = true;
							break;
						}
					}
					if(isExchageTarget){
						// 병합된 row 카운트만큼 Node 추가
						int countOfCopy = source.getInt("merged_row_count");
						for(int j = 0; countOfCopy >= j; j++){
							RuleNode nodeCopy = MapperUtils.fullCopyRuleNode(childNodeList.get(i));
							nodeCopy = exchangeReferenceNameForArray(nodeCopy, j+1, metaRule.getMergeDataKey()); // 1번행부터 시작 _1 ~ _n
							newChildNodeList.add(nodeCopy);
						}
					}else{
						// 병합이 필요없음.
						newChildNodeList.add(childNodeList.get(i));
					}

				}else if(childNodeList.get(i).getTransactionType().equals(TransactionType.CREATE_SINGLESTRING)) {
					newChildNodeList.add(childNodeList.get(i));

				}else if(childNodeList.get(i).getChildren().size() >= 1){
					List<RuleNode> nodeList = childNodeList;
					newChildNodeList.add(createTreeForArrayWithRecursive(metaRule, childNodeList.get(i), source));
				}else{
					ourLog.error("중요) 해당 유형의 맵의 대하여 데이터 복제 관련 정책 구성이 필요로 합니다...! ");
					printRuleAndRuleTypeInNodeTree(childNodeList.get(i));
					ourLog.error("-----------------------------------------------------------------------");
				}
			}
			ruleNode.setChildren(newChildNodeList);

			ourLog.info("-------------------------------------------------------");

			return ruleNode;
		}
	}

	/**
	 * Source에 들어가는 JsonObject에서 _n 으로 구성된 Array 가 존재한다면
	 * 그 사이즈를 반환한다.
	 *
	 * 존재하지 않으면 -1 을 반환한다.
	 *
	 * @param key          the key
	 * @param source       the source
	 * @return the int
	 */
	public static int getSizeOfJSONObjectHasArray(String key, JSONObject source){
		for(int arrange = 1; arrange <= 999; arrange++){ // 안정성에 따라 999행 이상은 merge 될 수 없음.
			String chkCol = key + "_" + arrange;

			// 소스에서 배열 데이터로 변환되었는지 확인하기
			Iterator<String> sourcekeysIteration = source.keys();
			boolean isCotainInSourceData = false;
			while(sourcekeysIteration.hasNext()){
				String eachSourceKey = sourcekeysIteration.next();
				if(eachSourceKey.equals(chkCol)){
					// 단순 복사의 경우 source == copyRow
					isCotainInSourceData = true;
					break;
				}
			}

			if(!isCotainInSourceData && arrange == 1){
				// 없음
				return -1;
			}else if(!source.has(chkCol)){
				ourLog.info(" [DEV] MapperUtil getSizeOfJSONObjectHasArray  / it has " + arrange + "   this key : " + key);
				// 특정 행 위치
				return arrange;
			}else{
				// 있으니 유지
			}
		}
		return -1;
	}


	/**
	 * 함수형 데이터의 결과에서 파라미터에 값들을 반환해준다.
	 *
	 * @param input the input
	 * @return the list
	 */
	public static List<String> extractMultipleValues(String input) {
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

	/**
	 * 하위노드를 전체복사한다.
	 *
	 * @param ruleNode the rule node
	 * @return the rule node
	 */
	public static RuleNode fullCopyRuleNode(@NonNull RuleNode ruleNode){
		if(ruleNode.getChildren().size() <= 0){
				return ruleNode.copyNode();
		}else{
			RuleNode copiedParentNode = ruleNode.copyNode();
			List<RuleNode> copiedChildNodeList = new ArrayList<>();
			for(RuleNode eachChildRuleNode : ruleNode.getChildren()){
				copiedChildNodeList.add(fullCopyRuleNode(eachChildRuleNode));
			}
			copiedParentNode.setChildren(copiedChildNodeList);
			return copiedParentNode;
		}
	}

	/**
	 * 2024. 03. 08.
	 * Array 의 대한 배열형 데이터컬럼으로 일괄 변경한다.
	 *
	 * @return the rule node
	 */
	public static RuleNode exchangeReferenceNameForArray(RuleNode ruleNode, int mergedBoundNum, Set<String> mergeKeySet){
		if(ruleNode.getChildren().size()<=0){
			if(ruleNode.getSourceReferenceNm() != null){
				ruleNode.setSourceReferenceNm(
					bindFunctionReferenceToArrayReference(
						ruleNode.getSourceReferenceNm(),
						mergedBoundNum,
						mergeKeySet
					)
				);
			}
		}else{
			for(RuleNode parentNode : ruleNode.getChildren()){
				exchangeReferenceNameForArray(parentNode, mergedBoundNum, mergeKeySet);
			}
		}
		return ruleNode;
	}

	/**
	 * 2024. 03. 08.
	 * MERGE( a, b, c, '', d) 의 referenceRule 중에
	 * a, b 가 mergeSet 이라면
	 * MERGE( a_"mergeBoundNum", b_"mergeBoundNum", c, '', d) 로 바꿔서 반환한다.
	 * ex) MERGE( a_1, b_1, c, '', d)
	 *
	 * @param referenceRule  the reference rule
	 * @param mergedBoundNum the merged bound num
	 * @param mergeKeySet       the merge set
	 */
	public static String bindFunctionReferenceToArrayReference(String referenceRule, int mergedBoundNum, Set<String> mergeKeySet){
		//ourLog.info("---- execute Reference Change... ----");
		//ourLog.info("before referenceRule : " + referenceRule + "  mergedBoundNum : " + mergedBoundNum + " mergeKeySet : " + mergeKeySet);
		for(String mergeKey : mergeKeySet){
			if(referenceRule.contains(mergeKey)){
				referenceRule = referenceRule.replaceAll(mergeKey, mergeKey+"_"+mergedBoundNum);
			}
		}
		//ourLog.info("replaced Key = : " + referenceRule);
		//ourLog.info("--------------------------------------");
		return referenceRule;
	}
}

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
		System.out.print(" > " + nd.getRuleType() + " : " + nd.getTransactionType() + " / " + nd.getRule() + "  || source : " + nd.getSourceReferenceNm() + " || target : "+ nd.getTargetElementNm());
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

	/**
	 * 2024. 02. 08. Array 대응 목적으로 구성한 
	 * 사용자 입력의 Array 값이 param_v1 ... vn 으로 입력되는 경우 
	 * 같은 구조로 Tree의 크기를 그만큼 키워주는 동작 수행
	 *
	 * @param ruleNodeList the rule node list
	 * @param source       the source
	 * @return the list
	 */
	// 순환함수
	// TODO.
	// ★ 보완필요. ruleNodeList, source, mergeKeySet
	// 3가지 값을 활용하여 AdditionTree 구성
	//
	public static List<RuleNode> createAdditionTreeForArray(List<RuleNode> ruleNodeList, JSONObject source, Set<String> mergeKey){
		List<RuleNode> retRuleNodeList = new ArrayList<>();
		for (RuleNode eachRuleNode : ruleNodeList){
			// arrayRule만 수행
			if (!eachRuleNode.getRuleType().equals(RuleType.CREATE_ARRAY)){
				retRuleNodeList.add(eachRuleNode);
				continue;
			}

			// 2024. 02. 07. Array 의 대한 Array 갯수만큼의 Rule Node 추가 기능 구성
			// 카운트 갯수는 array create Rule 바로 아래가 index로 됨
			List<RuleNode> createdChildNodeList = new ArrayList<>();
			List<RuleNode> sourceChildNodeList = new ArrayList<>();
			// swap
			if(sourceChildNodeList.size() <= 0){
				for(RuleNode originalNode : eachRuleNode.getChildren()){
					sourceChildNodeList.add(originalNode);
				}
			}

			boolean isUsermuxInputRule = false;
			for (int i = 1; i < sourceChildNodeList.size(); i++){
				String searchIndexSourceReferenceName = sourceChildNodeList.get(0).getSourceReferenceNm() + "_" + i;

				if (source.has(searchIndexSourceReferenceName)) {
					// Array Create Rule Node에 내부에 가진 Sourceref를 바꾼 뒤 추가 적재
					for(RuleNode originalNode : sourceChildNodeList){
						RuleNode copyNode = new RuleNode(originalNode.getParent(), originalNode.getRule(), originalNode.getLevel(), originalNode.isIdentifierNode(), originalNode.isMergedNode());
						RuleNode newArrayRule = createRuleSourceReferenceForArrayTypeData(copyNode, source, "_" + i);
						createdChildNodeList.add(newArrayRule);
					}
				}else{
					isUsermuxInputRule = true;
				}
			}

			if(isUsermuxInputRule){
				ourLog.info("----- createdChildNodeList size : " + createdChildNodeList.size());
				eachRuleNode.setChildren(createdChildNodeList);
			}else{
				// no active.
			}
			retRuleNodeList.add(eachRuleNode);
		}

		return retRuleNodeList;
	}

	/**
	 * 이진트리를 recursive 하게 수행하면서 병합되어있는 행의 source 정보에 대하여
	 * map에 반영시켜준다.
	 *
	 * @param ruleNode the rule node
	 * @param source   the source
	 * @return the rule node
	 */
// div conq
	public static RuleNode createTreeForArrayWithRecursive(RuleNode ruleNode, JSONObject source) throws JSONException {
		List<RuleNode> childNodeList = ruleNode.getChildren();
		MapperUtils.printRuleAndRuleTypeInNodeTree(ruleNode);

		// <MERGE> -> [{} {} {}]
		if(ruleNode.isMergedNode()){
			System.out.println("이 룰은 MERGE 기반 룰입니다. 해당 룰의 child 룰은 N차행 구조를 가지지말고, ");
			System.out.println("전체 소스 Row 병합수(source.merged_row_count)만큼 child와 함께 해당 노드 상위노드에 복제해서 넣으면 됌.");
			System.out.println("아래 for문에서 그렇게 처리하면 될 것으로 보임.");
			RuleNode copyMainRule = ruleNode.copyNode();
			List<RuleNode> childCopyNodeList = new ArrayList<>();
			for(int i = 0; source.getInt("merged_row_count") > i ; i++){
				RuleNode copyedNode = MapperUtils.fullCopyRuleNode(copyMainRule, i).copyNode();
				childCopyNodeList.addAll(copyedNode.getChildren());
				System.out.println("  ㄴ 이 노드를 복사하였음. " + i);
			}
			copyMainRule.setChildren(childCopyNodeList);
			return copyMainRule;
		}else{
			System.out.println("이 룰은 MERGE 기반 룰이 아닙니다.");
		}

		// {} -> [{} {}]
		int childNodeSize = childNodeList.size();
		List<RuleNode> newChildNodeList = new ArrayList<>();
		for(int i = 0; childNodeSize > i; i++){

			System.out.println("---------------------------------------------");
			System.out.println("NOW : " + childNodeList.get(i).getSourceReferenceNm());

			if(childNodeList.get(i).getRuleType().equals(RuleType.TRANS)){
				String childRuleSourceReference = childNodeList.get(i).getSourceReferenceNm();
				TransactionType childRuleTrasType = childNodeList.get(i).getTransactionType();
				// new
				System.out.println("childNodeList.get(i) is TransType.. then execute this : " + childRuleSourceReference);
				int countOfCreateNode = 1;
				if(childRuleTrasType.equals(TransactionType.COPY)){
					countOfCreateNode = getSizeOfJSONObjectHasArray(childRuleSourceReference, source);
					// act
					if(countOfCreateNode >= 1){
						System.out.println(countOfCreateNode + " count found then.. ");
						int createNodeSize = 1;
						while(countOfCreateNode > createNodeSize){
							RuleNode newRule = childNodeList.get(i).copyNode();
							newRule.setSourceReferenceNm(newRule.getSourceReferenceNm() + "_" + createNodeSize);

							newChildNodeList.add(newRule);
							createNodeSize++;
						}
					}else{
						newChildNodeList.add(childNodeList.get(i));
					}
				}else{
					List<String> ruleParamList = MapperUtils.extractMultipleValues(childRuleSourceReference);
					ourLog.info("    >>>> ruleNode.getSourceReferenceNm() : " + childRuleSourceReference + " size : " + ruleParamList.size());
					boolean isPureNoArrayFunction = true;
					for(String arg : ruleParamList){
						ourLog.info("   >>>>>>>>>>>>>> Now Param is : " + arg);
						countOfCreateNode = getSizeOfJSONObjectHasArray(arg, source);
						ourLog.info("   >>>>>>>>>>>>>> Found : " + arg + "      count : " + countOfCreateNode);
						if(countOfCreateNode >= 1){
							System.out.println(countOfCreateNode + " count found then.. ");
							int createNodeSize = 1;
							// 해당 행만큼 노드 생성
							while(countOfCreateNode > createNodeSize){
								RuleNode newRule = childNodeList.get(i).copyNode();
								String replaceExchagneParam = arg +  "_" + createNodeSize;
								ourLog.info(" [dev] Replace this ..... !!! >>> " + arg + " to " + replaceExchagneParam + " in " + newRule.getSourceReferenceNm());
								newRule.setSourceReferenceNm(newRule.getSourceReferenceNm().replaceAll(arg, replaceExchagneParam));
								newChildNodeList.add(newRule);
								createNodeSize++;
								isPureNoArrayFunction = false;
							}
						}
					}
					if(isPureNoArrayFunction){
						newChildNodeList.add(childNodeList.get(i));
					}
				}
			}else{ // create 계열
				// create 중 'a', 'b' 같은 단순 String의 경우
				if(childNodeList.get(i).getTransactionType().equals(TransactionType.CREATE_SINGLESTRING)){
					newChildNodeList.add(childNodeList.get(i));
				}

				// 룰이 하위 룰을 가지는 케이스인 경우 recursive for Swap
				if(childNodeList.get(i).getChildren().size() >= 1){
					List<RuleNode> nodeList = childNodeList;
					// 변환
					newChildNodeList.add(createTreeForArrayWithRecursive(childNodeList.get(i), source));
				}
			}
			// 반복문을 위한 사이즈 갱신
			childNodeSize = childNodeList.size();
		}
		ruleNode.setChildren(newChildNodeList);
		return ruleNode;
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

	// 배열화 된 SourceData의 A_1, A_2 식의 데이터로 구성함에 따라
	// 가변적인 룰 구성을 위해 하위룰 모두에게 append_separator 를 붙여준다.
	// rule.sourceReferenceName + append_separator
	private static RuleNode createRuleSourceReferenceForArrayTypeData(MetaRule metaRule, RuleNode ruleNode, JSONObject source, String append_separator) {
		RuleNode newNode = ruleNode.copyNode();
		ourLog.info("exchange Ref Target Refer Source Data : " + ruleNode.getSourceReferenceNm());

		// a = 'a' 같은경우 _1 append 무시
		RuleType rt = newNode.getRuleType();
		TransactionType tt = newNode.getTransactionType();

		ourLog.info("allocated Rule Type : " + rt.name());
		ourLog.info("each allocated type of transaction : " + tt.name());
	   if (RuleType.CREATE_ARRAY.equals(newNode.getRuleType())){
			ourLog.info("ARRAY IN ARRAY ... ");
			// 해당 행에 대하여 재수행(순환함수)
			createAdditionTreeForArray(newNode.getChildren(), source);
		}else{
			if (tt.equals(TransactionType.COPY_STRING)) {
				// do nothing
			} else if (tt.equals(TransactionType.COPY)) {
				String exchangeReferenceForArray = newNode.getSourceReferenceNm() + append_separator;
				newNode.setSourceReferenceNm(exchangeReferenceForArray);
			} else if (tt.equals(TransactionType.CASE) || tt.equals(TransactionType.DATE) || tt.equals(TransactionType.COPY_WITH_DEFAULT) || tt.equals(TransactionType.SPLIT) || tt.equals(TransactionType.TRANSLATION) || tt.equals(TransactionType.MERGE)){
				// Function 스타일로 구성된 TransactionType 의 대하여 Column -> Column_n 으로 변경
				// ex) FUNCTION(a, '', b) -> FUNCTION(a_1, '', a_2)
				List<String> argumentParam = extractMultipleValues(ruleNode.getSourceReferenceNm());
				String exchangeRef = argumentParam.get(0) + append_separator;
				newNode.setSourceReferenceNm(newNode.getSourceReferenceNm().replaceAll(argumentParam.get(0), exchangeRef));
			}
		}

		List<RuleNode> ruleNodeList = new ArrayList<>();
		for(RuleNode eachChangeNode : newNode.getChildren()){
			RuleNode node = createRuleSourceReferenceForArrayTypeData(eachChangeNode, source, append_separator);
			ruleNodeList.add(node);
		}
		newNode.setChildren(ruleNodeList);

		return newNode;
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
		if(ruleNode.getChildren().size() <= 0){
				ruleNode.setSourceReferenceNm(bindFunctionReferenceToArrayReference(
					ruleNode.getSourceReferenceNm(),
					mergedBoundNum,
					mergeKeySet
				));
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
		for(String mergeKey : mergeKeySet){
			referenceRule.replaceAll(mergeKey, mergeKey+"_"+mergedBoundNum);
		}
		return referenceRule;
	}
}

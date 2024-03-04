package ca.uhn.fhir.jpa.starter.transfor.base.util;

import ca.uhn.fhir.jpa.starter.transfor.base.code.ErrorHandleType;
import ca.uhn.fhir.jpa.starter.transfor.base.code.RuleType;
import ca.uhn.fhir.jpa.starter.transfor.base.code.TransactionType;
import ca.uhn.fhir.jpa.starter.transfor.base.core.TransformEngine;
import ca.uhn.fhir.jpa.starter.transfor.base.map.MetaRule;
import ca.uhn.fhir.jpa.starter.transfor.base.map.ReferenceNode;
import ca.uhn.fhir.jpa.starter.transfor.base.map.ReferenceParamNode;
import ca.uhn.fhir.jpa.starter.transfor.base.map.RuleNode;
import org.checkerframework.common.value.qual.IntRange;
import org.jetbrains.annotations.Range;
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

			boolean isArray = false;
			if(line.contains("(Array)") || line.contains("(ARRAY)") || line.contains("(array)")){
				isArray = true;
			}else{
				isArray = false;
			}

			// 맵 구조화
			RuleNode ruleNode = new RuleNode(null, line.trim(), level, isIdentifierNode, isArray);
			
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
			}else if(line.contains("mergeKey")){
				String mergeKeySingleStr = line.split("=")[1].trim();
				String[] mergeKeyArray = mergeKeySingleStr.split(",");
				Set<String> mergeKeySet = new HashSet<>();
				for(String arg : mergeKeyArray){
					mergeKeySet.add(arg.trim());
				}
				metaRule.setMergeDataKey(mergeKeySet);
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
	public static List<RuleNode> createAdditionTreeForArray(List<RuleNode> ruleNodeList, JSONObject source){
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
				// 흠...
				String searchIndexSourceReferenceName = sourceChildNodeList.get(0).getSourceReferenceNm() + "_" + i;

				if (source.has(searchIndexSourceReferenceName)) {
					// Array Create Rule Node에 내부에 가진 Sourceref를 바꾼 뒤 추가 적재
					for(RuleNode originalNode : sourceChildNodeList){
						RuleNode copyNode = new RuleNode(originalNode.getParent(), originalNode.getRule(), originalNode.getLevel(), originalNode.isIdentifierNode(), false);
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

	// div conq
	public static RuleNode createTreeForArrayWithRecursive(RuleNode ruleNode, JSONObject source){
		List<RuleNode> childNodeList = ruleNode.getChildren();
		System.out.println("active Start... childNode Size : " + childNodeList.size());

		MapperUtils.printRuleAndRuleTypeInNodeTree(ruleNode);

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
					System.out.println("하위 존재에 따른 처리 수행");
					List<RuleNode> nodeList = childNodeList;
					// 변환
					newChildNodeList.add(createTreeForArrayWithRecursive(childNodeList.get(i), source));
				}
			}
			// 반복문을 위한 사이즈 갱신
			childNodeSize = childNodeList.size();
			System.out.println("------------------------------------------------------- now child nude size : " + childNodeSize);
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

		ourLog.info(" [DEV] MapperUtil getSizeOfJSONObjectHasArray ");

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
	private static RuleNode createRuleSourceReferenceForArrayTypeData(RuleNode ruleNode, JSONObject source, String append_separator) {
		RuleNode newNode = ruleNode.copyNode();
		ourLog.info("exchange Ref Target Refer Source Data : " + ruleNode.getSourceReferenceNm());

		// a = 'a' 같은경우 _1 append 무시
		RuleType rt = newNode.getRuleType();
		TransactionType tt = newNode.getTransactionType();

		ourLog.info(" allocated Rule Type : " + rt.name());
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




}

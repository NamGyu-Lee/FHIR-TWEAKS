package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.jpa.starter.transfor.base.code.RuleType;
import ca.uhn.fhir.jpa.starter.transfor.base.code.TransactionType;
import ca.uhn.fhir.jpa.starter.transfor.base.core.TransformEngine;
import ca.uhn.fhir.jpa.starter.transfor.base.map.MetaRule;
import ca.uhn.fhir.jpa.starter.transfor.base.map.RuleNode;
import ca.uhn.fhir.jpa.starter.transfor.base.util.MapperUtils;
import ca.uhn.fhir.jpa.starter.transfor.util.TransformUtil;
import ca.uhn.fhir.jpa.starter.validation.config.CustomValidationRemoteConfigProperties;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mapstruct.Mapper;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.*;

@SpringBootTest
public class SampleMappingTest {

	String sourceA = "{\n" +
		"  \"test\" : \"true\",\n" +
		"  \"value1\" : 202230,\n" +
		"  \"value2\" : \"-\",\n" +
		"  \"value3\" : 200,\n" +
		"  \"detail\" : M,\n" +
		"  \"code\" : \"소스A의 코드\"\n" +
		"}\n";

	String sourceB = "{\n" +
		"  \"test\" : \"true\",\n" +
		"  \"value1\" : 201130,\n" +
		"  \"value2\" : \"-\",\n" +
		"  \"value3\" : 200,\n" +
		"  \"detail\" : M,\n" +
		"  \"code\" : \"소스B의 코드\"\n" +
		"}\n";

	String sourceC = "{\n" +
		"  \"test\" : \"true\",\n" +
		"  \"value1\" : 202230,\n" +
		"  \"value2\" : \"-\",\n" +
		"  \"value3\" : 200,\n" +
		"  \"detail\" : F,\n" +
		"  \"code\" : \"소스C의 코드\"\n" +
		"}\n";

	String sourceD = "{\n" +
		"  \"test\" : \"true\",\n" +
		"  \"value1\" : 12230,\n" +
		"  \"value2\" : \"-\",\n" +
		"  \"value3\" : 30,\n" +
		"  \"detail\" : F,\n" +
		"  \"code\" : \"소스D의 코드\"\n" +
		"}\n";


	String argMap =
		"* resourceType='Test'\n" +
		"* testv\n" +
		" * (normalStringArray).normalStringArray\n" +  // [ '', '' ]
		"  * 'aaa'\n" +
		"  * 'bbb'\n" +
		"* (test).test\n" + // [ {}, {} ]
		" * value1=value1\n" +
		" * value2=value2\n" +
		" * value3=value3\n"+
		" * value1=value1\n" +
		" * value2=value2\n" +
		" * value3=value3\n"+
		"* (arraytest).arraytest\n"+ // [ {}, {} ] with dynamics
		" * detail=CASE(detail, 'M', 'male', 'F', 'female', 'U', 'unknown')\n"+
		" * code=code\n"+
		" * (testdata).testdata\n"+
		"  * code=code\n"+
		" * (testarray).testarray\n"+
		"  * 'a'\n"+
		"  * 'b'\n"+
		"  * 'c'\n"+
		"  * (arraytest).arraytwo\n"+ // 중복되는 데이터의 관리
		"   * detail=detail\n"+
		"   * code=code\n" +
		"* (arraytest).arraytwo\n"+ // 중복되는 데이터의 관리
		" * detail=detail\n"+
		" * code=code\n"
		;

	String argMap3 =
		"* (test).test\n" + // [ {}, {} ]
			" * value1=value1\n" +
			" * value2=value2\n" +
			" * value3=value3\n"+
			" * value1=value1\n" +
			" * value2=value2\n" +
			" * value3=value3\n"+
			"* (MERGE)(testmerge).testmerge\n" +
			" * detail=detail\n" +
			" * code=code\n"+
			" * (array).hah\n"+
			"  * pu=code\n"+
			"* humm='323'\n"
		;

	@Test
	public void arrayMapTest() throws IOException, JSONException {
		// 변환 맵 구성
		List<RuleNode> nodeList = MapperUtils.createTree(argMap);
		MapperUtils.printRuleAndRuleTypeInNodeTree(nodeList.get(0));

		// meta 에서 생성할 데이터
		Set<String> indexKeySet = Set.of("value1", "value2", "value3");
		Set<String> mergeKeySet = Set.of("detail", "code");
		MetaRule metaRule = new MetaRule();
		metaRule.setMergeDataKey(mergeKeySet);
		metaRule.setCacheDataKey(indexKeySet);

		// source 데이터
		List<JsonElement> elementList = new ArrayList<>();
		JsonParser parser = new JsonParser();

		JsonElement element = parser.parse(sourceA);
		elementList.add(element);

		element = parser.parse(sourceB);
		elementList.add(element);

		element = parser.parse(sourceC);
		elementList.add(element);

		element = parser.parse(sourceD);
		elementList.add(element);

		// 병합
		List<JsonObject> obj = TransformUtil.mergeJsonObjectPattern(indexKeySet, mergeKeySet, elementList);

		// JsonObject -> JSONObject
		String v = obj.get(0).toString();
		System.out.println(v);
		JSONObject sourceObject = new JSONObject(v);

		System.out.println("------------------");
		// 병합된 갯수 세기
		for(String mergeKey : mergeKeySet){
			int count = MapperUtils.getSizeOfJSONObjectHasArray(mergeKey, sourceObject);
			System.out.println(" Merged Count : " + mergeKey + " " + count);
		}

		// 병합 활용 동작
		System.out.println("------------------");
		nodeList.set(0, MapperUtils.createTreeForArrayWithRecursive(metaRule, nodeList.get(0), sourceObject));
		System.out.println("------------------");
		MapperUtils.printRuleAndRuleTypeInNodeTree(nodeList.get(0));

		// ok
	}

	@Test
	public void 데이터변환까지() throws JSONException {
		// meta 에서 생성할 데이터
		Set<String> indexKeySet = Set.of("value1", "value2", "value3");
		Set<String> mergeKeySet = Set.of("detail", "code");
		MetaRule metaRule = new MetaRule();
		metaRule.setMergeDataKey(mergeKeySet);
		metaRule.setCacheDataKey(indexKeySet);

		// source 데이터
		List<JsonElement> elementList = new ArrayList<>();
		JsonParser parser = new JsonParser();

		JsonElement element = parser.parse(sourceA);
		elementList.add(element);

		element = parser.parse(sourceB);
		elementList.add(element);

		element = parser.parse(sourceA);
		elementList.add(element);

		element = parser.parse(sourceA);
		elementList.add(element);

		// 병합
		List<JsonObject> obj = TransformUtil.mergeJsonObjectPattern(indexKeySet, mergeKeySet, elementList);

		// 엔진 스타트
		CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties = new CustomValidationRemoteConfigProperties();
		TransformEngine engine = new TransformEngine(null, customValidationRemoteConfigProperties);

		// step 2 test
		// 변환
		for (JsonObject eachObj : obj){
			// 매 회별 변환 맵 구성
			//List<RuleNode> nodeList = MapperUtils.createTree(argMap);
			List<RuleNode> nodeList = MapperUtils.createTree(argMap);
			MapperUtils.printRuleAndRuleTypeInNodeTree(nodeList.get(0));

			// 회별 source Data
			String v = eachObj.toString();
			System.out.println(v);
			JSONObject sourceObject = new JSONObject(v);

			// 병합 활용 맵 재생성
			int ac = 0;
			for(RuleNode mainNodeList : nodeList){
				System.out.println("------------------");
				nodeList.set(ac++, MapperUtils.createTreeForArrayWithRecursive(metaRule, mainNodeList, sourceObject));
				System.out.println("------------------");
			}

			String eachObjToStr = eachObj.toString();
			System.out.println(eachObjToStr);
			JSONObject eachSourceObject = new JSONObject(eachObjToStr);
			try {
				engine.transformDataToResource(nodeList, eachSourceObject);
			}catch(Exception e){
				System.out.println("ERR ---------------------------------------------");
			}
		}

		// ok. 2024 02 28
	}

	@Test
	public void Merge활용로직구성() throws JSONException{
		// meta 에서 생성할 데이터
		Set<String> indexKeySet = Set.of("value1", "value2", "value3");
		Set<String> mergeKeySet = Set.of("detail", "code");

		// source 데이터
		List<JsonElement> elementList = new ArrayList<>();
		JsonParser parser = new JsonParser();

		JsonElement element = parser.parse(sourceA);
		elementList.add(element);

		element = parser.parse(sourceB);
		elementList.add(element);

		element = parser.parse(sourceC);
		elementList.add(element);

		element = parser.parse(sourceC);
		elementList.add(element);

		element = parser.parse(sourceC);
		elementList.add(element);

		element = parser.parse(sourceB);
		elementList.add(element);

		element = parser.parse(sourceD);
		elementList.add(element);

		// 병합
		List<JsonObject> obj = TransformUtil.mergeJsonObjectPattern(indexKeySet, mergeKeySet, elementList);

		// 엔진 스타트
		CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties = new CustomValidationRemoteConfigProperties();
		TransformEngine engine = new TransformEngine(null, customValidationRemoteConfigProperties);

		// step 2 test
		// 변환
		for (JsonObject eachObj : obj){
			// 매 회별 변환 맵 구성
			List<RuleNode> nodeList = MapperUtils.createTree(argMap);
			MapperUtils.printRuleAndRuleTypeInNodeTree(nodeList.get(0));

			// 회별 source Data
			String v = eachObj.toString();
			System.out.println(v);
			JSONObject sourceObject = new JSONObject(v);

			// copy before
			System.out.println("-------Copy Befre------------------------------");
			MapperUtils.printRuleAndRuleTypeInNodeTree(nodeList.get(0));
			System.out.println("-------Copy After------------------------------");

			// copy after
			RuleNode headerNode = nodeList.get(0).copyNode();
			RuleNode node1 = MapperUtils.fullCopyRuleNode(nodeList.get(0));
			RuleNode node2 = MapperUtils.fullCopyRuleNode(nodeList.get(0));
			RuleNode node3 = MapperUtils.fullCopyRuleNode(nodeList.get(0));
			List<RuleNode> nodeList1 = new ArrayList<>();
			nodeList1.add(node1);
			nodeList1.add(node2);
			nodeList1.add(node3);
			headerNode.setChildren(nodeList1);

			MapperUtils.printRuleAndRuleTypeInNodeTree(headerNode);
			nodeList.set(0, headerNode);
			System.out.println("-------END------------------------------");
		}
	}


	String argMap2 =
	"* resourceType='Test'\n" +
	"* (<)MERGE)(testv).testv\n" +
	" * (normalStringArray).normalStringArray\n" +  // [ '', '' ]
	"  * 'aaa'\n" +
	"  * 'bbb'\n"
	;

	@Test
	public void 맵테스트(){
		// 매 회별 변환 맵 구성
		List<RuleNode> nodeList = MapperUtils.createTree(argMap2);
		MapperUtils.printRuleAndRuleTypeInNodeTree(nodeList.get(0));
		int nowRow = 0;
		for(RuleNode eachNode : nodeList){
			if(eachNode.getRuleType().equals(RuleType.CREATE_ARRAY)){
				// copy before
				System.out.println("-------Copy Befre------------------------------");
				MapperUtils.printRuleAndRuleTypeInNodeTree(eachNode);
				System.out.println("-------Copy After------------------------------");
				// copy after
				RuleNode headerNode = eachNode.copyNode();
				MapperUtils.printRuleAndRuleTypeInNodeTree(headerNode);
				RuleNode node1 = MapperUtils.fullCopyRuleNode(eachNode);
				System.out.println("  ㄴ copy Test");
				MapperUtils.printRuleAndRuleTypeInNodeTree(node1);
				System.out.println("-------------------------------------");
				RuleNode node2 = MapperUtils.fullCopyRuleNode(eachNode);
				RuleNode node3 = MapperUtils.fullCopyRuleNode(eachNode);
				List<RuleNode> nodeList1 = new ArrayList<>();
				nodeList1.addAll(node1.getChildren());
				nodeList1.addAll(node2.getChildren());
				nodeList1.addAll(node3.getChildren());
				headerNode.setChildren(nodeList1);

				nodeList.set(nowRow, headerNode);
				MapperUtils.printRuleAndRuleTypeInNodeTree(headerNode);
				nowRow ++;
				System.out.println("-------END------------------------------");
			}
		}
	}

	@Test
	public void 간단한iterationTest() throws JSONException{
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("name", "John Doe");
		jsonObject.put("age", 30);
		jsonObject.put("city", "New York");

		Iterator<String> keys = jsonObject.keys();
		// 키 순회 및 출력
		while(keys.hasNext()) {
			System.out.println(keys.next());
		}
	}

}

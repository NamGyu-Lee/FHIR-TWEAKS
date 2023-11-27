package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.starter.transfor.base.code.RuleType;
import ca.uhn.fhir.jpa.starter.transfor.base.code.TransactionType;
import ca.uhn.fhir.jpa.starter.transfor.base.core.TransformEngine;
import ca.uhn.fhir.jpa.starter.transfor.base.map.ActivateTransNode;
import ca.uhn.fhir.jpa.starter.transfor.base.map.RuleNode;
import ca.uhn.fhir.jpa.starter.transfor.base.util.MapperUtils;
import ca.uhn.fhir.jpa.starter.transfor.base.util.RuleUtils;
import ca.uhn.fhir.jpa.starter.validation.config.CustomValidationRemoteConfigProperties;
import com.google.gson.JsonObject;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.base.Sys;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Patient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;


import java.util.*;

/**
 *  Map 을 활용하여 테스트
 */
public class StructureMapCastingMapTest {

	TransformEngine transformEngine;

	private String map = "* a = $test1" +
		"* b = $state" +
		"* (address).address" +
		"  * city = $city_name" +
		"  * line = $line" +
		"  * postalCode = $zipCode";

	private String map2 =
		"Instance: $pid := $uuid()\n" +
			"InstanceOf: Patient\n" +
			"* identifier\n" +
			" * system = $urn\n" +
			" * value = 'urn:uuid:' & $pid\n" +
			"* identifier\n" +
			" * system = $exampleMrn\n" +
			" * value = mrn\n" +
			"* identifier\n" +
			" * system = $ssn\n" +
			" * value = ssn\n" +
			"* identifier\n" +
			" * system = $passportPrefix & passport_country\n" +
			" * value = passport_number\n" +
			"* active = status='active'\n" +
			"* name\n" +
			" * given = first_name\n" +
			" * family = last_name\n" +
			"* birthDate = birth_date\n" +
			"* gender = $translate(sex, 'gender')\n" +
			"* (address).address\n" +
			" * city = city_name\n" +
			" * state = state\n" +
			" * country = 'USA'\n" +
			" * line = $join([$string(house_number),street_name], ' ')\n" +
			" * postalCode = zip_code\n" +
			" * extension\n" +
			"  * url = $extGeolocation\n" +
			"  * extension\n" +
			"   * url = 'latitude'\n" +
			"   * valueDecimal = lat\n" +
			"  * extension\n" +
			"   * url = 'longitude'\n" +
			"   * valueDecimal = long\n" +
			"* (phones).telecom\n" +
			" * system = 'phone'\n" +
			" * value = number\n" +
			" * use = (type='HOME'?'home':type='CELL'?'mobile')\n" +
			"* generalPractitioner\n" +
			" * identifier\n" +
			"  * value = primary_doctor.license\n" +
			"  * type.coding\n" +
			"   * system = 'http://terminology.hl7.org/CodeSystem/v2-0203'\n" +
			"   * code = 'MD'\n" +
			" * display = primary_doctor.full_name\n" +
			" * reference = $literal('Practitioner?identifier='&primary_doctor.license)";

	private String map3 = "* mother\n" + " * inner1\n" + " * inner2.1\n" + "  * inner2.1.1\n" + "   * inner2.1.1.1\n" + "    * inner4=$b1\n"
		+ "    * inner5=$b1\n" + "  * sam1=$b1\n" + "    * sam1.1=$b1\n" + " * sam3=$b1\n" + "* mother2=$b1\n"
		;

	private String map4 = "* act1\n" +
		" * act1.1\n" +
		"  * act1.1.1=$a1\n" +
		"  * act1.1.2=$a2\n" +
		" * act1.2\n" +
		"  * act1.2.1=$b1\n" +
		" * act1.3=$b1\n" +
		"  * act1.3.1=$b1\n" +
		"  * act1.3.2=$b1\n" +
		"  * act1.3.3=$b1\n" +
		"  * act1.3.4=$b1\n" +
		"* act2\n" +
		" * act2.1=$c1";

	@Test
	void 변환맵만들기() {
		// 1. 맵 루틴화
		String script = map3;
		List<RuleNode> ruleNodeList = MapperUtils.createTree(script);
		for(RuleNode eachRuleNode : ruleNodeList){
			// DFS 기반 맵서칭
			MapperUtils.printRuleTextInNodeTree(0, eachRuleNode);
		}
	}
	
	@Test
	void 일차원변환맵_치환하기(){
		String script = map3;
		List<RuleNode> ruleNodeList = MapperUtils.createTree(script);

		JSONObject sourceObject = new JSONObject();
		JSONObject targetObject = new JSONObject();

		try {
			sourceObject.put("$a1", "322");

			for (RuleNode eachRuleNode : ruleNodeList) {
				targetObject = transformEngine.executeRule(eachRuleNode, sourceObject);
			}
		}catch(org.json.JSONException e){

		}
		System.out.println(targetObject.toString());
	}

	@Test
	void 단일룰_전체_실행하기() throws JSONException{
		String script = map4;
		List<RuleNode> ruleNodeList = MapperUtils.createTree(script);

		JSONObject sourceObject = new JSONObject();
		JSONObject targetObject = new JSONObject();

		try {
			sourceObject.put("$a1", "a1val123");
			sourceObject.put("$a2", "a2val456");
			sourceObject.put("$b1", "b1val4442");
			sourceObject.put("$c1", "vava11");

			for (RuleNode eachRuleNode : ruleNodeList) {
				ActivateTransNode activateTransNode = new ActivateTransNode();
				activateTransNode.setRuleNode(eachRuleNode);
				activateTransNode.setSource(sourceObject);
				activateTransNode.setTarget(targetObject);
				ActivateTransNode ret = transformEngine.recursiveActTransNode(activateTransNode);
				System.out.println(" ▶ Active Result Per Rules : " + ret.getTarget().toString());
			}
		}catch(org.json.JSONException e){
			e.printStackTrace();
		}
		System.out.println(targetObject.toString());
	}

	String map5 = "* (phones).telecom\n" +  "* (phones).telecom2\n" +  "* (phones).telecom3\n" + " * test1"
//		" * system = type\n" +
//		" * value = number";
		;

	@Test
	void 변환맵_배열구조_만들기() throws org.json.JSONException {
		// 1. 맵 루틴화
		String script = map5;
		List<RuleNode> ruleNodeList = MapperUtils.createTree(script);
		for(RuleNode eachRuleNode : ruleNodeList){
			// DFS 기반 맵서칭
			MapperUtils.printRuleTextInNodeTree(0, eachRuleNode);
		}

		// 2. executeRule
		for(RuleNode eachRuleNode : ruleNodeList){
			System.out.println(transformEngine.executeRule(eachRuleNode, new JSONObject()).toString());
		}
	}

	@Test
	void 소스구성_정의하기() throws JSONException{
		JSONObject sourceObject_1 = new JSONObject();
		JSONObject sourceObject_2 = new JSONObject();

		JSONArray array = new JSONArray();
		sourceObject_1.put("number", "01063627991");
		sourceObject_1.put("type", "Phone");
		array.put(sourceObject_1);
		sourceObject_2.put("number", "01041414422");
		sourceObject_2.put("type", "Home");
		array.put(sourceObject_2);

		JSONObject sourceObject = new JSONObject();
		sourceObject.put("phone", array);
		System.out.println(sourceObject.toString());
	}

	String map7 = "* resourceType='Patient'\n" +
		"* id=id\n" +
		"* meta\n" +
		" * (profile).profile\n" +
		"  * 'http://connectdtx.kr/fhir/StructureDefinition/connectdtx-patient'\n" +
		"  * 'http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-patient'\n" +
		"* (identifier).identifier\n" +
		" * type\n" +
		"  * (coding).coding\n" +
		"   * system='http://terminology.hl7.org/CodeSystem/v2-0203-1'\n" +
		"   * code=code\n" +
		" * system='http://www.hl7korea.or.kr/Identifier/hira-krnpi'\n" +
		" * value=value\n" +
		" * type\n" +
		"  * (coding).coding\n" +
		"   * system='http://terminology.hl7.org/CodeSystem/v2-0203-2'\n" +
		"   * code=code\n" +
		" * system='http://www.hl7korea.or.kr/Identifier/hira-krnpi'\n" +
		" * value=value\n" +
		"* active='true'\n" +
		"* (name).name\n" +
		" * text=hng_nm\n"+
		" * given=eng_nm\n"+
		"* (telecom).telecom\n"+
		" * system=NULLTHEN(telType, 'phone')\n"+
		" * value=telno\n"+
		"* gender=sex_cd\n"+
		"* birthDate=brth_dd\n"+
		"* (address).address\n"+
		" * text=detl_addr\n"+
		" * postalCode=zipcd\n"+
		"* managingOrganization\n"+
		" * reference=$organization_Id"
		;

	String sourceMap = "{\n" +
		"  \"resourceType\": \"Patient\",\n" +
		"  \"id\": \"123\",\n" +
		"  \"code\": \"v1Code\",\n" +
		"  \"value\": \"codevalue\",\n" +
		"  \"pspt_no\": \"-\",\n" +
		"  \"proc_corp_cd\": \"11100338\",\n" +
		"  \"relign_cd\": \"-\",\n" +
		"  \"nati_cd\": \"-\",\n" +
		"  \"inst_cd\": \"012\",\n" +
		"  \"telno\": \"01000000000\",\n" +
		"  \"hosp_nm\": \"가톨릭대학교 서울성모병원\",\n" +
		"  \"forger_yn\": \"N\",\n" +
		"  \"detl_addr\": \"-\",\n" +
		"  \"pid\": \"36715224\",\n" +
		"  \"addr\": \"-\",\n" +
		"  \"brth_dd\": \"1947-01-01\",\n" +
		"  \"sex_cd\": \"male\",\n" +
		"  \"home_telno\": \"-\",\n" +
		"  \"eng_nm\": \"장호균\",\n" +
		"  \"hng_nm\": \"장호균\",\n" +
		"  \"hosp_addr\": \"서울 서초구  반포대로222(반포4동)\",\n" +
		"  \"prtb_telno\": \"010-0000-0000\",\n" +
		"  \"zipcd\": \"616231\",\n" +
		"  \"$organization_Id\": \"Organization/CMC012\"\n" + // Active 시 파라미터에 접붙이기
		"}";

	@Test
	void 실제_동작시켜보기() throws JSONException{
		// 해당 테스트는 진행 제대로하려면 실제 서버위에서 DI로 실행시켜야해서 생략
		CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties = new CustomValidationRemoteConfigProperties();
		customValidationRemoteConfigProperties.setLocalURL("http://localURL:8080/");
		customValidationRemoteConfigProperties.setRemoteTerminologyYn(false);

		transformEngine = new TransformEngine(customValidationRemoteConfigProperties);
		System.out.println("Transform Engine Start... ! ");

		String script = map7;
		List<RuleNode> ruleNodeList = MapperUtils.createTree(script);
		JSONObject targetObject = new JSONObject();
		try {
			JSONObject retJsonObject = new JSONObject();
			JSONObject sourceObj = new JSONObject(sourceMap);
			for (RuleNode eachRuleNode : ruleNodeList) {
				ActivateTransNode activateTransNode = new ActivateTransNode();
				activateTransNode.setRuleNode(eachRuleNode);
				activateTransNode.setSource(sourceObj);
				activateTransNode.setTarget(targetObject);
				ActivateTransNode ret = transformEngine.recursiveActTransNode(activateTransNode);
				System.out.println(" ▶ Active Result Per Rules : " + ret.getTarget().toString());

				RuleUtils.mergeJsonObjects(retJsonObject, ret.getTarget());
			}

			System.out.println("---------------------------------");
			System.out.println(retJsonObject);

			FhirContext context = new FhirContext(FhirVersionEnum.R4);
			IBaseResource pat = context.newJsonParser().parseResource(retJsonObject.toString());
			Patient patient = (Patient) pat;
			System.out.println("answer : " + context.newJsonParser().encodeResourceToString(patient));

		}catch(org.json.JSONException e){
			e.printStackTrace();
		}
		System.out.println(targetObject.toString());
	}

	@Test
	void 맵상태조회() {
		// 1. 맵 루틴화
		String script = map8;
		List<RuleNode> ruleNodeList = MapperUtils.createTree(script);
		for(RuleNode eachRuleNode : ruleNodeList){
			// DFS 기반 맵서칭
			MapperUtils.printRuleAndRuleTypeInNodeTree(eachRuleNode);
		}
	}

	@Test
	void 해당룰_전환_룰_조회(){
		String rule = "'http://connectdtx.kr/fhir/StructureDefinition/connectdtx-patient'";
		RuleType ruleType = RuleUtils.classifyRuleType(rule);
		TransactionType transactionType = RuleUtils.classifyTransactionType(rule);
		String sourceReferenceNm = RuleUtils.getSourceReferenceName(rule);
		if(transactionType.equals(TransactionType.CREATE_SINGLESTRING)){
			System.out.println("[" + ruleType + "]" + "[" + transactionType + "]" + " source Ref : " + sourceReferenceNm);
		}else{
			String targetElementNm = RuleUtils.getTargetElementName(rule);
			System.out.println("[" + ruleType + "]" + "[" + transactionType + "]" + " source Ref : " + sourceReferenceNm + " / Target : " + targetElementNm);

		}
	}

	String map8 = "* (a).A\n" +
		" * fnc\n" +
		"  * consn\n" +
		"   * diver='3'\n" +
		" * fnc\n" +
		"  * consn\n" +
		"   * diver='15'\n"
		;

	// case 1
	String map9 =
		"* identifier\n" +
		" * a='1'\n" +
		"  * b='1'\n" +
		"  * c='1'\n" +
		" * d='1'\n" +
		"* identifier\n" +
		" * a='2'\n" +
		" * b='ab'\n" +
		" * c='de'\n" +
		"* identifier\n" +
		" * (V1).test\n" +
		"  * a='11'\n" +
		"  * a='12'\n" +
		"  * a='13'\n"
		;

	// case 2
	String map10 =
		"* upper\n"+
		" * (Data).data\n" +
		"  * inner_1='32'\n"+
		"  * inner_2='15'\n"+
		"* upper\n"+
			" * (Data).data\n" +
			"  * inner_1='32'\n"+
			"  * inner_2\n"+
			"   * (a).a\n"+
			"    * (lower).lower\n"+
			"     * sam='v1f'\n"+
			"    * (lower).lower\n"+
			"     * sam='v2f'\n"+
			"     * sam='VD'\n"
		;

	@Test
	void 룰_중복시_병합시키기(){
		String script = map10;
		List<RuleNode> ruleNodeList = MapperUtils.createTree(script);

		for(RuleNode eachRuleNode : ruleNodeList){
			// DFS 기반 맵서칭
			MapperUtils.printRuleAndRuleTypeInNodeTree(eachRuleNode);
		}

		JSONObject targetObject = new JSONObject();
		Set<String> namedKeySet = new LinkedHashSet<>();
		try {
			JSONObject retJsonObject = new JSONObject();
			JSONObject sourceObj = new JSONObject(sourceMap);
			for (RuleNode eachRuleNode : ruleNodeList) {
				ActivateTransNode activateTransNode = new ActivateTransNode();
				activateTransNode.setRuleNode(eachRuleNode);
				activateTransNode.setSource(sourceObj);
				activateTransNode.setTarget(targetObject);
				ActivateTransNode ret = transformEngine.recursiveActTransNode(activateTransNode);
				System.out.println(" ▶ Active Result Per Rules : " + ret.getTarget().toString());
				System.out.println("---------------------------------");

				if(retJsonObject.length() != 0){
					// 추가하려는 JSON 의 최상위 노드 조회
					String inputDataHeaderKey = (String)ret.getTarget().keys().next();
					System.out.println( " Already Contain Key Set ::: " + namedKeySet.toString());
					System.out.println( " Search the key >>>> " + inputDataHeaderKey);
					if(namedKeySet.contains(inputDataHeaderKey)){
						// 이미 수행한 적이 있는 Rule이 반복된다면 배열화
						System.out.println( " >>>> " + inputDataHeaderKey + " is Already Contained Then ");
						Object jsonObject = retJsonObject.get(inputDataHeaderKey);
						JSONArray jsonArray = new JSONArray();
						if(jsonObject.getClass().equals(JSONArray.class)){
							// 이미 배열화 된 경우
							System.out.println( " >>>> " + inputDataHeaderKey + " is Already Arrayed");
							jsonArray = (JSONArray)jsonObject;
							jsonArray.put(ret.getTarget().get(inputDataHeaderKey));
						}else{
							// 최초 배열화
							System.out.println( " >>>> " + inputDataHeaderKey + " is to be Array");
							jsonArray.put(jsonObject);
							jsonArray.put(ret.getTarget().get(inputDataHeaderKey));
						}

						JSONObject retObject = new JSONObject();
						retObject.put(inputDataHeaderKey, jsonArray);
						RuleUtils.mergeJsonObjects(retJsonObject, retObject);
					}else{
						System.out.println("can not found the key " + inputDataHeaderKey + " then insert this.");
						namedKeySet.add(inputDataHeaderKey);
						RuleUtils.mergeJsonObjects(retJsonObject, ret.getTarget());
					}
				}else{
					String inputDataHeaderKey = (String)ret.getTarget().keys().next();
					namedKeySet.add(inputDataHeaderKey);
					RuleUtils.mergeJsonObjects(retJsonObject, ret.getTarget());
				}
			}

			System.out.println("---------------------------------");
			System.out.println(retJsonObject);

		}catch(org.json.JSONException e){
			e.printStackTrace();
		}
		System.out.println(targetObject.toString());
	}

	@Test
	void MultiMapTest(){
		MultiValueMap<String, Integer> mvMap = new LinkedMultiValueMap<>();

		mvMap.add("A", 100);
		mvMap.add("A", 200);
		mvMap.add("A", 300);

		List<Integer> a = mvMap.get("A");

		for(int data : a) {
			System.out.print(data + " ");	// output : 100 200 300
		}
	}

	@Before
	void initializeTraransformEngine(){
		CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties = new CustomValidationRemoteConfigProperties();
		customValidationRemoteConfigProperties.setLocalURL("http://localURL:8080/");
		customValidationRemoteConfigProperties.setRemoteTerminologyYn(false);

		transformEngine = new TransformEngine(customValidationRemoteConfigProperties);
		System.out.println("Transform Engine Start... ! ");
	}

	String map11 = "* gender=TRANSLATE(VAVA, 'b')\n";

	@Test
	void 코드성데이터변환작업테스트() throws org.json.JSONException{
		// 해당 테스트는 진행 제대로하려면 실제 서버위에서 DI로 실행시켜야해서 생략
		CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties = new CustomValidationRemoteConfigProperties();
		customValidationRemoteConfigProperties.setLocalURL("http://localURL:8080/");
		customValidationRemoteConfigProperties.setRemoteTerminologyYn(false);

		transformEngine = new TransformEngine(customValidationRemoteConfigProperties);
		System.out.println("Transform Engine Start... ! ");

		String script = map11;
		List<RuleNode> ruleNodeList = MapperUtils.createTree(script);

		for(RuleNode eachRuleNode : ruleNodeList){
			// DFS 기반 맵서칭
			MapperUtils.printRuleAndRuleTypeInNodeTree(eachRuleNode);
		}

		JSONObject source = new JSONObject();
		source.put("VAVA", "VDV");
		transformEngine.transformDataToResource(map11, source);
	}

	String map12 = "* gender=NULLTHEN(VAVA, 'b')\n";

	@Test
	void NUll의경우_DEFAULT넣기() throws org.json.JSONException{
		// 해당 테스트는 진행 제대로하려면 실제 서버위에서 DI로 실행시켜야해서 생략
		CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties = new CustomValidationRemoteConfigProperties();
		customValidationRemoteConfigProperties.setLocalURL("http://localURL:8080/");
		customValidationRemoteConfigProperties.setRemoteTerminologyYn(false);

		transformEngine = new TransformEngine(customValidationRemoteConfigProperties);
		System.out.println("Transform Engine Start... ! ");

		String script = map12;
		List<RuleNode> ruleNodeList = MapperUtils.createTree(script);

		for(RuleNode eachRuleNode : ruleNodeList){
			// DFS 기반 맵서칭
			MapperUtils.printRuleAndRuleTypeInNodeTree(eachRuleNode);
		}

		JSONObject source = new JSONObject();
		source.put("VAVA", "VAA");
		transformEngine.transformDataToResource(map12, source);
	}

	String map13 = "* name\n"+
		" * text=SPLIT(name, 0, 2)\n" +
		" * given=SPLIT(name, 2, 4)\n";

	/*
		" * given=SPLIT(name, 2, 4)\n"+
		"  * vgiven=SPLIT(name, 2, 4)\n"+
		" * text=SPLIT(name, 0, 2)\n"+
		" * given=SPLIT(name, 2, 4)\n" +
		"  * xgiven=SPLIT(name, 2, 4)\n"
		;
	*/

	@Test
	void 데이터_스플릿_처리_구현() throws org.json.JSONException{
		// 해당 테스트는 진행 제대로하려면 실제 서버위에서 DI로 실행시켜야해서 생략
		CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties = new CustomValidationRemoteConfigProperties();
		customValidationRemoteConfigProperties.setLocalURL("http://localURL:8080/");
		customValidationRemoteConfigProperties.setRemoteTerminologyYn(false);

		transformEngine = new TransformEngine(customValidationRemoteConfigProperties);
		System.out.println("Transform Engine Start... ! ");

		String script = map13;
		List<RuleNode> ruleNodeList = MapperUtils.createTree(script);

		for(RuleNode eachRuleNode : ruleNodeList){
			// DFS 기반 맵서칭
			MapperUtils.printRuleAndRuleTypeInNodeTree(eachRuleNode);
		}

		JSONObject source = new JSONObject();
		source.put("name", "가나다라");
		transformEngine.transformDataToResource(script, source);
	}





}

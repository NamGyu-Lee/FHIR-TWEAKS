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
import com.google.gson.JsonObject;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.base.Sys;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Patient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;


import java.util.LinkedList;
import java.util.List;

/**
 *  Map 을 활용하여 테스트
 */
public class StructureMapCastingMapTest {

	TransformEngine transformEngine = new TransformEngine();

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
		"   * system='http://terminology.hl7.org/CodeSystem/v2-0203'\n" +
		"   * code=code\n" +
		" * system='http://www.hl7korea.or.kr/Identifier/hira-krnpi'\n" +
		" * value=value\n" +
		// test
		" * type\n" +
		"  * (coding).coding\n" +
		"   * system='http://terminology.hl7.org/CodeSystem/v2-0203'\n" +
		"   * code=code\n" +
		" * system='http://www.hl7korea.or.kr/Identifier/hira-krnpi'\n" +
		" * value=value\n" +
		//
		"* active='true'\n" +
		"* (name).name\n" +
		" * text=hng_nm\n"+
		" * given=eng_nm\n"+
		"* (telecom).telecom\n"+
		" * system='phone'\n"+
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
		String script = map7;
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
			//String targetElementNm = RuleUtils.getTargetElementName(rule);
			System.out.println("[" + ruleType + "]" + "[" + transactionType + "]" + " source Ref : " + sourceReferenceNm);
		}else{
			String targetElementNm = RuleUtils.getTargetElementName(rule);
			System.out.println("[" + ruleType + "]" + "[" + transactionType + "]" + " source Ref : " + sourceReferenceNm + " / Target : " + targetElementNm);

		}
	}

	@Test
	void 룰_배열문제_해결하기(){
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


}

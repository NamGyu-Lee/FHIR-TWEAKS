package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.jpa.starter.transfor.base.code.RuleType;
import ca.uhn.fhir.jpa.starter.transfor.base.code.TransactionType;
import ca.uhn.fhir.jpa.starter.transfor.base.core.TransformEngine;
import ca.uhn.fhir.jpa.starter.transfor.base.map.ActivateTransNode;
import ca.uhn.fhir.jpa.starter.transfor.base.map.RuleNode;
import ca.uhn.fhir.jpa.starter.transfor.base.util.MapperUtils;
import org.apache.jena.base.Sys;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
	void 룰_전체_실행하기() throws JSONException{
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

}

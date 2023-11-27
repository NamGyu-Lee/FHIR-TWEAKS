package ca.uhn.fhir.jpa.starter;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.starter.transfor.base.core.TransformEngine;
import ca.uhn.fhir.jpa.starter.transfor.base.map.ActivateTransNode;
import ca.uhn.fhir.jpa.starter.transfor.base.map.RuleNode;
import ca.uhn.fhir.jpa.starter.transfor.base.util.MapperUtils;
import ca.uhn.fhir.jpa.starter.transfor.base.util.RuleUtils;
import ca.uhn.fhir.jpa.starter.transfor.util.TransformUtil;
import ca.uhn.fhir.jpa.starter.validation.config.CustomValidationRemoteConfigProperties;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Patient;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.hl7.fhir.r4.model.ResourceType.Patient;

public class ResourceCreateTest {


	String map1 = "* resourceType='Patient'\n" +
		"* id=(KEY)id\n" +
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
		" * given=MERGE('afaf', hng_nm, '(', eng_nm ,')')\n"+
		"* (telecom).telecom\n"+
		" * system=NULLTHEN(telType, 'phone')\n"+
		" * value=telno\n"+
		"* gender=sex_cd\n"+
		"* birthDate=brth_dd\n"+
		"* (address).address\n"+
		" * text=detl_addr\n"+
		" * postalCode=zipcd\n"+
		"* managingOrganization\n"+
		" * reference=organization_Id"
		;

	@Test
	void 리소스_아이디_자동부여(){
		String script = map1;
		List<RuleNode> ruleNodeList = MapperUtils.createTree(script);
		LinkedHashSet<String> identifierMap = MapperUtils.createIdentifierMap(ruleNodeList);

		System.out.println("Rule List : " + ruleNodeList);
		System.out.println("----------------------");
		for(RuleNode eachNode: ruleNodeList){
			MapperUtils.printRuleAndRuleTypeInNodeTree(eachNode);
		}

		System.out.println("----------------------");
		for(String identifierStr : identifierMap){
			System.out.println("    identifierStr : " + identifierStr);
		}
	}

	@Test
	void 리소스_아이디_생성() throws org.json.JSONException{
		String script = map1;
		List<RuleNode> ruleNodeList = MapperUtils.createTree(script);
		LinkedHashSet<String> identifierSet = MapperUtils.createIdentifierMap(ruleNodeList);

		JSONObject sourceMap = new JSONObject();
		sourceMap.put("id", "123456");
		String id = TransformUtil.createResourceId("Patient", identifierSet, sourceMap);
		System.out.println(" >>>> " + id);
	}

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
		"  \"organization_Id\": \"Organization/CMC012\"\n" + // Active 시 파라미터에 접붙이기
		"}";


	TransformEngine transformEngine;
	@Test
	void 엔진_적용() throws org.json.JSONException{
		// 해당 테스트는 진행 제대로하려면 실제 서버위에서 DI로 실행시켜야해서 생략
		CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties = new CustomValidationRemoteConfigProperties();
		customValidationRemoteConfigProperties.setLocalURL("http://localURL:8080/");
		customValidationRemoteConfigProperties.setRemoteTerminologyYn(false);

		transformEngine = new TransformEngine(customValidationRemoteConfigProperties);
		System.out.println("Transform Engine Start... ! ");

		String script = map1;
		JSONObject targetObject = new JSONObject();
		try {
			System.out.println("--------------------");
			JSONObject sourceObj = new JSONObject(sourceMap);
			IBaseResource resource = transformEngine.transformDataToResource(script, sourceObj);

			FhirContext context = new FhirContext(FhirVersionEnum.R4);
			System.out.println(context.newJsonParser().encodeResourceToString(resource));
			System.out.println("--------------------");
		}catch(org.json.JSONException e){
			e.printStackTrace();
		}
	}

	@Test
	void JSONObject_innerSearch_Test() throws org.json.JSONException{
		JSONObject obj = new JSONObject();
		obj.put("a1", "data1");
		obj.put("a2", "data2");

		JSONObject obj2 = new JSONObject();
		obj2.put("a3", obj);

		System.out.println(obj2);
		//System.out.println(obj.get("a1.a1"));

		System.out.println("Search Source : " + TransformUtil.getNestedValueInJson(obj2, "a3.a1").toString());

	}


}

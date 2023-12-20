package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.jpa.starter.transfor.base.code.ErrorHandleType;
import ca.uhn.fhir.jpa.starter.transfor.base.map.MetaRule;
import ca.uhn.fhir.jpa.starter.transfor.base.map.ReferenceNode;
import ca.uhn.fhir.jpa.starter.transfor.base.map.ReferenceParamNode;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

@SpringBootTest
public class SampleTest {
	@Test
	public void test(){
		String arg = "{\n" +
			"  \"success\" : \"true\",\n" +
			"  \"stateCode\" : 200,\n" +
			"  \"errorReason\" : \"-\",\n" +
			"  \"body\" : \"{\\\"resourceType\\\":\\\"Bundle\\\",\\\"entry\\\":[{\\\"resource\\\":{\\\"resourceType\\\":\\\"Organization\\\",\\\"id\\\":\\\"11100338\\\",\\\"meta\\\":{\\\"profile\\\":[\\\"http://connectdtx.kr/fhir/StructureDefinition/connectdtx-organization\\\",\\\"http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-healthcare-organization\\\"]},\\\"identifier\\\":[{\\\"system\\\":\\\"http://www.hl7korea.or.kr/Identifier/hira-krnpi\\\",\\\"value\\\":\\\"urn:oid:1.2.410.100110.10.11100338\\\"}],\\\"active\\\":true,\\\"type\\\":[{\\\"coding\\\":[{\\\"system\\\":\\\"http://www.hl7korea.or.kr/CodeSystem/hira-healthcare-organization-types\\\",\\\"code\\\":\\\"01\\\"}]}],\\\"name\\\":\\\"가톨릭대학교 서울성모병원\\\",\\\"telecom\\\":[{\\\"system\\\":\\\"phone\\\",\\\"value\\\":\\\"02-2258-5518\\\"}],\\\"address\\\":[{\\\"text\\\":\\\"서울 서초구  반포대로222(반포4동)\\\",\\\"postalCode\\\":\\\"06591\\\"}]}},{\\\"resource\\\":{\\\"resourceType\\\":\\\"Practitioner\\\",\\\"id\\\":\\\"PRAT.11100338.21200328\\\",\\\"meta\\\":{\\\"profile\\\":[\\\"http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-medical-doctor\\\"]},\\\"identifier\\\":[{\\\"system\\\":\\\"urn:oid:1.2.410.100110.10.11100338\\\",\\\"value\\\":\\\"21200328\\\"}],\\\"active\\\":true,\\\"name\\\":[{\\\"text\\\":\\\"남경은\\\"}]}},{\\\"resource\\\":{\\\"resourceType\\\":\\\"Practitioner\\\",\\\"id\\\":\\\"PRAT.11100338.21901198\\\",\\\"meta\\\":{\\\"profile\\\":[\\\"http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-medical-doctor\\\"]},\\\"identifier\\\":[{\\\"system\\\":\\\"urn:oid:1.2.410.100110.10.11100338\\\",\\\"value\\\":\\\"21901198\\\"}],\\\"active\\\":true,\\\"name\\\":[{\\\"text\\\":\\\"김승재\\\"}]}},{\\\"resource\\\":{\\\"resourceType\\\":\\\"PractitionerRole\\\",\\\"id\\\":\\\"PROL.11100338.21200328\\\",\\\"meta\\\":{\\\"profile\\\":[\\\"http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-medical-doctor\\\"]},\\\"identifier\\\":[{\\\"system\\\":\\\"urn:oid:1.2.410.100110.10.11100338\\\",\\\"value\\\":\\\"21200328\\\"}],\\\"practitioner\\\":{\\\"reference\\\":\\\"Practitioner/PRAT.11100338.21200328\\\"},\\\"specialty\\\":[{\\\"coding\\\":[{\\\"system\\\":\\\"https://hira.or.kr/CodeSystem/medical-subject\\\",\\\"code\\\":\\\"21\\\",\\\"display\\\":\\\"재활의학과\\\"}],\\\"text\\\":\\\"재활의학과\\\"}]}},{\\\"resource\\\":{\\\"resourceType\\\":\\\"PractitionerRole\\\",\\\"id\\\":\\\"PROL.11100338.21901198\\\",\\\"meta\\\":{\\\"profile\\\":[\\\"http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-medical-doctor\\\"]},\\\"identifier\\\":[{\\\"system\\\":\\\"urn:oid:1.2.410.100110.10.11100338\\\",\\\"value\\\":\\\"21901198\\\"}],\\\"practitioner\\\":{\\\"reference\\\":\\\"Practitioner/PRAT.11100338.21901198\\\"},\\\"specialty\\\":[{\\\"coding\\\":[{\\\"system\\\":\\\"https://hira.or.kr/CodeSystem/medical-subject\\\",\\\"code\\\":\\\"23\\\",\\\"display\\\":\\\"가정의학과\\\"}],\\\"text\\\":\\\"가정의학과\\\"}]}},{\\\"resource\\\":{\\\"resourceType\\\":\\\"Patient\\\",\\\"id\\\":\\\"11100338.10280955\\\",\\\"meta\\\":{\\\"profile\\\":[\\\"http://connectdtx.kr/fhir/StructureDefinition/connectdtx-patient\\\",\\\"http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-patient\\\"]},\\\"identifier\\\":[{\\\"type\\\":{\\\"coding\\\":[{\\\"system\\\":\\\"http://terminology.hl7.org/CodeSystem/v2-0203-1\\\",\\\"code\\\":\\\"code\\\"}]},\\\"system\\\":\\\"http://www.hl7korea.or.kr/Identifier/hira-krnpi\\\",\\\"value\\\":\\\"11100338.10280955\\\"},{\\\"type\\\":{\\\"coding\\\":[{\\\"system\\\":\\\"http://terminology.hl7.org/CodeSystem/v2-0203-2\\\",\\\"code\\\":\\\"MR\\\"}]},\\\"system\\\":\\\"http://www.hl7korea.or.kr/Identifier/hira-krnpi\\\",\\\"value\\\":\\\"11100338.10280955\\\"}],\\\"active\\\":true,\\\"name\\\":[{\\\"text\\\":\\\"박성구\\\",\\\"given\\\":[\\\"박성구\\\"]}],\\\"telecom\\\":[{\\\"system\\\":\\\"phone\\\",\\\"value\\\":\\\"01000000000\\\"}],\\\"gender\\\":\\\"male\\\",\\\"birthDate\\\":\\\"1967-01-01\\\",\\\"address\\\":[{\\\"text\\\":\\\"-\\\",\\\"postalCode\\\":\\\"000000\\\"}],\\\"managingOrganization\\\":{\\\"reference\\\":\\\"Organization/11100338\\\"}}},{\\\"resource\\\":{\\\"resourceType\\\":\\\"Encounter\\\",\\\"id\\\":\\\"ENC.11100338.20230719.2220000000.21200328.O.1.10280955\\\",\\\"meta\\\":{\\\"profile\\\":[\\\"http://connectdtx.kr/fhir/StructureDefinition/connectdtx-encounter\\\",\\\"http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-encounter\\\"]},\\\"identifier\\\":[{\\\"system\\\":\\\"urn:oid:1.2.410.100110.10.11100338\\\",\\\"value\\\":\\\"11100338.20230719.2220000000.21200328.O.1.10280955\\\"}],\\\"status\\\":\\\"finished\\\",\\\"class\\\":{\\\"system\\\":\\\"http://terminology.hl7.org/CodeSystem/v3-ActCode\\\",\\\"code\\\":\\\"AMB\\\"},\\\"subject\\\":{\\\"reference\\\":\\\"Patient/11100338.10280955\\\"},\\\"participant\\\":[{\\\"individual\\\":{\\\"reference\\\":\\\"PractitionerRole/PROL.11100338.21200328\\\"}}],\\\"period\\\":{\\\"start\\\":\\\"2023-07-19\\\",\\\"end\\\":\\\"2023-07-19\\\"},\\\"serviceProvider\\\":{\\\"reference\\\":\\\"Organization/11100338\\\"}}},{\\\"resource\\\":{\\\"resourceType\\\":\\\"Encounter\\\",\\\"id\\\":\\\"ENC.11100338.20230728.2230000000.21901198.O.2.10280955\\\",\\\"meta\\\":{\\\"profile\\\":[\\\"http://connectdtx.kr/fhir/StructureDefinition/connectdtx-encounter\\\",\\\"http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-encounter\\\"]},\\\"identifier\\\":[{\\\"system\\\":\\\"urn:oid:1.2.410.100110.10.11100338\\\",\\\"value\\\":\\\"11100338.20230728.2230000000.21901198.O.2.10280955\\\"}],\\\"status\\\":\\\"finished\\\",\\\"class\\\":{\\\"system\\\":\\\"http://terminology.hl7.org/CodeSystem/v3-ActCode\\\",\\\"code\\\":\\\"AMB\\\"},\\\"subject\\\":{\\\"reference\\\":\\\"Patient/11100338.10280955\\\"},\\\"participant\\\":[{\\\"individual\\\":{\\\"reference\\\":\\\"PractitionerRole/PROL.11100338.21901198\\\"}}],\\\"period\\\":{\\\"start\\\":\\\"2023-07-28\\\",\\\"end\\\":\\\"2023-07-28\\\"},\\\"serviceProvider\\\":{\\\"reference\\\":\\\"Organization/11100338\\\"}}},{\\\"resource\\\":{\\\"resourceType\\\":\\\"Condition\\\",\\\"id\\\":\\\"PROL.11100338.20230719.2220000000.10280955.106069882\\\",\\\"meta\\\":{\\\"profile\\\":[\\\"http://connectdtx.kr/fhir/StructureDefinition/connectdtx-condition\\\",\\\"http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-condition-chief-complaint\\\"]},\\\"identifier\\\":[{\\\"system\\\":\\\"urn:oid:1.2.410.100110.10.11100338\\\",\\\"value\\\":\\\"11100338.20230719.2220000000.10280955.106069882\\\"}],\\\"category\\\":[{\\\"coding\\\":[{\\\"system\\\":\\\"http://www.hl7korea.or.kr/fhir/krcore/CodeSystem/krcore-condition-category-types\\\",\\\"code\\\":\\\"주호소\\\"}]}],\\\"code\\\":{\\\"coding\\\":[{\\\"system\\\":\\\"http://www.hl7korea.or.kr/CodeSystem/kostat-kcd-8\\\",\\\"code\\\":\\\"M51.2\\\",\\\"display\\\":\\\"L-HNP (Lumbar herniated nucleus pulposis)\\\"}]},\\\"subject\\\":{\\\"reference\\\":\\\"Patient/11100338.10280955\\\"},\\\"encounter\\\":{\\\"reference\\\":\\\"Encounter/ENC.11100338.20230719.2220000000.21200328.O.1.10280955\\\"},\\\"recordedDate\\\":\\\"2023-07-19\\\",\\\"recorder\\\":{\\\"reference\\\":\\\"PractitionerRole/PROL.11100338.21200328\\\"}}},{\\\"resource\\\":{\\\"resourceType\\\":\\\"Condition\\\",\\\"id\\\":\\\"PROL.11100338.20230728.2230000000.10280955.106362231\\\",\\\"meta\\\":{\\\"profile\\\":[\\\"http://connectdtx.kr/fhir/StructureDefinition/connectdtx-condition\\\",\\\"http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-condition-chief-complaint\\\"]},\\\"identifier\\\":[{\\\"system\\\":\\\"urn:oid:1.2.410.100110.10.11100338\\\",\\\"value\\\":\\\"11100338.20230728.2230000000.10280955.106362231\\\"}],\\\"category\\\":[{\\\"coding\\\":[{\\\"system\\\":\\\"http://www.hl7korea.or.kr/fhir/krcore/CodeSystem/krcore-condition-category-types\\\",\\\"code\\\":\\\"주호소\\\"}]}],\\\"code\\\":{\\\"coding\\\":[{\\\"system\\\":\\\"http://www.hl7korea.or.kr/CodeSystem/kostat-kcd-8\\\",\\\"code\\\":\\\"R05.\\\",\\\"display\\\":\\\"Cough\\\"}]},\\\"subject\\\":{\\\"reference\\\":\\\"Patient/11100338.10280955\\\"},\\\"encounter\\\":{\\\"reference\\\":\\\"Encounter/ENC.11100338.20230728.2230000000.21901198.O.2.10280955\\\"},\\\"recordedDate\\\":\\\"2023-07-28\\\",\\\"recorder\\\":{\\\"reference\\\":\\\"PractitionerRole/PROL.11100338.21901198\\\"}}},{\\\"resource\\\":{\\\"resourceType\\\":\\\"MedicationRequest\\\",\\\"id\\\":\\\"MEDR.MR.11100338.10280955.1661703011\\\",\\\"meta\\\":{\\\"profile\\\":[\\\"http://connectdtx.kr/fhir/StructureDefinition/connectdtx-medicationrequest\\\",\\\"http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-medicationrequest\\\"]},\\\"identifier\\\":[{\\\"system\\\":\\\"urn:oid:1.2.410.100110.10.11100338\\\",\\\"value\\\":\\\"MR.11100338.10280955.1661703011\\\"}],\\\"status\\\":\\\"completed\\\",\\\"intent\\\":\\\"order\\\",\\\"reportedBoolean\\\":false,\\\"medicationCodeableConcept\\\":{\\\"coding\\\":[{\\\"system\\\":\\\"http://www.hl7korea.or.kr/CodeSystem/hira-edi-medication\\\",\\\"code\\\":\\\"DAAPER650\\\",\\\"display\\\":\\\"타이레놀8시간이알서방정650mg\\\"}]},\\\"subject\\\":{\\\"reference\\\":\\\"Patient/11100338.10280955\\\"},\\\"encounter\\\":{\\\"reference\\\":\\\"Encounter/ENC.11100338.20230728.2230000000.21901198.O.2.10280955\\\"},\\\"authoredOn\\\":\\\"2023-07-28\\\",\\\"requester\\\":{\\\"reference\\\":\\\"PractitionerRole/PROL.11100338.21901198\\\"},\\\"dosageInstruction\\\":[{\\\"text\\\":\\\"아,점,저 식후30분\\\",\\\"timing\\\":{\\\"repeat\\\":{\\\"frequency\\\":3,\\\"period\\\":7,\\\"periodUnit\\\":\\\"d\\\"}},\\\"route\\\":{\\\"text\\\":\\\"unknown\\\"},\\\"doseAndRate\\\":[{\\\"doseQuantity\\\":{\\\"value\\\":650,\\\"unit\\\":\\\"mg\\\"}}]}]}},{\\\"resource\\\":{\\\"resourceType\\\":\\\"MedicationRequest\\\",\\\"id\\\":\\\"MEDR.MR.11100338.10280955.1661703012\\\",\\\"meta\\\":{\\\"profile\\\":[\\\"http://connectdtx.kr/fhir/StructureDefinition/connectdtx-medicationrequest\\\",\\\"http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-medicationrequest\\\"]},\\\"identifier\\\":[{\\\"system\\\":\\\"urn:oid:1.2.410.100110.10.11100338\\\",\\\"value\\\":\\\"MR.11100338.10280955.1661703012\\\"}],\\\"status\\\":\\\"completed\\\",\\\"intent\\\":\\\"order\\\",\\\"reportedBoolean\\\":false,\\\"medicationCodeableConcept\\\":{\\\"coding\\\":[{\\\"system\\\":\\\"http://www.hl7korea.or.kr/CodeSystem/hira-edi-medication\\\",\\\"code\\\":\\\"DREBST\\\",\\\"display\\\":\\\"리노에바스텔캅셀\\\"}]},\\\"subject\\\":{\\\"reference\\\":\\\"Patient/11100338.10280955\\\"},\\\"encounter\\\":{\\\"reference\\\":\\\"Encounter/ENC.11100338.20230728.2230000000.21901198.O.2.10280955\\\"},\\\"authoredOn\\\":\\\"2023-07-28\\\",\\\"requester\\\":{\\\"reference\\\":\\\"PractitionerRole/PROL.11100338.21901198\\\"},\\\"dosageInstruction\\\":[{\\\"text\\\":\\\"아침 식후30분\\\",\\\"timing\\\":{\\\"repeat\\\":{\\\"frequency\\\":1,\\\"period\\\":7,\\\"periodUnit\\\":\\\"d\\\"}},\\\"route\\\":{\\\"text\\\":\\\"unknown\\\"},\\\"doseAndRate\\\":[{\\\"doseQuantity\\\":{\\\"value\\\":1,\\\"unit\\\":\\\"C\\\"}}]}]}},{\\\"resource\\\":{\\\"resourceType\\\":\\\"MedicationRequest\\\",\\\"id\\\":\\\"MEDR.MR.11100338.10280955.1661703013\\\",\\\"meta\\\":{\\\"profile\\\":[\\\"http://connectdtx.kr/fhir/StructureDefinition/connectdtx-medicationrequest\\\",\\\"http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-medicationrequest\\\"]},\\\"identifier\\\":[{\\\"system\\\":\\\"urn:oid:1.2.410.100110.10.11100338\\\",\\\"value\\\":\\\"MR.11100338.10280955.1661703013\\\"}],\\\"status\\\":\\\"completed\\\",\\\"intent\\\":\\\"order\\\",\\\"reportedBoolean\\\":false,\\\"medicationCodeableConcept\\\":{\\\"coding\\\":[{\\\"system\\\":\\\"http://www.hl7korea.or.kr/CodeSystem/hira-edi-medication\\\",\\\"code\\\":\\\"DLEPZ60\\\",\\\"display\\\":\\\"레보투스 정 60mg\\\"}]},\\\"subject\\\":{\\\"reference\\\":\\\"Patient/11100338.10280955\\\"},\\\"encounter\\\":{\\\"reference\\\":\\\"Encounter/ENC.11100338.20230728.2230000000.21901198.O.2.10280955\\\"},\\\"authoredOn\\\":\\\"2023-07-28\\\",\\\"requester\\\":{\\\"reference\\\":\\\"PractitionerRole/PROL.11100338.21901198\\\"},\\\"dosageInstruction\\\":[{\\\"text\\\":\\\"아,점,저 식후30분\\\",\\\"timing\\\":{\\\"repeat\\\":{\\\"frequency\\\":3,\\\"period\\\":7,\\\"periodUnit\\\":\\\"d\\\"}},\\\"route\\\":{\\\"text\\\":\\\"unknown\\\"},\\\"doseAndRate\\\":[{\\\"doseQuantity\\\":{\\\"value\\\":60,\\\"unit\\\":\\\"mg\\\"}}]}]}},{\\\"resource\\\":{\\\"resourceType\\\":\\\"MedicationRequest\\\",\\\"id\\\":\\\"MEDR.MR.11100338.10280955.1661702976\\\",\\\"meta\\\":{\\\"profile\\\":[\\\"http://connectdtx.kr/fhir/StructureDefinition/connectdtx-medicationrequest\\\",\\\"http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-medicationrequest\\\"]},\\\"identifier\\\":[{\\\"system\\\":\\\"urn:oid:1.2.410.100110.10.11100338\\\",\\\"value\\\":\\\"MR.11100338.10280955.1661702976\\\"}],\\\"status\\\":\\\"completed\\\",\\\"intent\\\":\\\"order\\\",\\\"reportedBoolean\\\":false,\\\"medicationCodeableConcept\\\":{\\\"coding\\\":[{\\\"system\\\":\\\"http://www.hl7korea.or.kr/CodeSystem/hira-edi-medication\\\",\\\"code\\\":\\\"DG-BZD\\\",\\\"display\\\":\\\"삼아탄툼액100ml\\\"}]},\\\"subject\\\":{\\\"reference\\\":\\\"Patient/11100338.10280955\\\"},\\\"encounter\\\":{\\\"reference\\\":\\\"Encounter/ENC.11100338.20230728.2230000000.21901198.O.2.10280955\\\"},\\\"authoredOn\\\":\\\"2023-07-28\\\",\\\"requester\\\":{\\\"reference\\\":\\\"PractitionerRole/PROL.11100338.21901198\\\"},\\\"dosageInstruction\\\":[{\\\"text\\\":\\\"일반 가글용법(1일2~3회)\\\",\\\"timing\\\":{\\\"repeat\\\":{\\\"frequency\\\":1,\\\"period\\\":1,\\\"periodUnit\\\":\\\"d\\\"}},\\\"route\\\":{\\\"text\\\":\\\"unknown\\\"},\\\"doseAndRate\\\":[{\\\"doseQuantity\\\":{\\\"value\\\":100,\\\"unit\\\":\\\"ml\\\"}}]}]}},{\\\"resource\\\":{\\\"resourceType\\\":\\\"AllergyIntolerance\\\",\\\"id\\\":\\\"ALG.ALG.11100338.20210324.H00501805\\\",\\\"meta\\\":{\\\"profile\\\":[\\\"http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-allergyintolerance\\\"]},\\\"identifier\\\":[{\\\"system\\\":\\\"urn:oid:1.2.410.100110.10.11100338\\\",\\\"value\\\":\\\"ALG.11100338.20210324.H00501805\\\"}],\\\"code\\\":{\\\"coding\\\":[{\\\"system\\\":\\\"http://www.whocc.no/atc\\\",\\\"code\\\":\\\"H00501805\\\"}],\\\"text\\\":\\\"아스트라제네카 코비드-19 백신주\\\"},\\\"patient\\\":{\\\"reference\\\":\\\"Patient/11100338.10280955\\\"},\\\"recordedDate\\\":\\\"2021-03-24\\\"}},{\\\"resource\\\":{\\\"resourceType\\\":\\\"AllergyIntolerance\\\",\\\"id\\\":\\\"ALG.ALG.11100338.20210324.H00501805\\\",\\\"meta\\\":{\\\"profile\\\":[\\\"http://www.hl7korea.or.kr/fhir/krcore/StructureDefinition/krcore-allergyintolerance\\\"]},\\\"identifier\\\":[{\\\"system\\\":\\\"urn:oid:1.2.410.100110.10.11100338\\\",\\\"value\\\":\\\"ALG.11100338.20210324.H00501805\\\"}],\\\"code\\\":{\\\"coding\\\":[{\\\"system\\\":\\\"http://www.whocc.no/atc\\\",\\\"code\\\":\\\"H00501805\\\"}],\\\"text\\\":\\\"아스트라제네카 코비드-19 백신주\\\"},\\\"patient\\\":{\\\"reference\\\":\\\"Patient/11100338.10280955\\\"},\\\"recordedDate\\\":\\\"2021-03-24\\\"}}]}\"\n" +
			"}\n";


		String str = arg;
		JsonParser parser = new JsonParser();
		JsonElement element = parser.parse(str);
		String jsonDataByBody = element.getAsJsonObject().get("body").getAsString();

		JsonObject jsonObjectBody = parser.parse(jsonDataByBody).getAsJsonObject();

		System.out.println("sccuess : " + jsonObjectBody);
	}

	@Test
	public void test2(){
		String arg = "{";
		arg = arg + "\"svcreqkey\" : \"" + "ab" + "\",";
		arg = arg + "\"reason\" : \"" + "ab" + "\",";
		arg = arg + "\"bundle\" : " + "{}" + "}";
		System.out.println(arg);
	}

	String argMap =
		"* metadata\n" +
		" * mapping\n" +
		"  * error_policy = exception\n" +
		"  * cacheKey = pid, cret_no, prcp_no\n" +
		"  * referenceData\n" +
		"   * referenceResource\n" +
		"    * target = Patient\n" +
		"    * depend_policy = warning\n" +
		"    * v1 -> j1 :: Organization.pid\n" +
		"    * v2 -> j2 :: -\n" +
		"    * v3 -> j3 :: -\n"+
		"    * v4 -> j4 :: -\n"+
		"   * referenceResource\n" +
		"    * target = Patient\n" +
		"    * depend_policy = warning\n" +
		"    * pid -> pid :: Organization.pid\n" +
		"    * cret_no -> ord_dd :: -\n" +
		"    * prcp_no -> prcp_no :: -\n";


	String argMap2 = "* metadata\n" +
		" * mapping\n" +
		"  * error_policy = exception\n" +
		"  * cacheKey = inst_cd, proc_corp_cd\n";

	@Test
	public void test3(){
		MetaRule node = createMetaRule(argMap2);
		System.out.println(node.getCacheDataKey());
		System.out.println(node.getReferenceNodeList().size());
		System.out.println(node.getReferenceNodeList().get(0).getReferenceParamNodeList().get(0).getFhirTargetStr());
	}

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

				System.out.println(cacheKeySingleStr);
				String[] cacheKeyArray = cacheKeySingleStr.split(",");
				System.out.println(cacheKeyArray);
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

	public static int getLevel(String line) {
		int count = 0;
		for (char c : line.toCharArray()) {
			if (c != ' ') break;
			count++;
		}
		return count;
	}

	public static ReferenceParamNode createReferenceParamNode(String scriptLine) throws IllegalArgumentException{
			String[] parts = scriptLine.split("->|::");
			ReferenceParamNode refNode = new ReferenceParamNode();
			refNode.setSourceStr(parts[0].trim());
			refNode.setCacheTargetStr(parts[1].trim());
			refNode.setFhirTargetStr(parts[2].trim());
			return refNode;
	}
}

package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import com.google.gson.Gson;
import org.hl7.fhir.exceptions.DefinitionException;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.context.IWorkerContext;
import org.hl7.fhir.r4.context.SimpleWorkerContext;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.utils.FHIRPathEngine;
import org.hl7.fhir.r4.utils.StructureMapUtilities;
import org.hl7.fhir.utilities.CommaSeparatedStringBuilder;
import org.junit.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

public class StructureMapTest {

	FhirContext context = new FhirContext(FhirVersionEnum.R4);

	private FHIRPathEngine fpe;

	private IWorkerContext worker;

	HumanName target;

	@Before
	public void createWorker() throws IOException{
		worker = new SimpleWorkerContext();
		this.fpe = new FHIRPathEngine(worker);
		this.fpe.setHostServices(null);

		target = new HumanName();
	}


	// StructureMap 테스트
	@Test
	public void TestStructureMap(){
		String structureMapXmlStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"\n" +
			"<StructureMap xmlns=\"http://hl7.org/fhir\">\n" +
			"  <id value=\"tutorial\"/>\n" +
			"  <url value=\"http://hl7.org/fhir/StructureMap/tutorial-step12\"/>\n" +
			"  <name value=\"tutorial\"/>\n" +
			"  <status value=\"draft\"/>\n" +
			"  <structure>\n" +
			"    <url value=\"http://hl7.org/fhir/StructureDefinition/tutorial-left-12\"/>\n" +
			"    <mode value=\"source\"/>\n" +
			"    <alias value=\"TLeft\"/>\n" +
			"  </structure>\n" +
			"  <structure>\n" +
			"    <url value=\"http://hl7.org/fhir/StructureDefinition/tutorial-right-12\"/>\n" +
			"    <mode value=\"target\"/>\n" +
			"    <alias value=\"TRight\"/>\n" +
			"  </structure>\n" +
			"  <group>\n" +
			"    <name value=\"tutorial\"/>\n" +
			"    <input>\n" +
			"      <name value=\"src\"/>\n" +
			"      <type value=\"TLeft\"/>\n" +
			"      <mode value=\"source\"/>\n" +
			"    </input>\n" +
			"    <input>\n" +
			"      <name value=\"tgt\"/>\n" +
			"      <type value=\"TRight\"/>\n" +
			"      <mode value=\"target\"/>\n" +
			"    </input>\n" +
			"    <!-- setting up a variable for the parent -->\n" +
			"    <rule>\n" +
			"      <name value=\"rule_saz1\"/>\n" +
			"      <source>\n" +
			"        <context value=\"src\"/>\n" +
			"        <element value=\"az1\"/>\n" +
			"        <variable value=\"s_az1\"/>\n" +
			"      </source>\n" +
			"      <!-- one tgt.az1 for each az3 -->\n" +
			"      <rule>\n" +
			"        <name value=\"rule_taz1\"/>\n" +
			"        <source>\n" +
			"          <context value=\"s_az1\"/>\n" +
			"          <element value=\"az3\"/>\n" +
			"          <variable value=\"s_az3\"/>\n" +
			"        </source>\n" +
			"        <target>\n" +
			"          <context value=\"tgt\"/>\n" +
			"          <element value=\"az1\"/>\n" +
			"          <variable value=\"t_az1\"/>\n" +
			"        </target>\n" +
			"        <!-- value for az2. Note that this refers to a previous context in the source -->\n" +
			"        <rule>\n" +
			"          <name value=\"rule_az2\"/>\n" +
			"          <source>\n" +
			"            <context value=\"s_az1\"/>\n" +
			"            <element value=\"az2\"/>\n" +
			"            <variable value=\"az2\"/>\n" +
			"          </source>\n" +
			"          <target>\n" +
			"            <context value=\"t_az1\"/>\n" +
			"            <element value=\"az2\"/>\n" +
			"            <transform value=\"copy\"/>\n" +
			"            <parameter>\n" +
			"              <valueId value=\"az2\"/>\n" +
			"            </parameter>\n" +
			"          </target>\n" +
			"        </rule>\n" +
			"        <!-- value for az3 -->\n" +
			"        <rule>\n" +
			"          <name value=\"rule_az3\"/>\n" +
			"          <source>\n" +
			"            <context value=\"s_az3\"/>\n" +
			"          </source>\n" +
			"          <target>\n" +
			"            <context value=\"t_az1\"/>\n" +
			"            <element value=\"az3\"/>\n" +
			"            <transform value=\"copy\"/>\n" +
			"            <parameter>\n" +
			"              <valueId value=\"s_az3\"/>\n" +
			"            </parameter>\n" +
			"          </target>\n" +
			"        </rule>\n" +
			"      </rule>\n" +
			"    </rule>\n" +
			"  </group>\n" +
			"</StructureMap>";


		IBaseResource mapBase = context.newXmlParser().parseResource(structureMapXmlStr);
		StructureMap map = (StructureMap) mapBase;

		System.out.println( "> " + map.getId());

		// 소스 정의
		String sourceXmlStr = "<TLeft xmlns=\"http://hl7.org/fhir/tutorial\">\n" +
			"\t<az1>\n" +
			"\t\t<az2 value=\"FHIR\" />\n" +
			"\t\t<az3 value=\"Fast\" />\n" +
			"\t\t<az3 value=\"Resource\" />\n" +
			"\t</az1>\n" +
			"</TLeft>";

		SampleBaseObject source = new SampleBaseObject();
		SampleBaseObject innerObj = new SampleBaseObject();
		SampleBaseObject doubleInnerObject = new SampleBaseObject();
		doubleInnerObject.setProperty("az2", new StringType("FHIR"));
		doubleInnerObject.setProperty("az3", new StringType("Fast"));
		doubleInnerObject.setProperty("az3", new StringType("Resource"));
		innerObj.setProperty("az1", doubleInnerObject);
		source.setProperty("TLeft", innerObj);

		// Target
		SampleBaseObject target = new SampleBaseObject();
		SampleBaseObject innerTargetObj = new SampleBaseObject();
		innerTargetObj.setProperty("az1", new StringType());
		innerTargetObj.setProperty("az2", new StringType());
		innerTargetObj.setProperty("az3", new StringType());
		target.setProperty("TRight", innerTargetObj);
		// Transform
		try {
			System.out.println("active Start ---------------------------------------------");
			IWorkerContext hapiWorkerContext = new SimpleWorkerContext();
			StructureMapUtilities utils = new StructureMapUtilities(hapiWorkerContext);

			utils.transform(null, source, map, target);
			System.out.println("active End ---------------------------------------------");

		}catch(IOException e){

		}

		target.printAll();

	}


	@Test
	void humanNameTest(){
		String structureMapStr = "{\n" +
			"  \"resourceType\": \"StructureMap\",\n" +
			"  \"id\": \"2252\",\n" +
			"  \"meta\": {\n" +
			"    \"versionId\": \"2\",\n" +
			"    \"lastUpdated\": \"2023-11-10T16:26:21.167+09:00\",\n" +
			"    \"source\": \"#HfAFOhEcPlQaGYpL\"\n" +
			"  },\n" +
			"  \"url\": \"http://hl7.org/fhir/StructureMap/example\",\n" +
			"  \"identifier\": [ {\n" +
			"    \"system\": \"urn:ietf:rfc:3986\",\n" +
			"    \"value\": \"urn:oid:2.16.840.1.113883.4.642.13.2\"\n" +
			"  } ],\n" +
			"  \"version\": \"6.0.0-cibuild\",\n" +
			"  \"name\": \"ExampleHumanNameExchange\",\n" +
			"  \"title\": \"Example HumanName Map\",\n" +
			"  \"status\": \"active\",\n" +
			"  \"experimental\": true,\n" +
			"  \"date\": \"2017-03-09\",\n" +
			"  \"publisher\": \"HL7 FHIR Standard\",\n" +
			"  \"contact\": [ {\n" +
			"    \"telecom\": [ {\n" +
			"      \"system\": \"url\",\n" +
			"      \"value\": \"http://hl7.org/fhir\"\n" +
			"    } ]\n" +
			"  } ],\n" +
			"  \"description\": \"Example Structure Map\",\n" +
			"  \"jurisdiction\": [ {\n" +
			"    \"coding\": [ {\n" +
			"      \"system\": \"http://unstats.un.org/unsd/methods/m49/m49.htm\",\n" +
			"      \"code\": \"009\",\n" +
			"      \"display\": \"Oceania\"\n" +
			"    } ]\n" +
			"  } ],\n" +
			"  \"group\": [ {\n" +
			"    \"name\": \"MapToHumanName\",\n" +
			"    \"typeMode\": \"types\",\n" +
			"    \"documentation\": \"Map source fields to HumanName\",\n" +
			"    \"input\": [ {\n" +
			"      \"name\": \"source\",\n" +
			"      \"type\": \"myHumanName\",\n" +
			"      \"mode\": \"source\"\n" +
			"    }, {\n" +
			"      \"name\": \"target\",\n" +
			"      \"type\": \"HumanName\",\n" +
			"      \"mode\": \"target\"\n" +
			"    } ],\n" +
			"    \"rule\": [ {\n" +
			"      \"name\": \"mapGiven\",\n" +
			"      \"source\": [ {\n" +
			"        \"context\": \"source\",\n" +
			"        \"element\": \"given\",\n" +
			"        \"variable\": \"given\",\n" +
			"        \"min\": \"0\",\n" +
			"        \"max\": \"0\",\n" +
			"        \"defaultValue\": \"Vavaeee\"\n" +
			"      } ],\n" +
			"      \"target\": [ {\n" +
			"        \"context\": \"target\",\n" +
			"        \"element\": \"given\",\n" +
			"        \"transform\": \"create\",\n" +
			"        \"parameter\": [ {\n" +
			"          \"valueId\": \"given\"\n" +
			"        } ]\n" +
			"      } ]\n" +
			"    }, {\n" +
			"      \"name\": \"mapFamily\",\n" +
			"      \"source\": [ {\n" +
			"        \"context\": \"source\",\n" +
			"        \"element\": \"family\",\n" +
			"        \"variable\": \"family\"\n" +
			"      } ],\n" +
			"      \"target\": [ {\n" +
			"        \"context\": \"target\",\n" +
			"        \"element\": \"family\",\n" +
			"        \"transform\": \"copy\",\n" +
			"        \"parameter\": [ {\n" +
			"          \"valueId\": \"family\"\n" +
			"        } ]\n" +
			"      } ]\n" +
			"    }, {\n" +
			"      \"name\": \"mapText\",\n" +
			"      \"source\": [ {\n" +
			"        \"context\": \"source\",\n" +
			"        \"element\": \"text\",\n" +
			"        \"variable\": \"text\"\n" +
			"      } ],\n" +
			"      \"target\": [ {\n" +
			"        \"context\": \"target\",\n" +
			"        \"element\": \"text\",\n" +
			"        \"transform\": \"copy\",\n" +
			"        \"parameter\": [ {\n" +
			"          \"valueId\": \"text\"\n" +
			"        } ]\n" +
			"      } ]\n" +
			"    } ]\n" +
			"  } ]\n" +
			"}";

		IBaseResource mapBase = context.newJsonParser().parseResource(structureMapStr);
		StructureMap structureMap = (StructureMap) mapBase;

		// source
		// 검증1. Inner에 있는 getProperty 를 수행한다.
		SampleBaseObject source = new SampleBaseObject();
		SampleBaseObject innerObj = new SampleBaseObject();
		innerObj.setProperty("given", new StringType("FHIR"));
		innerObj.setProperty("family", new StringType("Fast"));
		innerObj.setProperty("text", new StringType("Resource"));
		source.setProperty("myHumanName", innerObj);

		// target
		// target의 맵을 찾아서 안들어가지는 이슈.
		target = new HumanName();
		target.setFamily("D");
		test(target);
		// Transform
		try {

			System.out.println("Semi Test............................");
			transformFromMainClass(null, source, structureMap, target);

			System.out.println("active Start ---------------------------------------------");
			IWorkerContext hapiWorkerContext = new SimpleWorkerContext();
			StructureMapUtilities utils = new StructureMapUtilities(hapiWorkerContext);

			utils.transform(context, source, structureMap, target);
			System.out.println("active End ---------------------------------------------");

		}catch(IOException e){}

		// return
		System.out.println("ret : " + context.newJsonParser().encodeToString(target));
	}

	public void test(HumanName nm){
		nm.setFamily("????!!");
	}

	// AIM TO THIS..!
	// StructureMap 기반 변경 테스트
	/*
	public IBaseResource createResourceUsingStructureDef(StructureMap map, Map<String, Reference> requestMap, referenceSet set){
		// 1. exchange data -> map
		// IBaseResource resource = exchange(requestMap, map);

		// 2. reference control
	}
	*/

	// StructureMapUtil 에서 뗘온것들
	// 액티브 순서를 이해하는 목적으로 활용
	public void transformFromMainClass(Object appInfo, Base source, StructureMap map, Base target) throws FHIRException {
		StructureMap.StructureMapGroupComponent g = (StructureMap.StructureMapGroupComponent)map.getGroup().get(0);
		Variables vars = new Variables();
		vars.add(StructureMapUtilities.VariableMode.INPUT, this.getInputName(g, StructureMap.StructureMapInputMode.SOURCE, "source"), source);
		if (target != null) {
			vars.add(StructureMapUtilities.VariableMode.OUTPUT, this.getInputName(g, StructureMap.StructureMapInputMode.TARGET, "target"), target);
		}

		// test
		System.out.println("-------------------------------------------");
		System.out.println("Show Variables Summary : " +  vars.summary());
		System.out.println("-------------------------------------------");
		Base baseInput = vars.get(StructureMapUtilities.VariableMode.INPUT, this.getInputName(g, StructureMap.StructureMapInputMode.SOURCE, "source"));
		System.out.println("-------------------------------------------");
		// 정의 2. input 을 와퍼하지 않고 그대로 활용한다. 이는 Resource 들도 마찬가지.
		// Variable 이 name, mode, object를 가지고있고, object가 실제 in/out에 넣는게 그대로 들어간다.
		System.out.println(baseInput.getClass().getName() + "    ... " + baseInput.toString());
		System.out.println("-------------------------------------------");

		// test
		for(Variable v : vars.list){
			System.out.println(" - name : " + v.getName());
			System.out.println(" - mode : " + v.getMode());
			System.out.println(" - object : " + v.getObject());
		}

		// 세팅 목적으로 확인되어 StructureMapUtilities.TransformContext context 생략
		// g는 해당 StructureMap의 첫번째 그룹이다.
		// 정의 3. 각각의 리소스의 대한 StructureDefinition 은 필요가 없다.
		// 정의 4. executeRule 1397 Line 에 따라 Source 가 없는 경우 예외처리
		// 정의 5. Rule 은 Source 의 기준의 따라 실행 후 target 의 기준의 따라 실행
		// 정의 6. 이 후 Rule 내 Rule을 체크하여 존재하는지 체크. 즉 Rule 은 그저 Source/Target 이고,
		//         매번 Rule은 계속 Source/Target으로만 정의
		// 정의 7. Rule을 다 돌리고 난 뒤 Dependency 를 수행
		// 정의 8. 실제 변환이라는 개념의 동작은 executeRule 내 processTarget() 이라는 액티브를 수행함
		this.executeGroup("", map, vars, g, true);
		if (target instanceof org.hl7.fhir.r4.elementmodel.Element) {
			((org.hl7.fhir.r4.elementmodel.Element)target).sort();
		}

	}

	public void testTransform과정에서의_processTarget임의구성(){
		// MapUtil 에서 1990 라인의 v = dest.setProperty(tgt.getElement().hashCode(), tgt.getElement(), v);
		// 로직이 target의 결과 조회 기능을 동작하지 못하게 하는것으로 추정하여 테스트 로직 구성

		// 1. 목적지 가져오기 1976 Line
		//dest = vars.get(StructureMapUtilities.VariableMode.OUTPUT, tgt.getContext());

		// 2. 목적지에 넣을 값 가져오기 1988 Line runTransform
		Base stdType = new StringType("V1");


		// 3. 저장
		//HumanName hn = new HumanName();
		//Variable ab = hn.getProperty(hn.getFamily().hashCode(), hn.getFamily(), false);

	}

	// 메인Util 클래스에서 가져옴.
	// in, out 오브젝트의 대하여 반환하는 용도.
	private String getInputName(StructureMap.StructureMapGroupComponent g, StructureMap.StructureMapInputMode mode, String def) throws DefinitionException {
		String name = null;
		Iterator var5 = g.getInput().iterator();

		while(var5.hasNext()) {
			StructureMap.StructureMapGroupInputComponent inp = (StructureMap.StructureMapGroupInputComponent)var5.next();
			if (inp.getMode() == mode) {
				if (name != null) {
					throw new DefinitionException("This engine does not support multiple source inputs");
				}

				name = inp.getName();
			}
		}

		return name == null ? def : name;
	}

	public class Variables {
		private List<Variable> list = new ArrayList();

		public Variables() {
		}

		public void add(StructureMapUtilities.VariableMode mode, String name, Base object) {
			Variable vv = null;
			Iterator var5 = this.list.iterator();

			while(var5.hasNext()) {
				Variable v = (Variable)var5.next();
				if (v.mode == mode && v.getName().equals(name)) {
					vv = v;
				}
			}

			if (vv != null) {
				this.list.remove(vv);
			}

			this.list.add(new Variable(mode, name, object));
		}

		public Variables copy() {
			Variables result = new Variables();
			result.list.addAll(this.list);
			return result;
		}

		public Base get(StructureMapUtilities.VariableMode mode, String name) {
			Iterator var3 = this.list.iterator();

			Variable v;
			do {
				if (!var3.hasNext()) {
					return null;
				}

				v = (Variable)var3.next();
			} while(v.mode != mode || !v.getName().equals(name));

			return v.getObject();
		}

		public String summary() {
			CommaSeparatedStringBuilder s = new CommaSeparatedStringBuilder();
			CommaSeparatedStringBuilder t = new CommaSeparatedStringBuilder();
			CommaSeparatedStringBuilder sh = new CommaSeparatedStringBuilder();
			Iterator var4 = this.list.iterator();

			while(var4.hasNext()) {
				Variable v = (Variable)var4.next();
				switch (v.mode) {
					case INPUT:
						s.append(v.summary());
						break;
					case OUTPUT:
						t.append(v.summary());
						break;
					case SHARED:
						sh.append(v.summary());
				}
			}

			String var10000 = s.toString();
			return "source variables [" + var10000 + "], target variables [" + t.toString() + "], shared variables [" + sh.toString() + "]";
		}
	}

	public class Variable {
		private StructureMapUtilities.VariableMode mode;
		private String name;
		private Base object;

		public Variable(StructureMapUtilities.VariableMode mode, String name, Base object) {
			this.mode = mode;
			this.name = name;
			this.object = object;
		}

		public StructureMapUtilities.VariableMode getMode() {
			return this.mode;
		}

		public String getName() {
			return this.name;
		}

		public Base getObject() {
			return this.object;
		}

		public String summary() {
			if (this.object == null) {
				return null;
			} else {
				String var10000;
				if (this.object instanceof PrimitiveType) {
					var10000 = this.name;
					return var10000 + ": \"" + ((PrimitiveType)this.object).asStringValue() + "\"";
				} else {
					var10000 = this.name;
					return var10000 + ": (" + this.object.fhirType() + ")";
				}
			}
		}
	}

	// 실제 그룹 생성, 작업진행
	// StructureMapUtilities.TransformContext context 생략
	private void executeGroup(String indent, StructureMap map, Variables vars, StructureMap.StructureMapGroupComponent group, boolean atRoot) throws FHIRException {
		System.out.println(indent + "Group : " + group.getName() + "; vars = " + vars.summary());
		System.out.println(" >> group Has Extends  : " + group.hasExtends());
		// 그룹의 확장형이면 그룹-> Inner Group -> Group 식으로 깊게 내려들어감
		if (group.hasExtends()) {
			System.out.println(" >> Then Regroup operation.. resolveGroupReference");
			ResolvedGroup rg = this.resolveGroupReference(map, group, group.getExtends());
			this.executeGroup(indent + " ", rg.targetMap, vars, rg.target, false);
		}

		Iterator var9 = group.getRule().iterator();

		while(var9.hasNext()) {
			StructureMap.StructureMapGroupRuleComponent r = (StructureMap.StructureMapGroupRuleComponent)var9.next();
			System.out.println(" r.getName() : " + r.getName());
			System.out.println(" ---- this timing To executeRule... Test");
			//this.executeRule(indent + "  ", map, vars, group, r, atRoot);
		}
	}

	private ResolvedGroup resolveGroupReference(StructureMap map, StructureMap.StructureMapGroupComponent source, String name) throws FHIRException {
		String kn = "ref^" + name;
		if (source.hasUserData(kn)) {
			return (ResolvedGroup)source.getUserData(kn);
		} else {
			ResolvedGroup res = new ResolvedGroup();
			res.targetMap = null;
			res.target = null;
			Iterator var6 = map.getGroup().iterator();

			while(var6.hasNext()) {
				StructureMap.StructureMapGroupComponent grp = (StructureMap.StructureMapGroupComponent)var6.next();
				if (grp.getName().equals(name)) {
					if (res.targetMap != null) {
						throw new FHIRException("Multiple possible matches for rule '" + name + "'");
					}

					res.targetMap = map;
					res.target = grp;
				}
			}

			if (res.targetMap != null) {
				source.setUserData(kn, res);
				return res;
			} else {
				var6 = map.getImport().iterator();

				label64:
				while(var6.hasNext()) {
					UriType imp = (UriType)var6.next();
					List<StructureMap> impMapList = this.findMatchingMaps((String)imp.getValue());
					if (impMapList.size() == 0) {
						throw new FHIRException("Unable to find map(s) for " + (String)imp.getValue());
					}

					Iterator var9 = impMapList.iterator();

					while(true) {
						StructureMap impMap;
						do {
							if (!var9.hasNext()) {
								continue label64;
							}

							impMap = (StructureMap)var9.next();
						} while(impMap.getUrl().equals(map.getUrl()));

						Iterator var11 = impMap.getGroup().iterator();

						while(var11.hasNext()) {
							StructureMap.StructureMapGroupComponent grp = (StructureMap.StructureMapGroupComponent)var11.next();
							if (grp.getName().equals(name)) {
								if (res.targetMap != null) {
									throw new FHIRException("Multiple possible matches for rule group '" + name + "' in " + res.targetMap.getUrl() + "#" + res.target.getName() + " and " + impMap.getUrl() + "#" + grp.getName());
								}

								res.targetMap = impMap;
								res.target = grp;
							}
						}
					}
				}

				if (res.target == null) {
					throw new FHIRException("No matches found for rule '" + name + "'. Reference found in " + map.getUrl());
				} else {
					source.setUserData(kn, res);
					return res;
				}
			}
		}
	}


	public class ResolvedGroup {
		public StructureMap.StructureMapGroupComponent target;
		public StructureMap targetMap;

		public ResolvedGroup() {
		}
	}

	private List<StructureMap> findMatchingMaps(String value) {
		List<StructureMap> res = new ArrayList();
		if (value.contains("*")) {
			Iterator var3 = this.worker.listTransforms().iterator();

			while(var3.hasNext()) {
				StructureMap sm = (StructureMap)var3.next();
				if (this.urlMatches(value, sm.getUrl())) {
					res.add(sm);
				}
			}
		} else {
			StructureMap sm = this.worker.getTransform(value);
			if (sm != null) {
				res.add(sm);
			}
		}

		Set<String> check = new HashSet();
		Iterator var8 = res.iterator();

		while(var8.hasNext()) {
			StructureMap sm = (StructureMap)var8.next();
			if (check.contains(sm.getUrl())) {
				throw new Error("duplicate");
			}

			check.add(sm.getUrl());
		}

		return res;
	}

	private boolean urlMatches(String mask, String url) {
		return url.length() > mask.length() && url.startsWith(mask.substring(0, mask.indexOf("*"))) && url.endsWith(mask.substring(mask.indexOf("*") + 1));
	}

	/*
	private void executeRule(String indent, StructureMap map, Variables vars, StructureMap.StructureMapGroupComponent group, StructureMap.StructureMapGroupRuleComponent rule, boolean atRoot) throws FHIRException {
		System.out.println(indent + "rule : " + rule.getName() + "; vars = " + vars.summary());
		Variables srcVars = vars.copy();
		if (rule.getSource().size() != 1) {
			throw new FHIRException("Rule \"" + rule.getName() + "\": not handled yet");
		} else {
			List<Variables> source = this.processSource(rule.getName(), srcVars, (StructureMap.StructureMapGroupRuleSourceComponent)rule.getSource().get(0), map.getUrl(), indent);
			if (source != null) {
				Iterator var10 = source.iterator();

				while(true) {
					while(var10.hasNext()) {
						StructureMapUtilities.Variables v = (StructureMapUtilities.Variables)var10.next();
						Iterator var12 = rule.getTarget().iterator();

						while(var12.hasNext()) {
							StructureMap.StructureMapGroupRuleTargetComponent t = (StructureMap.StructureMapGroupRuleTargetComponent)var12.next();
							this.processTarget(rule.getName(), context, v, map, group, t, rule.getSource().size() == 1 ? rule.getSourceFirstRep().getVariable() : null, atRoot, vars);
						}

						if (rule.hasRule()) {
							var12 = rule.getRule().iterator();

							while(var12.hasNext()) {
								StructureMap.StructureMapGroupRuleComponent childrule = (StructureMap.StructureMapGroupRuleComponent)var12.next();
								this.executeRule(indent + "  ",  map, v, group, childrule, false);
							}
						} else if (rule.hasDependent()) {
							var12 = rule.getDependent().iterator();

							while(var12.hasNext()) {
								StructureMap.StructureMapGroupRuleDependentComponent dependent = (StructureMap.StructureMapGroupRuleDependentComponent)var12.next();
								this.executeDependency(indent + "  ", context, map, v, group, dependent);
							}
						} else if (rule.getSource().size() == 1 && rule.getSourceFirstRep().hasVariable() && rule.getTarget().size() == 1 && rule.getTargetFirstRep().hasVariable() && rule.getTargetFirstRep().getTransform() == StructureMap.StructureMapTransform.CREATE && !rule.getTargetFirstRep().hasParameter()) {
							System.out.println(v.summary());
							Base src = v.get(StructureMapUtilities.VariableMode.INPUT, rule.getSourceFirstRep().getVariable());
							Base tgt = v.get(StructureMapUtilities.VariableMode.OUTPUT, rule.getTargetFirstRep().getVariable());
							String srcType = src.fhirType();
							String tgtType = tgt.fhirType();
							StructureMapUtilities.ResolvedGroup defGroup = this.resolveGroupByTypes(map, rule.getName(), group, srcType, tgtType);
							StructureMapUtilities.Variables vdef = new StructureMapUtilities.Variables();
							vdef.add(StructureMapUtilities.VariableMode.INPUT, ((StructureMap.StructureMapGroupInputComponent)defGroup.target.getInput().get(0)).getName(), src);
							vdef.add(StructureMapUtilities.VariableMode.OUTPUT, ((StructureMap.StructureMapGroupInputComponent)defGroup.target.getInput().get(1)).getName(), tgt);
							this.executeGroup(indent + "  ", context, defGroup.targetMap, vdef, defGroup.target, false);
						}
					}

					return;
				}
			}
		}
		*/
	}

	// in out 테스트 목적으로 만든 클래스
	class SampleBaseObject extends Base {

		Map<String, Base> map = new HashMap<>();

		@Override
		public String fhirType() {
			return getClass().getName();
		}

		@Override
		protected void listChildren(List<Property> list) {

		}

		@Override
		public String getIdBase() {
			return null;
		}

		@Override
		public void setIdBase(String s) {

		}

		@Override
		public Base copy() {
			return null;
		}

		/*
		@Override
		public Base[] getProperty(int hash, String name, boolean checkValid) throws FHIRException {

		}*/

		@Override
		public Base[] getProperty(int hash, String name, boolean checkValid) throws FHIRException {
			Base[] nw = new Base[1];
			Base baseTest = map.get(name);
			nw[0] = baseTest;

			System.out.println(" >> Tester getProperty This Value : " + name );

			return nw;
		}

		@Override
		public Base setProperty(int hash, String name, Base value) throws FHIRException {
			map.put(name, value);
			System.out.println(" >> Tester setProperties This Value with hashVal : " + name + " : " + value.toString());

			return value;
		}

		@Override
		public Base setProperty(String name, Base value) throws FHIRException {
			map.put(name, value);
			System.out.println(" >> Tester setProperties This Value : " + name + " : " + value.toString());

			return value;
		}

		public void printAll(){
			System.out.println("---------------------------");
			Gson gson = new Gson();
			System.out.println(gson.toJson(map));
			System.out.println("---------------------------");
		}

		@Override
		public String toString(){
			String retString = "";
			for(String key : map.keySet()){
				retString = retString + key;
				retString = retString + " : " + map.get(key) + "\n";
			}
			return retString;
		}
	}

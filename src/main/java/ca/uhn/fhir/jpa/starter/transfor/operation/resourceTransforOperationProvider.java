package ca.uhn.fhir.jpa.starter.transfor.operation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.provider.BaseJpaProvider;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.jpa.starter.transfor.code.MyHumanName;
import ca.uhn.fhir.jpa.starter.transfor.config.TransformDataOperationConfigProperties;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.api.SearchTotalModeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.r4.context.SimpleWorkerContext;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.utils.StructureMapUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 *  2023. 11. 07. CMC 기반의 EMR시스템에서 접근하는 Operation의 대하여 핸들링한다.
 */

public class resourceTransforOperationProvider extends BaseJpaProvider {

	private static final Logger ourLog = LoggerFactory.getLogger(resourceTransforOperationProvider.class);

	private DaoRegistry myDaoRegistry;
	@Autowired
	void setMyDaoRegistry(DaoRegistry myDaoRegistry){
		this.myDaoRegistry = myDaoRegistry;
	}

	private TransformDataOperationConfigProperties transformDataOperationConfigProperties;
	@Autowired
	void setTransformDataOperationConfigProperties(TransformDataOperationConfigProperties transformDataOperationConfigProperties){
		this.transformDataOperationConfigProperties = transformDataOperationConfigProperties;
	}

	@Autowired
	private HapiWorkerContext hapiWorkerContext;

	public resourceTransforOperationProvider() {

	}

	// 1. CMC 기반 데이터를 전달받아 Patient 를 생성한다.
	// theServletResponse 에 write으로 리턴하는 방식.
	// locahost:8080/fhir/$sample-custom-operation
	@Operation(
		name="$tranform-resource",
		idempotent = false,
		manualRequest = true,
		manualResponse = true
	)
	public void transforResourceStandardService(HttpServletRequest theServletRequest, HttpServletResponse theResponse) throws IOException {
		String retMessage = "-";
		ourLog.info(" > Create CMC Standard Data Transfor initalized.. ");

		try {
			byte[] bytes = IOUtils.toByteArray(theServletRequest.getInputStream());
			String bodyData = new String(bytes);

			ourLog.info(" > Request String : " + bodyData);
			JsonParser jsonParser = new JsonParser();
			JsonElement jsonElement = jsonParser.parse(bodyData);
			JsonObject jsonObject = jsonElement.getAsJsonObject();

			// 1. JSON TO JsonObject
			// 1.1. Sorting Cause Reference.
			Queue<JsonElement> upperSortingQueue = new LinkedList<>();
			Queue<JsonElement> lowerSortingQueue = new LinkedList<>();
			Queue<JsonElement> nonSortingQueue = new LinkedList<>();
			Queue<JsonElement> sortedQueue = new LinkedList<>();
			for(Map.Entry<String, JsonElement> eachEntry : jsonObject.entrySet()){
				// first
				for(String upperString : transformDataOperationConfigProperties.getResourceUpperSortingReferenceSet()){
					if(eachEntry.getKey().equals(upperString)){
						upperSortingQueue.add(eachEntry.getValue());
						continue;
					}
				}

				// lower
				for(String lowerString : transformDataOperationConfigProperties.getResourceLowerSortingReferenceSet()){
					if(eachEntry.getKey().equals(lowerString)){
						lowerSortingQueue.add(eachEntry.getValue());
						continue;
					}
				}

				// middle
				nonSortingQueue.add(eachEntry.getValue());
			}
			// 1.2. merge
			while(upperSortingQueue.size() != 0){
				sortedQueue.add(upperSortingQueue.poll());
			}
			while(nonSortingQueue.size() != 0){
				sortedQueue.add(nonSortingQueue.poll());
			}
			while(lowerSortingQueue.size() != 0){
				sortedQueue.add(lowerSortingQueue.poll());
			}

			// 2. StructureMap Read



			// 3. Save

		}catch(Exception e){
			e.printStackTrace();
			retMessage = e.getMessage();
		}finally{
			theResponse.setContentType("text/plain");
			theResponse.getWriter().write(retMessage);
			theResponse.getWriter().close();
		}
	}

	@Operation(
		name="$std-test",
		idempotent = false,
		manualRequest = true,
		manualResponse = true
	)
	public void structureMapTest(HttpServletRequest theServletRequest, HttpServletResponse theResponse) throws IOException {
			FhirContext context = this.getContext();

			ourLog.info(" >>> StructureMap Test");
			ourLog.info("transformDataOperationConfigProperties : " + transformDataOperationConfigProperties.getServiceTarget());

			IBaseCoding serachCodeForStructureDef = new Coding();
			serachCodeForStructureDef.setCode("active");
			TokenParam searchToken = new TokenParam(serachCodeForStructureDef);
			SearchParameterMap searchParameterMapForStructureDef = new SearchParameterMap().add(StructureDefinition.SP_STATUS, searchToken);
			searchParameterMapForStructureDef.setSearchTotalMode(SearchTotalModeEnum.ACCURATE);
			searchParameterMapForStructureDef.setCount(1000);
			IFhirResourceDao structureDefinitionResourceProvider = myDaoRegistry.getResourceDao("StructureDefinition");
			IBundleProvider results = structureDefinitionResourceProvider.search(searchParameterMapForStructureDef);
			StructureDefinition def = (StructureDefinition) results.getAllResources().get(1);

			ourLog.info(def.getId() + " is the ref id!");
			
			// test 목적으로 ID만 활용하여 맵 조회
			//StringParam param = new StringParam("2202"); // Patient 단위 테스트( 보류 )
			StringParam param = new StringParam("2252"); // HumanResource 단위
			SearchParameterMap searchParameterMapForStructureMap = new SearchParameterMap().add(StructureDefinition.SP_RES_ID, param);
			searchParameterMapForStructureMap.setSearchTotalMode(SearchTotalModeEnum.ACCURATE);
			searchParameterMapForStructureMap.setCount(1000);
			IFhirResourceDao structureMapResourceProvider = myDaoRegistry.getResourceDao("StructureMap");
			IBundleProvider resultsMap = structureMapResourceProvider.search(searchParameterMapForStructureMap);

			ourLog.info("map loaded : size : " + resultsMap.getAllResources().size());

			StructureMap map = (StructureMap) resultsMap.getAllResources().get(0);

			// 1. StructureMapUtil 이 가지는 FHIR의 StructureMap 기반의 Transaction 을 테스트한다.
			// ref. https://confluence.hl7.org/display/FHIR/Using+the+FHIR+Mapping+Language
			StructureMapUtilities utils = new StructureMapUtilities(hapiWorkerContext);

			// input
			BaseObject source = new BaseObject();
			// input.type
			source.setType("SourceClassA");

			// source.element
			source.setProperty("test", new StringType("THE VALUES...!"));

			/* 1. NO Resource to No Resource
 			BaseObject target = new BaseObject();
			utils.transform(null, source, map, target);
			*/

			/* 2. Patient 단위 테스트( 보류 )
			   - 하위 element 부터 차근차근 작업하지 않으면 안 됌. ( 2202 )
			Patient targetPatient = new Patient();
			ourLog.info(" >>>>> " + targetPatient.getProperty(3373707, "name.text", true));

			utils.transform(null, source, map, targetPatient);
			*/

			// 3. MyHumanName -> HumanName
			// 통과됌. 20923 .11. 12.
			Map<String, Base> hn = new HashMap<>();
			hn.put("nameType", new StringType("HumanName"));
			hn.put("given", new StringType("GIVEN_NAME!"));
			hn.put("family", new StringType("Family_NAME!"));
			hn.put("text", new StringType("Text_NAME!"));
			MyHumanName myHumanName = new MyHumanName(hn);
			HumanName targetHumanName = new HumanName();

			utils.transform(null, myHumanName, map, targetHumanName);

			utils.transform(targetHumanName, myHumanName, map, targetHumanName);
			ourLog.info(" >>> HumanName to do : " + getContext().newJsonParser().encodeToString(targetHumanName) + " / " + targetHumanName.getText() + " " + targetHumanName.getGiven());

			// 2. Not active
			ourLog.info("Str Def size : " + results.getAllResources().size());

			StructureMap map2 = utils.generateMapFromMappings(def);
			ourLog.info("Created Group Size : " + map.getGroup().size());

			ourLog.info("wonder.. : " + getContext().newJsonParser().encodeResourceToString(map));

			// 4. Patient 에서 HumanName 을 Child 룰로 가져올 수 있는지 확인
			param = new StringParam("2402"); // HumanResource 단위
			searchParameterMapForStructureMap = new SearchParameterMap().add(StructureDefinition.SP_RES_ID, param);
			searchParameterMapForStructureMap.setSearchTotalMode(SearchTotalModeEnum.ACCURATE);
			searchParameterMapForStructureMap.setCount(1000);
			resultsMap = structureMapResourceProvider.search(searchParameterMapForStructureMap);
			ourLog.info("map loaded : size : " + resultsMap.getAllResources().size());
			StructureMap mapQ4 = (StructureMap) resultsMap.getAllResources().get(0);
			ourLog.info(" > " + mapQ4.getId());
			
			// tip. Map 의 구조분석기. 단 SturcutreMap에 Def가 Source, Target 모두 정형화 되어있지 않으면 오류냄.
			//utils.analyse(null, mapQ4);
			Patient pt = new Patient();

			HumanName hm = new HumanName();
			hm.setText("tx");
			hm.setFamily("fm");
			hm.setGiven(new ArrayList<StringType>());
			List<HumanName> hmli = new ArrayList<HumanName>();
			hmli.add(hm);
			pt.setName(hmli);
			pt.setGender(Enumerations.AdministrativeGender.MALE);
			try {
				Property b = pt.getNamedProperty("name");
				ourLog.info(" >>> Property Test 2) name.family : " + b.getName());
			}catch(Exception e){
				ourLog.info("error skip.");
			}

			try {
				Property b = pt.getNamedProperty("gender");
				ourLog.info(" >>> Property Test 2) name.family : " + b.getName());
			}catch(Exception e){
				ourLog.info("error skip.");
			}

			// 계층구조 프로퍼티 가능?
			try {
				Property b = pt.getNamedProperty("name.family");
				ourLog.info(" >>> Property Test 2) name.family : " + b.getName());
			}catch(Exception e){
				ourLog.info("error skip.");
			}

			try {
				Property b = pt.getNamedProperty("name->family");
				ourLog.info(" >>> Property Test 2) name.family : " + b.getName());
			}catch(Exception e){
				ourLog.info("error skip.");
			}

			try {
				Property b = pt.getNamedProperty("name[0].family");
				ourLog.info(" >>> Property Test 2) name.family : " + b.getName());
			}catch(Exception e){
				ourLog.info("error skip.");
			}

			try {
				Property b = pt.getNamedProperty("name.first().family");
				ourLog.info(" >>> Property Test 2) name.family : " + b.getName());
			}catch(Exception e){
				ourLog.info("error skip.");
			}

			utils.transform(null, myHumanName, mapQ4, pt);
			ourLog.info(" >>> SubRule Patient to do : " + getContext().newJsonParser().encodeResourceToString(pt));
	}

	class BaseObject extends Base {

		private String type;

		// target.elements
		public Base testValue;

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		@Override
		public String fhirType() {
			return type;
		}

		@Override
		protected void listChildren(List<Property> list) {
			ourLog.info(" >> {DEV} listChildren");
		}

		@Override
		public String getIdBase() {
			return getIdBase();
		}

		@Override
		public void setIdBase(String s) {
			getUserData(s);
		}

		@Override
		public Base copy() {
			return copy();
		}

		@Override
		public Base setProperty(String name, Base value) throws FHIRException {
			ourLog.info(" [DEV] Custom Base setProperty init in MapUtil..!  / name : " + name + "  value : " + value);
			setUserData(name, value);
			return value;
		}

		@Override
		public Base[] getProperty(int hash, String name, boolean checkValid) throws FHIRException {
			ourLog.info(" [DEV] Custom Base getProperty init in MapUtil..!  / hash : " + hash + " name : " + name + "  checkValid : " + checkValid);
			//if (checkValid) {
			 try {
				 Base[] ba = new Base[1];
				 ba[0] = new StringType("TEST...!!");
				 return ba;
			 }catch(Exception e){
				 e.printStackTrace();
				 ourLog.error("?????????????????? " + e.getMessage());
				 return null;
			 }
				//return super.getProperty(hash, name, checkValid);
			//} else {
			//	return null;
			//}
		}

	}
}

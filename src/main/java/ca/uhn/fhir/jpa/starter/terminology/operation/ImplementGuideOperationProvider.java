package ca.uhn.fhir.jpa.starter.terminology.operation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.provider.BaseJpaProvider;
import ca.uhn.fhir.jpa.provider.ValueSetOperationProvider;
import ca.uhn.fhir.jpa.starter.terminology.config.TerminologyCodeConfigProperties;

import ca.uhn.fhir.jpa.starter.terminology.util.FHIRUtils;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 2023. 10. 19.
 * Implement 관련한 액티브를 수행한다.
 *
 */
// tip.
// http://localhost:8080/fhir/$manualInputAndOutput 으로 Post 동작 수행 시
// 해당 클래스 선언 후 startjpaConfig 에서 설정.
public class ImplementGuideOperationProvider extends BaseJpaProvider {

	private static final Logger ourLog = LoggerFactory.getLogger(ValueSetOperationProvider.class);

	private final TerminologyCodeConfigProperties terminologyCodeConfigProperties;

	public ImplementGuideOperationProvider(TerminologyCodeConfigProperties terminologyCodeConfigProperties) {
		this.terminologyCodeConfigProperties = terminologyCodeConfigProperties;
	}

	/**
	 * Server 내에 IG를 FHIR에 적재한다.(POST)
	 * 컨텐츠에 \n 소거.
	 *
	 * @param theServletRequest  the the servlet request
	 * @param theServletResponse the the servlet response
	 * @throws IOException the io exception
	 */
	@Operation(name="$input-ig", manualResponse=true, manualRequest=true)
	public void insertImplementGuideStructure(HttpServletRequest theServletRequest, HttpServletResponse theServletResponse) throws IOException {
		String contentType = theServletRequest.getContentType();
		byte[] bytes = IOUtils.toByteArray(theServletRequest.getInputStream());
		ourLog.info("Input Implement Guide Structure init...");

		try {
			// location
			ourLog.info(" - package location : " + terminologyCodeConfigProperties.getPackageAddress());
			String packageAddress = terminologyCodeConfigProperties.getPackageAddress();
			// 1. File 처리
			File dir = new File(packageAddress);
			File files[] = dir.listFiles();

			List<IBaseResource> structureResource = new ArrayList<IBaseResource>();
			List<IBaseResource> valueSetResource = new ArrayList<IBaseResource>();
			List<IBaseResource> codeSystemResource = new ArrayList<IBaseResource>();
			List<IBaseResource> searchParamResource = new ArrayList<IBaseResource>();
			List<IBaseResource> namingSystemResource = new ArrayList<IBaseResource>();
			List<IBaseResource> capabilityStatementResource = new ArrayList<IBaseResource>();

			for(int i = 0; i < files.length; i++){
				BufferedReader br = new BufferedReader(new FileReader(files[i]));
				String str = br.readLine();
				if(files[i].isFile() && !files[i].toString().contains(".json") && files[i].toString().matches("^.+\\\\..+$")){
					continue;
				}else{
					ourLog.info(" - file Name : " + files[i] );
				}

				IBaseResource thisResource = FHIRUtils.StringResourceToResource(str);
				if(thisResource.fhirType().equals("StructureDefinition")) {
					structureResource.add(thisResource);
				}else if(thisResource.fhirType().equals("ValueSet")) {
					valueSetResource.add(thisResource);
				}else if(thisResource.fhirType().equals("CodeSystem")) {
					codeSystemResource.add(thisResource);
				}else if(thisResource.fhirType().equals("SearchParameter")) {
					searchParamResource.add(thisResource);
				}else if(thisResource.fhirType().equals("NamingSystem")) {
					namingSystemResource.add(thisResource);
				}else if(thisResource.fhirType().equals("CapabilityStatement")) {
					capabilityStatementResource.add(thisResource);
				}else {
					System.out.println(">>>>> OTHER TYPE :  " + thisResource.fhirType());
				}

				br.close();
			}

			// 2. FHIR Client로 Server 호출
			FhirContext ctx = FhirContext.forR4();
			ctx.getRestfulClientFactory().setSocketTimeout(terminologyCodeConfigProperties.getTimeout());
			IGenericClient client =  ctx.newRestfulGenericClient(terminologyCodeConfigProperties.getCodeInjectTargetUrl());

			// 3. FHIR에 적재
			// valueset
			for(IBaseResource eachResource : valueSetResource){
				ourLog.info("CREATE Valueset BY " + eachResource.getIdElement());
				ValueSet valueSet = (ValueSet) eachResource;
				valueSet.setStatus(Enumerations.PublicationStatus.ACTIVE);

				ourLog.info("CREATE Type : " + client.update().resource(valueSet).execute().getId().getIdPart());
				ourLog.info("CREATE Valueset End");
			}

			// namingsystem
			for(IBaseResource eachResource : namingSystemResource){
				ourLog.info("CREATE Namingsystem BY " + eachResource.getIdElement());
				NamingSystem namingSystem = (NamingSystem) eachResource;
				namingSystem.setStatus(Enumerations.PublicationStatus.ACTIVE);

				ourLog.info("CREATE Type : " + client.update().resource(namingSystem).execute().getId().getIdPart());
				ourLog.info("CREATE Namingsystem End");
			}

			// codesystem
			for(IBaseResource eachResource : codeSystemResource){
				ourLog.info("CREATE Codesystem BY " + eachResource.getIdElement());
				CodeSystem codeSystem = (CodeSystem) eachResource;
				codeSystem.setStatus(Enumerations.PublicationStatus.ACTIVE);

				Set<String> befIDStr = new HashSet<String>();
				List<CodeSystem.ConceptDefinitionComponent> conceptDefinitionComponentList = new ArrayList<>();
				// 2023. 11. 02. CodeSystem 의 Concept 이 겹치는 경우의 대한 소거처리
				for(CodeSystem.ConceptDefinitionComponent concept : codeSystem.getConcept()){
					boolean isAlreadyHas = befIDStr.contains(concept.getCode());
					if(isAlreadyHas){
						ourLog.info(" > Duplication Id ... " + concept.getCode());
					}else{
						befIDStr.add(concept.getCode());
						conceptDefinitionComponentList.add(concept);
					}
				}

				// 해당 Concept 중에 겹치지 않는것만 처리
				codeSystem.setConcept(conceptDefinitionComponentList);

				ourLog.info("CREATE Type : " + client.update().resource(codeSystem).execute().getId().getIdPart());
				ourLog.info("CREATE Codesystem End");
			}

			// searchParam
			for(IBaseResource eachResource : searchParamResource){
				ourLog.info("CREATE SearchParam BY " + eachResource.getIdElement());
				SearchParameter searchParameter = (SearchParameter)eachResource;
				searchParameter.setStatus(Enumerations.PublicationStatus.ACTIVE);

				ourLog.info("CREATE Type : " + client.update().resource(searchParameter).execute().getId().getIdPart());
				ourLog.info("CREATE SearchParam End");
			}

			// StructureDefinition
			for(IBaseResource eachResource : structureResource){
				ourLog.info("CREATE StructureDefinition BY " + eachResource.getIdElement());
				StructureDefinition structureDefinition = (StructureDefinition)eachResource;
				structureDefinition.setStatus(Enumerations.PublicationStatus.ACTIVE);

				ourLog.info("CREATE Type : " + client.update().resource(structureDefinition).execute().getId().getIdPart());
				ourLog.info("CREATE StructureDefinition End");
			}

			// CapabilityStatement
			for(IBaseResource eachResource : capabilityStatementResource){
				ourLog.info("CREATE CapabilityStatement BY " + eachResource.getIdElement());
				CapabilityStatement capabilityStatement = (CapabilityStatement)eachResource;
				capabilityStatement.setStatus(Enumerations.PublicationStatus.ACTIVE);

				ourLog.info("CREATE Type : " + client.update().resource(capabilityStatement).execute().getId().getIdPart());
				ourLog.info("CREATE CapabilityStatement End");
			}

			ourLog.info("Received call with content type {} and {} bytes", contentType, bytes.length);
			theServletResponse.setContentType("text/plain");
			theServletResponse.getWriter().write("Implement Guide Insert Done...");
			theServletResponse.getWriter().close();

		}catch(IOException e){
			theServletResponse.setContentType("text/plain");
			theServletResponse.getWriter().write(e.getMessage());
			theServletResponse.getWriter().close();
		}
	}

	@Operation(name="$input-ig-exam", manualResponse=true, manualRequest=true)
	public void insertImplementGuideExample(HttpServletRequest theServletRequest, HttpServletResponse theServletResponse) throws IOException {
		String contentType = theServletRequest.getContentType();
		byte[] bytes = IOUtils.toByteArray(theServletRequest.getInputStream());
		ourLog.info("Input Implement Guide Example init...");

		try{
			// location
			ourLog.info(" - Package Example location : " + terminologyCodeConfigProperties.getPackageExampleAddress());
			String packageAddress = terminologyCodeConfigProperties.getPackageExampleAddress();

			// 1. File 처리
			File dir = new File(packageAddress);
			File files[] = dir.listFiles();
			List<IBaseResource> allResource = new ArrayList<IBaseResource>();
			for(int i = 0; i < files.length; i++){
				BufferedReader br = new BufferedReader(new FileReader(files[i]));
				String str = br.readLine();
				if(files[i].isFile() && !files[i].toString().contains(".json") && files[i].toString().matches("^.+\\\\..+$")){
					continue;
				}else{
					ourLog.info(" - file Name : " + files[i] );
				}

				IBaseResource thisResource = FHIRUtils.StringResourceToResource(str);
				allResource.add(thisResource);

				br.close();
			}

			// 2. FHIR Client로 Server 호출
			FhirContext ctx = FhirContext.forR4();
			ctx.getRestfulClientFactory().setSocketTimeout(terminologyCodeConfigProperties.getTimeout());
			IGenericClient client =  ctx.newRestfulGenericClient(terminologyCodeConfigProperties.getCodeInjectTargetUrl());

			// 3. FHIR에 적재
			Bundle operationBundle = new Bundle();
			List<Bundle.BundleEntryComponent> entrypComp = new ArrayList<Bundle.BundleEntryComponent>();

			for(IBaseResource eachResource : allResource) {
				Bundle.BundleEntryComponent comp = new Bundle.BundleEntryComponent();

				org.hl7.fhir.r4.model.Resource eachDefinitionResource = (org.hl7.fhir.r4.model.Resource)eachResource;
				comp.setResource(eachDefinitionResource);
				System.out.println("ID :: " + eachDefinitionResource.getId());
				System.out.println("ID BASE :: " + eachDefinitionResource.getIdBase());

				Bundle.BundleEntryRequestComponent reqComp = new Bundle.BundleEntryRequestComponent();
				reqComp.setMethod(Bundle.HTTPVerb.PUT);
				reqComp.setUrl(eachDefinitionResource.getId());
				comp.setRequest(reqComp);
				entrypComp.add(comp);
			}
			operationBundle.setEntry(entrypComp);
			client.transaction().withBundle(operationBundle).execute();

			ourLog.info(" - Received call with content type {} and {} bytes", contentType, bytes.length);
			theServletResponse.setContentType("text/plain");
			theServletResponse.getWriter().write("Implement Guide Example Insert Done...");
			theServletResponse.getWriter().close();

		}catch(IOException e){
			theServletResponse.setContentType("text/plain");
			theServletResponse.getWriter().write(e.getMessage());
			theServletResponse.getWriter().close();
		}
	}

}

package ca.uhn.fhir.jpa.starter.transfor.base.core;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.jpa.provider.BaseJpaProvider;
import ca.uhn.fhir.jpa.starter.terminology.util.FHIRUtils;
import ca.uhn.fhir.jpa.starter.validation.config.CustomValidationRemoteConfigProperties;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IOperationUnnamed;
import ca.uhn.fhir.util.ParametersUtil;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Iterator;

/**
 * 2023. 11. 24.
 * Code 간 변환로직의 대한 세부적인 정의를 나타낸다.
 * RemoteTerminolgoyServerValidationSupport.invokeRemoteValidateCode 을 전체적으로 참고하여 구성하였다.
 */
public class TranslationEngine extends BaseJpaProvider {

	private CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties;

	public TranslationEngine(FhirContext context, CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties){
		this.customValidationRemoteConfigProperties = customValidationRemoteConfigProperties;
		this.setContext(context);
	}

	/**
	 *  2023. 11. 24. 해당 source 값을 표준 규격에 알맞은 코드로 치환해준다.
	 *  해당 기능은 HAPI FHIR 의 RemoteTerminolgoyServerValidationSupport.invokeRemoteValidateCode 를 참고하여 구성하였다.
	 *
	 * @param source              the source
	 * @param sourceURL           the source url
	 * @param targetConceptSystem the target concept system
	 * @param version             the version
	 * @return the String 치환된 코드결과
	 * @throws IllegalArgumentException the illegal argument exception
	 */
	public String translateCode(@NonNull String source, @NonNull String sourceURL, @NonNull String targetConceptSystem, String version) throws IllegalArgumentException{
		// 1. 해당 코드 시스템의 local 검색(로컬 우선)
		if(customValidationRemoteConfigProperties.isLocalTerminologyYn()){
			String localUrl = customValidationRemoteConfigProperties.getLocalURL();
			String result = convert(source, sourceURL, targetConceptSystem, version, localUrl);
			if(!StringUtils.isEmpty(result)){
				return result;
			}
		}

		// 2. remote 서버에서 검색
		if(customValidationRemoteConfigProperties.isRemoteTerminologyYn()){
			String remoteUrl = customValidationRemoteConfigProperties.getRemoteURL();
			String remoteResult = convert(source, sourceURL, targetConceptSystem, version, remoteUrl);
			if(!StringUtils.isEmpty(remoteResult)){
				return remoteResult;
			}
		}

		return "";
		//throw new IllegalArgumentException("해당 코드를 찾을 수 없어 오류가 발생하였습니다. Source : " + source + " / CodeSystem URL :" + codeUrl);
	}

	private IGenericClient provideClient(String url) {
		IGenericClient retVal = this.getContext().newRestfulGenericClient(url);
		return retVal;
	}

	private String convert(@NonNull String theCode, @NonNull String theSourceUrl, @NonNull String theTargetCodeSystem, String conceptMapVersion, @NonNull String fhirServerUrl){
		// 서버 정의
		IGenericClient client = this.provideClient(fhirServerUrl);
		FhirContext fhirContext = client.getFhirContext();
		FhirVersionEnum fhirVersion = fhirContext.getVersion().getVersion();

		Parameters parameters = new Parameters();

		// Target source url
		parameters.addParameter().setName("url").setValue(new StringType(theSourceUrl.replaceAll("'", "")));

		// Target URL
		parameters.addParameter().setName("targetScope").setValue(new StringType(theTargetCodeSystem.replaceAll("'", "")));

		// Version
		if (!StringUtils.isEmpty(conceptMapVersion)){
			parameters.addParameter().setName("conceptMapVersion").setValue(new StringType(conceptMapVersion.replaceAll("'", "")));
		}

		// 최종값
		parameters.addParameter().setName("code").setValue(new StringType(theCode.replaceAll("'", "")));

		// 조회
		if(fhirVersion.isEquivalentTo(FhirVersionEnum.R4)){
			Parameters outcome = client
				.operation()
				.onType(ConceptMap.class)
				.named("$translate")
				.withParameters(parameters)
				.useHttpGet() // HTTP GET 사용
				.execute();

			if(outcome.getParameter().size() <= 0){
				throw new IllegalArgumentException("Terminology 서버간의 연동과정에서 오류가 발생하였습니다. 서버의 응답이 없습니다.");
			}
			Parameters.ParametersParameterComponent retParam = outcome.getParameter().get(0);
			if(retParam == null){
				throw new IllegalArgumentException(fhirServerUrl + " Terminology 서버간의 연동과정에서 오류가 발생하였습니다.");
			}else{
				BooleanType type = (BooleanType) retParam.getValue();
				boolean booleanValue = type.getValue();
				if(booleanValue){
					//System.out.println("...................................... it has..!!!");
					for(Parameters.ParametersParameterComponent matchParameter : outcome.getParameter()){
						//System.out.println(" >>> " + matchParameter.getName());
						if(matchParameter.getName().equals("match")){
							// 항상 가장 첫번째로 조회된 것을 리턴
							Parameters.ParametersParameterComponent partParameter = matchParameter.getPart().get(0);
							Coding coding = (Coding) partParameter.getValue();
							return coding.getCode();
						}
					}
				}else{
					throw new IllegalArgumentException(" 해당 Terminology 값이 존재하지 않습니다. " + fhirServerUrl + "  / code : " + theCode + " / url : " + theSourceUrl);
				}
			}
			throw new IllegalArgumentException(" 해당 Terminology 값이 존재하지 않습니다. " + fhirServerUrl + "  / code : " + theCode + " / url : " + theSourceUrl);
		}else{
			throw new IllegalArgumentException(" 해당 FHIR 서버 " + fhirServerUrl + " 는 R4 기반의 FHIR 서버가 아닙니다.");
		}
	}

	// CodeSystem 의 Code 중 매칭되는 것을 반환한다. remote
	private IValidationSupport.LookupCodeResult generateLookupCodeResultR4(String theCode, String theSystem, org.hl7.fhir.r4.model.Parameters outcomeR4) {
		IValidationSupport.LookupCodeResult result = new IValidationSupport.LookupCodeResult();
		result.setSearchedForCode(theCode);
		result.setSearchedForSystem(theSystem);
		result.setFound(true);
		Iterator var5 = outcomeR4.getParameter().iterator();

		while(true) {
			while(var5.hasNext()) {
				org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent parameterComponent = (org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent)var5.next();
				switch (parameterComponent.getName()) {
					case "property":
						org.hl7.fhir.r4.model.Property part = parameterComponent.getChildByName("part");
						if (part != null && part.hasValues() && part.getValues().size() >= 2) {
							String key = ((org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent)part.getValues().get(0)).getValue().toString();
							String value = ((org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent)part.getValues().get(1)).getValue().toString();
							if (!StringUtils.isEmpty(key) && !StringUtils.isEmpty(value)) {
								result.getProperties().add(new IValidationSupport.StringConceptProperty(key, value));
							}
						}
						break;
					case "designation":
						IValidationSupport.ConceptDesignation conceptDesignation = new IValidationSupport.ConceptDesignation();
						Iterator var11 = parameterComponent.getPart().iterator();

						while(var11.hasNext()) {
							org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent designationComponent = (org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent)var11.next();
							switch (designationComponent.getName()) {
								case "language":
									conceptDesignation.setLanguage(designationComponent.getValue().toString());
									break;
								case "use":
									org.hl7.fhir.r4.model.Coding coding = (org.hl7.fhir.r4.model.Coding)designationComponent.getValue();
									if (coding != null) {
										conceptDesignation.setUseSystem(coding.getSystem());
										conceptDesignation.setUseCode(coding.getCode());
										conceptDesignation.setUseDisplay(coding.getDisplay());
									}
									break;
								case "value":
									conceptDesignation.setValue(designationComponent.getValue() == null ? null : designationComponent.getValue().toString());
							}
						}

						result.getDesignations().add(conceptDesignation);
						break;
					case "name":
						result.setCodeSystemDisplayName(parameterComponent.getValue() == null ? null : parameterComponent.getValue().toString());
						break;
					case "version":
						result.setCodeSystemVersion(parameterComponent.getValue() == null ? null : parameterComponent.getValue().toString());
						break;
					case "display":
						result.setCodeDisplay(parameterComponent.getValue() == null ? null : parameterComponent.getValue().toString());
						break;
					case "abstract":
						result.setCodeIsAbstract(parameterComponent.getValue() == null ? false : Boolean.parseBoolean(parameterComponent.getValue().toString()));
				}
			}

			return result;
		}
	}


}

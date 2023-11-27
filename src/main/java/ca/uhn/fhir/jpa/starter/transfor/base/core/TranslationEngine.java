package ca.uhn.fhir.jpa.starter.transfor.base.core;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.jpa.provider.BaseJpaProvider;
import ca.uhn.fhir.jpa.starter.validation.config.CustomValidationRemoteConfigProperties;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IOperationUnnamed;
import ca.uhn.fhir.util.ParametersUtil;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.r4.model.CodeSystem;

import java.util.Iterator;

/** 2023. 11. 24.
 * Code 간 변환로직의 대한 세부적인 정의를 나타낸다.
 * RemoteTerminolgoyServerValidationSupport.invokeRemoteValidateCode 을 전체적으로 참고하여 구성하였다.
 *
 */
public class TranslationEngine extends BaseJpaProvider {

	private CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties;

	public TranslationEngine(CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties){
		this.customValidationRemoteConfigProperties = customValidationRemoteConfigProperties;
	}

	/**
	 * 2023. 11. 24. 해당 source 값을 표준 규격에 알맞은 코드로 치환해준다.
	 * 해당 기능은 HAPI FHIR 의 RemoteTerminolgoyServerValidationSupport.invokeRemoteValidateCode 를 참고하여 구성하였다.
	 *
	 * @param source  the source
	 * @param codeUrl the code url
	 * @return the string
	 * @throws IllegalArgumentException the illegal argument exception
	 */
	public String translateCode(@NonNull String source, @NonNull String codeUrl) throws IllegalArgumentException{
		// 1. 해당 코드 시스템의 local 검색
		String localUrl = customValidationRemoteConfigProperties.getLocalURL();
		System.out.println("[DEV] localURL : " + localUrl);

		IValidationSupport.LookupCodeResult result = convert(source, null, null, codeUrl, localUrl);
		if(result.isFound()){
			return result.getSearchedForCode();
		}else if(customValidationRemoteConfigProperties.isRemoteTerminologyYn()){
			// 2. 코드값이 없다면 remote  서버 체크
			String remoteUrl = customValidationRemoteConfigProperties.getRemoteURL();
			IValidationSupport.LookupCodeResult resultRemoteServer = convert(source, null, null, codeUrl, remoteUrl);
		}


		throw new IllegalArgumentException("해당 코드를 찾을 수 없어 오류가 발생하였습니다. Source : " + source + " / CodeSystem URL :" + codeUrl);
	}

	private IGenericClient provideClient(String url) {
		IGenericClient retVal = this.getContext().newRestfulGenericClient(url);
		return retVal;
	}

	private IValidationSupport.LookupCodeResult convert(@NonNull String theCode, String theSystem, String theDisplayLanguage, @NonNull String codeUrl, @NonNull String fhirServerUrl){
		// 서버 정의
		IGenericClient client = this.provideClient(fhirServerUrl);
		FhirContext fhirContext = client.getFhirContext();
		IBaseParameters params = ParametersUtil.newInstance(fhirContext);
		FhirVersionEnum fhirVersion = fhirContext.getVersion().getVersion();

		// 파라미터 추가
		ParametersUtil.addParameterToParametersString(fhirContext, params, "code", theCode);

		if (!StringUtils.isEmpty(theSystem)) {
			ParametersUtil.addParameterToParametersString(fhirContext, params, "system", theSystem);
		}

		if (!StringUtils.isEmpty(theDisplayLanguage)) {
			ParametersUtil.addParameterToParametersString(fhirContext, params, "language", theDisplayLanguage);
		}

		// 조회
		if(fhirVersion.isEquivalentTo(FhirVersionEnum.R4)){
			IBaseParameters outcome = (IBaseParameters)((IOperationUnnamed)client.operation().onType(CodeSystem.class)).named("$lookup").withParameters(params).useHttpGet().execute();
			if (outcome != null && !outcome.isEmpty()) {
				IValidationSupport.LookupCodeResult result = this.generateLookupCodeResultR4(theCode, theSystem, (org.hl7.fhir.r4.model.Parameters)outcome);
				return result;
			}else{
				throw new IllegalArgumentException(" 해당 FHIR 서버에서 코드가 존재하지 않아 오류가 발생하였습니다.");
			}
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

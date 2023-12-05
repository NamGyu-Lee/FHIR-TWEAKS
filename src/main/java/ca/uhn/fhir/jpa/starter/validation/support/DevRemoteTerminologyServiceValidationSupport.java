package ca.uhn.fhir.jpa.starter.validation.support;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.ConceptValidationOptions;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleUtil;
import org.hl7.elm.r1.Null;
import org.hl7.fhir.common.hapi.validation.support.RemoteTerminologyServiceValidationSupport;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;

/**
 *  2023. 11. 03. ValidationChain에 적용할 ValidationSupport 중
 *   기존 RemoteTerminology 가 Terminology Server 의 성능 측정 및 다각도의 활용을 위하여 재정의한 클래스
 *
 */
public class DevRemoteTerminologyServiceValidationSupport extends RemoteTerminologyServiceValidationSupport {

	private static final Logger ourLog = LoggerFactory.getLogger(RemoteTerminologyServiceValidationSupport.class);

	public DevRemoteTerminologyServiceValidationSupport(FhirContext theFhirContext) {
		super(theFhirContext);
	}

	@Override
	public IValidationSupport.CodeValidationResult validateCode(@Nonnull ValidationSupportContext theValidationSupportContext, @Nonnull ConceptValidationOptions theOptions, String theCodeSystem, String theCode, String theDisplay, String theValueSetUrl) {
		ourLog.info("--------------");
		ourLog.info(" > Remote Validation Support Operation Start.... ");
		ourLog.info("   ㄴ theCodeSystem : " + theCodeSystem);
		ourLog.info("   ㄴ code : " + theCode);
		CodeValidationResult retValidationResult = this.invokeRemoteValidateCode(theCodeSystem, theCode, theDisplay, theValueSetUrl, (IBaseResource) null);
		try {
			ourLog.info(" > Remote Validation Support Operation End Result.... : " + retValidationResult.getCode());
		}catch(NullPointerException e){
			ourLog.info(" > Remote Validation Support Operation End Result.... IS Null");
		}
		ourLog.info("--------------");
		return retValidationResult;
	}

	@Override
	public IBaseResource fetchValueSet(String theValueSetUrl){
		ourLog.info("--------------");
		try {
			ourLog.info(" > Remote ValueSet Fetch Start ....  : " + theValueSetUrl);
		}catch(NullPointerException e){
			ourLog.info(" > Remote ValueSet Fetch Start .... But ValueSetUrl Is null");
		}

		IBaseResource baseResource = super.fetchValueSet(theValueSetUrl);
		try{
			ourLog.info(" > Remote ValueSet Fetch Result ....  : " + getFhirContext().newJsonParser().encodeResourceToString(baseResource));
		}catch(NullPointerException e){
			ourLog.info(" > Remote ValueSet Fetch End Result.... IS Null");
		}
		ourLog.info("--------------");
		return baseResource;
	}
}

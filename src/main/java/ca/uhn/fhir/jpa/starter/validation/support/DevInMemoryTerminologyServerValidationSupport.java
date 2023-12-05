package ca.uhn.fhir.jpa.starter.validation.support;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.ConceptValidationOptions;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import org.hl7.elm.r1.Null;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.RemoteTerminologyServiceValidationSupport;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class DevInMemoryTerminologyServerValidationSupport extends InMemoryTerminologyServerValidationSupport {

	private static final Logger ourLog = LoggerFactory.getLogger(RemoteTerminologyServiceValidationSupport.class);

	public DevInMemoryTerminologyServerValidationSupport(FhirContext theCtx) {
		super(theCtx);
	}

	public IValidationSupport.CodeValidationResult validateCodeInValueSet(ValidationSupportContext theValidationSupportContext, ConceptValidationOptions theOptions, String theCodeSystemUrlAndVersion, String theCode, String theDisplay, @Nonnull IBaseResource theValueSet) {
		try {
			ourLog.info("--------------");
			ourLog.info(" > InMemory Validation Support.validateCodeInValueSet Start... ");
			ourLog.info("   ㄴ Codsystem URL and Version : " + theCodeSystemUrlAndVersion);
			ourLog.info("   ㄴ code : " + theCode);
			super.getFhirContext().newJsonParser().encodeResourceToString(theValueSet);
		}catch(NullPointerException e){
			ourLog.info(" > InMemory Validation Support.validateCodeInValueSet Start... but codesystem or code is null");
		}

		IValidationSupport.CodeValidationResult codeValidationResult = super.validateCodeInValueSet(theValidationSupportContext, theOptions, theCodeSystemUrlAndVersion, theCode, theDisplay, theValueSet);
		try{
			ourLog.info(" > codeValidationResult Result ... ");
			ourLog.info("   ㄴ code : " + codeValidationResult.getCode());
			ourLog.info("   ㄴ message : " + codeValidationResult.getMessage());
		}catch(NullPointerException e){
			ourLog.info(" > codeValidationResult Result is Null ... ");
		}
		ourLog.info("--------------");
		return codeValidationResult;
	}

	public IValidationSupport.CodeValidationResult validateCode(@Nonnull ValidationSupportContext theValidationSupportContext, @Nonnull ConceptValidationOptions theOptions, String theCodeSystem, String theCode, String theDisplay, String theValueSetUrl) {
		try {
			ourLog.info("--------------");
			ourLog.info(" > InMemory Validation Support.validateCode Start... ");
			ourLog.info("   ㄴ system : " + theCodeSystem);
			ourLog.info("   ㄴ code : " + theCode);
		}catch(NullPointerException e){
			ourLog.info(" > InMemory Validation Support.validateCode Start but code, codesystem is null ... ");
		}

		IValidationSupport.CodeValidationResult codeValidationResult = super.validateCode(theValidationSupportContext, theOptions, theCodeSystem, theCode, theDisplay, theValueSetUrl);

		try{
			ourLog.info(" > InMemory Validation Support.validateCode Result... ");
			ourLog.info("   ㄴ code : " + codeValidationResult.getCode());
			ourLog.info("   ㄴ message : " + codeValidationResult.getMessage());
		}catch(NullPointerException e){
			ourLog.info(" > InMemory Validation Support.validateCode result is null... ");
		}
		ourLog.info("--------------");
		return codeValidationResult;
	}


}

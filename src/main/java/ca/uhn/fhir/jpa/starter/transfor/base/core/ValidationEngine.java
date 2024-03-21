package ca.uhn.fhir.jpa.starter.transfor.base.core;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.starter.terminology.util.FHIRUtils;
import ca.uhn.fhir.jpa.starter.validation.config.CustomValidationRemoteConfigProperties;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.OperationOutcome;

public class ValidationEngine{

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(TransformEngine.class);

	CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties;

	FhirContext context;

	public ValidationEngine(FhirContext context, CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties){
		this.context = context;
		this.customValidationRemoteConfigProperties = customValidationRemoteConfigProperties;
	}

	public boolean executeValidation(IBaseResource resource, boolean isFailThenExcept) throws IllegalArgumentException {
		ourLog.info("------------------------------");
		if (customValidationRemoteConfigProperties.isLocalTerminologyYn()) {
			// 로컬
			FhirValidator validator = context.newValidator();
			ValidationResult result = validator.validateWithResult(resource);
			if (result.isSuccessful()) {
				ourLog.info("[Validation] validation 이 성공하였습니다.");
				ourLog.info("------------------------------");
				return true;
			} else {

				ourLog.info("[Validation] validation 수행이 실패하였습니다.");
				result.getMessages().forEach(singleValidationMessage -> {
					ourLog.info("Issue: " + singleValidationMessage.getMessage());
				});

				if (isFailThenExcept) {
					throw new IllegalArgumentException("해당 " + resource.fhirType() + " 의 리소스의 Validation 이 실패하였습니다.");
				}else{
					ourLog.info("------------------------------");
					return false;
				}
			}
		}else{
			// 외부
			FhirContext ctx = FhirContext.forR4();
			IGenericClient client = ctx.newRestfulGenericClient(customValidationRemoteConfigProperties.getRemoteURL());
			MethodOutcome methodOutcome = client.validate().resource(resource).execute();
			OperationOutcome outcome = (OperationOutcome) methodOutcome.getOperationOutcome();
			for(OperationOutcome.OperationOutcomeIssueComponent eachComp : outcome.getIssue()){
				OperationOutcome.IssueSeverity thisIssueSeverity = eachComp.getSeverity();

				if(thisIssueSeverity.equals(OperationOutcome.IssueSeverity.ERROR)
				|| thisIssueSeverity.equals(OperationOutcome.IssueSeverity.FATAL)){
					ourLog.error("원격지 서버의 Validation  과정에서 다음과 같은 에러가 발생하였습니다.");
					ourLog.error(FHIRUtils.resourceToString(outcome));
					if (isFailThenExcept) {
						throw new IllegalArgumentException("해당 " + resource.fhirType() + " 의 리소스의 Validation 이 실패하였습니다.");
					}else{
						ourLog.info("------------------------------");
						return false;
					}
				}else{
					String validationLoggingLevel = customValidationRemoteConfigProperties.getRemoteValidationLoggingLevel();
					if(validationLoggingLevel.equals("warn")){
						if(thisIssueSeverity.equals(OperationOutcome.IssueSeverity.WARNING)){
							ourLog.warn("[WARN] 아래의 결과는 warn 으로 처리된 Remote Validation 결과입니다. ");
							ourLog.warn(FHIRUtils.resourceToString(outcome));
						}
					}else if(validationLoggingLevel.equals("info")){
						ourLog.info(FHIRUtils.resourceToString(outcome));
					}
					ourLog.info("------------------------------");
				}
			}
			ourLog.info("------------------------------");
			return true;
		}
	}
}

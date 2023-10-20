package ca.uhn.fhir.jpa.starter.terminology.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.ResponseDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;


// TODO) CONFIG) 프로젝트 목적성에 맞게 Interceptor 를 구성필요.
/**
 *  2023. 10.
 *  FHIR 에서 IN/OUT 되는 요청사항의 대하여 인터셉터들을 정의한다.
 *  https://hapifhir.io/hapi-fhir/docs/interceptors/server_pointcuts.html
 *
 *  인터셉터를 class를 통해 등록하고 난 뒤에는
 *  Interceptor 별로 application.yaml 에 사전에 정의해놓아야 한다.
 *  EX) custom-interceptor-classes:  ca.uhn.fhir.jpa.starter.intercept.BaseInterceptor, ca.uhn.fhir.jpa.starter.intercept.NonBaseInterceptor
 *
 */
@Configuration
public class SampleInterceptor {

	private static final Logger ourLog = LoggerFactory.getLogger(SampleInterceptor.class);

	@Hook(value = Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
	public void handleServerIncommingRequestPre(RequestDetails theRequestDetails, ResponseDetails theResponseDetails, IBaseResource theResource) {
		// Authorization
		ourLog.info(" 1.1.1. SERVER_INCOMING_REQUEST_PRE_HANDLED.Authoriziation Interceptor Handler");
		ourLog.info("Request Path : " + theRequestDetails.getRequestPath());
		ourLog.info("TenantId : " + theRequestDetails.getTenantId());

		// test
		//theRequestDetails.setRequestPath("Patient/2");

		// Consent Service
		ourLog.info(" 1.1.2. SERVER_INCOMING_REQUEST_PRE_HANDLED.Consent Interceptor Handler");
	}

}

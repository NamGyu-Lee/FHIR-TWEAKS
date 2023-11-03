package ca.uhn.fhir.jpa.starter.terminology.interceptor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.starter.terminology.config.TerminologySearchConfigProperties;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.ResponseDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;

/**
 *  2023. 10. Terminology 를 위한 신규 Interceptor 구성
 *
 */
@Configuration
public class TerminologyInterceptor {

	private static final Logger ourLog = LoggerFactory.getLogger(TerminologyInterceptor.class);

	private TerminologySearchConfigProperties terminologySearchConfigProperties;

	@Autowired
	public TerminologyInterceptor(TerminologySearchConfigProperties terminologySearchConfigProperties) {
		this.terminologySearchConfigProperties = terminologySearchConfigProperties;
	}

	/**
	 * 2023. 10.
	 *  사용자가 FHIR Terminology 서버에 접근하여 데이터를 요청하는 경우
	 *  요청한 CodeSystem 이 수많은 값을 가지고 있는 경우가 많아
	 *  이러한 경우 summary 처리를 하여 리소스 낭비를 줄인다.
	 *
	 * @param theRequestDetails  the request details
	 * @param theResponseDetails the response details
	 * @param theResource        the resource
	 */


	@Hook(value = Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
	public void handleIncommingRequestPreHandleWithCodeSystem(RequestDetails theRequestDetails, ResponseDetails theResponseDetails, IBaseResource theResource) {
		/*
		ourLog.info(" 1.1.1. SERVER_INCOMING_REQUEST_PRE_HANDLED.Authoriziation Interceptor Handler");
		ourLog.info("Request Path : " + theRequestDetails.getRequestPath());

		String requestDetail = theRequestDetails.getRequestPath();

		String[] arg = {"true"};
		if(!requestDetail.contains("_summary")) {
			if (requestDetail.contains("CodeSystem") && terminologySearchConfigProperties.getCodeSystemSummaryBoolStr().equals("true")){
				theRequestDetails.addParameter("_summary", arg);
			}else if(requestDetail.contains("ValueSet") && terminologySearchConfigProperties.getValuesetSummaryBoolStr().equals("true")){
				theRequestDetails.addParameter("_summary", arg);
			}else if(requestDetail.contains("SearchParameter")  && terminologySearchConfigProperties.getSearchParameterSummaryBoolStr().equals("true")){
				theRequestDetails.addParameter("_summary", arg);
			}
		}
		*/

	}

}

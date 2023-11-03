package ca.uhn.fhir.jpa.starter.terminology.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.ResponseDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.interceptor.InterceptorOrders;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Enumerations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** 2023. 10.
 *  PHIS 기준의 리턴에 출력할 헤더를 위한 인터셉터.
 *
 *  필요 시 restfulServer.registerInterceptor(new PhisResponseInterceptor()); 같은 방식으로 적용한다.
 */
@Interceptor
public class PhisResponseInterceptor {

	@Hook(value = Pointcut.SERVER_OUTGOING_RESPONSE, order = InterceptorOrders.RESPONSE_HIGHLIGHTER_INTERCEPTOR)
	public boolean outgoingResponse(RequestDetails theRequestDetails, ResponseDetails theResponseObject, HttpServletRequest theServletRequest, HttpServletResponse theServletResponse)
		throws AuthenticationException {
		//System.out.println(">>> SERVER_OUTGOING_RESPONSE ");
		theServletResponse.addHeader("User-Agent", "PHIS-FHIR-SVR-20230526");
		return true;
	}

	@Hook(value = Pointcut.SERVER_PRE_PROCESS_OUTGOING_EXCEPTION, order = InterceptorOrders.RESPONSE_HIGHLIGHTER_INTERCEPTOR)
	public void failureOutgoingResponse(RequestDetails theRequestDetails, ServletRequestDetails srd, Throwable t, HttpServletRequest req, HttpServletResponse theServletResponse) {
		//System.out.println(">>> SERVER_PRE_PROCESS_OUTGOING_EXCEPTION ");
		theServletResponse.addHeader("User-Agent", "PHIS-FHIR-SVR-20230526");
	}

	@Hook(Pointcut.SERVER_CAPABILITY_STATEMENT_GENERATED)
	public void customize(IBaseConformance theCapabilityStatement) {
		// Cast to the appropriate version
		CapabilityStatement cs = (CapabilityStatement) theCapabilityStatement;

		// Customize the CapabilityStatement as desired
		cs.setName("PHIS_FHIR_Server");
		cs.setTitle("PHIS FHIR Server");
		cs.setStatus(Enumerations.PublicationStatus.ACTIVE);
		cs.setPublisher("주)평화이즈");
	}
}
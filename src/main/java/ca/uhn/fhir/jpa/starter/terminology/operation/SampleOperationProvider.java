package ca.uhn.fhir.jpa.starter.terminology.operation;

import ca.uhn.fhir.jpa.provider.BaseJpaProvider;
import ca.uhn.fhir.jpa.provider.ValueSetOperationProvider;
import ca.uhn.fhir.model.primitive.DateDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// TODO SAMPLE) FHIR서버 내 커스텀 오퍼레이션(Controller) 구현체. 사용자 필요에 따라 개발
/**
 * 2023. 10.
 * 나중에 잊을 가능성이 있어 구성
 * HAPI FHIR 기반의 JPA Project에 니즈에 맞는 Operation Provider 생성
 *
 * 작업 후 StarterJPAConfig 에서 해당 provider 를 FHIR서버에 IOC로 추가해야한다.
 * EX)
 * SampleOperationProvider testOperationProvider = new SampleOperationProvider();
 * fhirServer.registerProvider(SampleOperationProvider);
 *
 */
public class SampleOperationProvider extends BaseJpaProvider {
	private static final Logger ourLog = LoggerFactory.getLogger(ValueSetOperationProvider.class);

	// 1. 리소스 단위 커스텀 오퍼레이션 프로바이더
	// get 에 응답한다
	// operationParam 이라는 값을 활용할 수 있다.
	// localhost:8080/fhir/Patient/$everything
	@Operation(name = "$sample-everything", idempotent = true)
	public Bundle patientTypeOperation(
		@OperationParam(name = "start") DateDt theStart, @OperationParam(name = "end") DateDt theEnd) {

		Bundle retVal = new Bundle();
		// Populate bundle with matching resources
		return retVal;
	}

	// 2. 인스턴스 단위 커스텀 오퍼레이션 프로바이더
	// get 에 응답한다
	// IdParam에 해당 ID 값이 들어온다
	// localhost:8080/fhir/Patient/123/$everything
	@Operation(name = "$sample-target-everything", idempotent = true)
	public Bundle patientInstanceOperation(
		@IdParam IdType thePatientId,
		@OperationParam(name = "start") DateDt theStart,
		@OperationParam(name = "end") DateDt theEnd) {

		Bundle retVal = new Bundle();
		// Populate bundle with matching resources
		return retVal;
	}

	// 3. 서버단위 활용
	// POST에만 수행 가능하다
	// theServletResponse 에 write으로 리턴하는 방식.
	// locahost:8080/fhir/$sample-custom-operation
	@Operation(name="$sample-custom-operation", manualResponse=true, manualRequest=true)
	public void manualInputAndOutput(HttpServletRequest theServletRequest, HttpServletResponse theServletResponse) throws IOException {
		String contentType = theServletRequest.getContentType();
		byte[] bytes = IOUtils.toByteArray(theServletRequest.getInputStream());
		ourLog.debug("Input Implement Guide init...");

		try {
			ourLog.info("Received call with content type {} and {} bytes", contentType, bytes.length);
			theServletResponse.setContentType("text/plain");
			theServletResponse.getWriter().write("사용자에게 이 메세지가 리턴되요.");
			theServletResponse.getWriter().close();
		}catch(IOException e){
			theServletResponse.setContentType("text/plain");
			theServletResponse.getWriter().write(e.getMessage());
			theServletResponse.getWriter().close();
		}
	}

}

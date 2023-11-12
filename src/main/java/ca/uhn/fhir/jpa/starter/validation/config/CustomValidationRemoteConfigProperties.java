package ca.uhn.fhir.jpa.starter.validation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/** 2023. 11. 06.
 *  FHIR 서비스 중 Remote서버에 접근하는 구조의 Terminology 를 구성한다.
 *   ※ instance Validaiton 위주의 동작구성이 요구된다.
 */

@Configuration
public class CustomValidationRemoteConfigProperties {

	@Value("${service.validation.remote.server.enabled}")
	private boolean remoteTerminologyYn;

	@Value("${service.validation.remote.server.url}")
	private String remoteURL;

	public boolean isRemoteTerminologyYn() {
		return remoteTerminologyYn;
	}

	public String getRemoteURL() {
		return remoteURL;
	}
}
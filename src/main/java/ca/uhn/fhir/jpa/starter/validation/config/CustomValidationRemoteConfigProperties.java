package ca.uhn.fhir.jpa.starter.validation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/** 2023. 11. 06.
 *  FHIR 서비스 중 Remote서버에 접근하는 구조의 Terminology 를 구성한다.
 *   ※ instance Validaiton 위주의 동작구성이 요구된다.
 */

@Configuration
public class CustomValidationRemoteConfigProperties {

	@Value("${hapi.fhir.tester.home.server_address}")
	private String localURL;

	@Value("${service.validation.local.enabled}")
	private boolean localTerminologyYn;

	@Value("${service.validation.remote.server.enabled}")
	private boolean remoteTerminologyYn;

	@Value("${service.validation.remote.server.url}")
	private String remoteURL;

	@Value("${service.validation.enabled}")
	private boolean validationYn;

	@Value("${service.validation.remote.logginglevel}")
	private String remoteValidationLoggingLevel;

	public boolean isLocalTerminologyYn() {return localTerminologyYn;}

	public boolean isRemoteTerminologyYn() {
		return remoteTerminologyYn;
	}

	public boolean isValidationYn() {return validationYn;}

	public String getRemoteURL() {
		return remoteURL;
	}

	public String getLocalURL() {	return localURL; }

	public String getRemoteValidationLoggingLevel() {
		return remoteValidationLoggingLevel;
	}

	/**
	 * TEST ONLY
	 *
	 * @param localURL the local url
	 */
	public void setLocalURL(String localURL) {
		this.localURL = localURL;
	}

	/**
	 * TEST ONLY
	 *
	 */
	public void setRemoteTerminologyYn(boolean remoteTerminologyYn) {
		this.remoteTerminologyYn = remoteTerminologyYn;
	}

	/**
	 * TEST ONLY
	 *
	 * @param remoteURL the local url
	 */
	public void setRemoteURL(String remoteURL) {
		this.remoteURL = remoteURL;
	}
}

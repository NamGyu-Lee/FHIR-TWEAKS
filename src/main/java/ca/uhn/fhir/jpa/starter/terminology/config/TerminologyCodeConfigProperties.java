package ca.uhn.fhir.jpa.starter.terminology.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *  2023. 10. 19. Terminology 서비스 작업을 위한 설정 구성
 */
@Configuration
//@ConfigurationProperties()
public class TerminologyCodeConfigProperties {

	@Value("${service.terminology.ig.location}")
	private String packageAddress;

	@Value("${service.terminology.ig.examplelocation}")
	private String packageExampleAddress;

	@Value("${service.terminology.common.ig.timeout}")
	private int timeout;

	@Value("${hapi.fhir.tester.home.server_address}")
	private String codeInjectTargetUrl;

	@Bean
	public String getPackageAddress() {
		return packageAddress;
	}

	public String getPackageExampleAddress() {
		return packageExampleAddress;
	}

	public int getTimeout() {
		return timeout;
	}

	public String getCodeInjectTargetUrl() {
		return codeInjectTargetUrl;
	}
}

package ca.uhn.fhir.jpa.starter.validation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/** 2023. 11.
 *  Server 의 Validaiton 의 기본적인 설정을 정의한다.
 */
@Configuration
public class CustomValidationBaseConfigProperties {

	@Value("${service.validation.enabled}")
	private boolean enableValidation;

	public boolean isEnableValidation() {
		return enableValidation;
	}
}

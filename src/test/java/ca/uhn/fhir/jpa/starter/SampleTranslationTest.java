package ca.uhn.fhir.jpa.starter;


import ca.uhn.fhir.jpa.starter.transfor.base.core.TranslationEngine;
import ca.uhn.fhir.jpa.starter.validation.config.CustomValidationRemoteConfigProperties;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringBootTest
@SpringJUnitConfig
public class SampleTranslationTest {

	private CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties
		= new CustomValidationRemoteConfigProperties();

	@Test
	public void codeTranslateTest(){
		customValidationRemoteConfigProperties.setRemoteTerminologyYn(true);
		customValidationRemoteConfigProperties.setLocalURL("http://fhrdev.cmcnu.or.kr/fhir");
		customValidationRemoteConfigProperties.setRemoteURL("http://fhrdev.cmcnu.or.kr/fhir");
		//TranslationEngine translationEngine = new TranslationEngine(customValidationRemoteConfigProperties);

		//System.out.println(translationEngine.translateCode("나", "http://terminology.hl7.org/CodeSystem/OrganizationTypes"));
	}

}

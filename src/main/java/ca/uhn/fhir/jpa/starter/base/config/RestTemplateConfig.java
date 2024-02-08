package ca.uhn.fhir.jpa.starter.base.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 *  2023. 12. 22. MSA 를 위한 RestTemplate 설정 구성 테스트
 *
 */
@Configuration
public class RestTemplateConfig {

	@Bean
	public ClientHttpRequestFactory clientHttpRequestFactory(){
		SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
		clientHttpRequestFactory.setConnectTimeout(300000);
		clientHttpRequestFactory.setReadTimeout(100000);
		clientHttpRequestFactory.setBufferRequestBody(false);

		return clientHttpRequestFactory;
	}


	@Bean
	public RestTemplate restTemplate(ClientHttpRequestFactory clientHttpRequestFactory){
		RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory);
		// restTemplate.setErrorHandler(new DefaultResponseErrorHandler());
		return restTemplate;
	}

}

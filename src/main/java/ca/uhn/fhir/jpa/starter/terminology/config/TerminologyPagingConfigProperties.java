package ca.uhn.fhir.jpa.starter.terminology.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TerminologyPagingConfigProperties {

	@Value("${service.terminology.common.paging.defaultsize}")
	private int pagingDefaultSize;

	@Value("${service.terminology.common.paging.maxsize}")
	private int pagingMaxSize;

	@Value("${service.terminology.common.paging.fifosize}")
	private int fifoPagingSize;

	public int getPagingDefaultSize() {
		return pagingDefaultSize;
	}

	public int getPagingMaxSize() {
		return pagingMaxSize;
	}

	public int getFifoPagingSize() {
		return fifoPagingSize;
	}
}

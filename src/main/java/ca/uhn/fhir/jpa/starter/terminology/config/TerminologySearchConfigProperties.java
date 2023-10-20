package ca.uhn.fhir.jpa.starter.terminology.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 *  2023. 10.
 *  조회 관련한 설정의 대하여 일괄 정의한다.
 */
@Configuration
public class TerminologySearchConfigProperties {

	@Value("${service.terminology.common.search.summary.codesystem}")
	private String codeSystemSummaryBoolStr;

	@Value("${service.terminology.common.search.summary.valueset}")
	private String valuesetSummaryBoolStr;

	@Value("${service.terminology.common.search.summary.searchparameter}")
	private String searchParameterSummaryBoolStr;

	public String getCodeSystemSummaryBoolStr() {
		return codeSystemSummaryBoolStr;
	}

	public String getValuesetSummaryBoolStr() {
		return valuesetSummaryBoolStr;
	}

	public String getSearchParameterSummaryBoolStr() {
		return searchParameterSummaryBoolStr;
	}
}

package ca.uhn.fhir.jpa.starter.common.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.interceptor.validation.IRepositoryValidatingRule;
import ca.uhn.fhir.jpa.interceptor.validation.RepositoryValidatingInterceptor;
import ca.uhn.fhir.jpa.interceptor.validation.RepositoryValidatingRuleBuilder;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import ca.uhn.fhir.jpa.starter.terminology.interceptor.TerminologyInterceptor;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.SearchContainedModeEnum;
import ca.uhn.fhir.rest.api.SearchTotalModeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ca.uhn.fhir.jpa.starter.common.validation.IRepositoryValidationInterceptorFactory.ENABLE_REPOSITORY_VALIDATING_INTERCEPTOR;

/**
 * This class can be customized to enable the {@link ca.uhn.fhir.jpa.interceptor.validation.RepositoryValidatingInterceptor}
 * on this server.
 * <p>
 * The <code>enable_repository_validating_interceptor</code> property must be enabled in <code>application.yaml</code>
 * in order to use this class.
 */
@ConditionalOnProperty(prefix = "hapi.fhir", name = ENABLE_REPOSITORY_VALIDATING_INTERCEPTOR, havingValue = "true")
@Configuration
@Conditional(OnR4Condition.class)
public class RepositoryValidationInterceptorFactoryR4 implements IRepositoryValidationInterceptorFactory {

	private static final Logger ourLog = LoggerFactory.getLogger(RepositoryValidationInterceptorFactoryR4.class);

	private final FhirContext fhirContext;
	private final RepositoryValidatingRuleBuilder repositoryValidatingRuleBuilder;
	private final IFhirResourceDao structureDefinitionResourceProvider;

	public RepositoryValidationInterceptorFactoryR4(RepositoryValidatingRuleBuilder repositoryValidatingRuleBuilder, DaoRegistry daoRegistry) {
		this.repositoryValidatingRuleBuilder = repositoryValidatingRuleBuilder;
		this.fhirContext = daoRegistry.getSystemDao().getContext();
		structureDefinitionResourceProvider = daoRegistry.getResourceDao("StructureDefinition");

	}

	/* 2023. 11. 03. Instance Validation vs Repository Validation 관련 이슈
	* Instanace 의 경우 Validation 의 대하여 Chain 구조로 적용
	* Repository 의 경우 사전에 있는 구조에 Structure만 적용시키고, 나머지는 알아서 소스가 수행
	* 양 쪽 모두 Structure Definition 의 대하여 정상 처리 확인 완료.
	* 다만 ValueSet, CodeSystem 부분에사 양쪽에서 차이가 존재함.
	*
	* instance 의 경우 ValueSet, CodeSystem 이 없는 경우 Terminology 서버에 접근하여 확인하는 Chain 적용이 가능하지만,
	* Repository에서는 직접 해당 서버에 소스가 구현되어있음을 권장함.
	*
	* SmileCDR 의 경우 Repository Validaiton 을 활용하여 서비스의 validation용 서버를 요청하는 방식으로 서비스를 구현하는것을 선호함.
	*
	* 따라서 프로비저닝 시 구성요건을 아래와같이 정의할 수 있음
	*  1. Storage 목적의 FHIR 서버와 Repository Validation 서버간의 연동을 통한 Validation 수행
	*
	*  2. Storage 목적의 FHIR 서버와 instance 서버간의 연동을 수행하고, 연동된 서버 위에 Terminology Server를 구현하여
	*     Terminology 의 서비스 정의
	*
	* */
	@Override
	public RepositoryValidatingInterceptor buildUsingStoredStructureDefinitions() {
		// 2023. 11. 01. 내부에서 정의한 Structure Definition 를 일괄 조회해서 적용하기 위한 용도로 활용.
		// 개별 리소스별로 핸들링하는건 아래 Build Function에서 예시처럼 작업하면 됌.
		//IBundleProvider results = structureDefinitionResourceProvider.search(new SearchParameterMap().add(StructureDefinition.SP_TOKEN, new Token("resource")));
		// 해당 조회된 results 의 StructureDefinition 들만 기능적으로 RepositoryValidation 을 수행함.
		// Extension 의 대하여 정의된 StructureDefinition 은 Validation 등록과정이 해당되지 않으므로 수행치 않음.
		IBaseCoding serachCode = new Coding();
		serachCode.setCode("resource");
		TokenParam searchToken = new TokenParam(serachCode);
		SearchParameterMap searchParameterMap = new SearchParameterMap().add(StructureDefinition.SP_KIND , searchToken);
		searchParameterMap.setSearchTotalMode(SearchTotalModeEnum.ACCURATE);
		searchParameterMap.setCount(1000);

		ourLog.info("Build Strcture Definition Validation.... search Contain Mod :  " + searchParameterMap.getSearchContainedMode().name());
		ourLog.info("Build Strcture Definition Validation.... search Total Mod :  " + searchParameterMap.getSearchTotalMode().name());
		ourLog.info("Build Strcture Definition Validation.... search Normaized Query String :  " + searchParameterMap.toNormalizedQueryString(this.fhirContext));

		IBundleProvider results = structureDefinitionResourceProvider.search(searchParameterMap);
		ourLog.info("Build Strcture Definition Validation.... searched Count : " + results.getAllResources().size());

		for(IBaseResource eachResource : results.getAllResources()){
			StructureDefinition def = (StructureDefinition)eachResource;
			ourLog.debug(" > Searched Def url : " + def.getUrl());
		}

		Map<String, List<StructureDefinition>> structureDefintions = results.getResources(0, results.size())
			.stream()
			.map(StructureDefinition.class::cast)
			.collect(Collectors.groupingBy(StructureDefinition::getType));

		structureDefintions.forEach((key, value) -> {
			String[] urls = value.stream().map(StructureDefinition::getUrl).toArray(String[]::new);
			repositoryValidatingRuleBuilder.forResourcesOfType(key).requireAtLeastOneProfileOf(urls).and().requireValidationToDeclaredProfiles()
				.allowAnyExtensions()
				//.disableTerminologyChecks() // 2023. 11. 02. Terminology 체크 끄기
				//.suppressWarningForExtensibleValueSetValidation() // ValueSet 기반 Validation 억제
				//.neverReject()
				;
		});

		List<IRepositoryValidatingRule> rules = repositoryValidatingRuleBuilder.build();
		return new RepositoryValidatingInterceptor(fhirContext, rules);
	}

	@Override
	public RepositoryValidatingInterceptor build() {
		// Customize the ruleBuilder here to have the rules you want! We will give a simple example
		// of enabling validation for all Patient resources
		repositoryValidatingRuleBuilder.forResourcesOfType("Patient").requireAtLeastProfile("http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient").and().requireValidationToDeclaredProfiles();

		// Do not customize below this line
		List<IRepositoryValidatingRule> rules = repositoryValidatingRuleBuilder.build();
		return new RepositoryValidatingInterceptor(fhirContext, rules);
	}

}

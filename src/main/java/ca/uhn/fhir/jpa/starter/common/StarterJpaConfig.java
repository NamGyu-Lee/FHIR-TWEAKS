package ca.uhn.fhir.jpa.starter.common;

import ca.uhn.fhir.batch2.coordinator.JobDefinitionRegistry;
import ca.uhn.fhir.batch2.jobs.imprt.BulkDataImportProvider;
import ca.uhn.fhir.batch2.jobs.reindex.ReindexJobParameters;
import ca.uhn.fhir.batch2.jobs.reindex.ReindexProvider;
import ca.uhn.fhir.batch2.model.JobDefinition;
import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;

import ca.uhn.fhir.interceptor.api.IInterceptorBroadcaster;
import ca.uhn.fhir.jpa.api.IDaoRegistry;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.api.config.ThreadPoolFactoryConfig;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.binary.interceptor.BinaryStorageInterceptor;
import ca.uhn.fhir.jpa.binary.provider.BinaryAccessProvider;
import ca.uhn.fhir.batch2.jobs.export.BulkDataExportProvider;
import ca.uhn.fhir.jpa.config.util.HapiEntityManagerFactoryUtil;
import ca.uhn.fhir.jpa.config.util.ResourceCountCacheUtil;
import ca.uhn.fhir.jpa.config.util.ValidationSupportConfigUtil;
import ca.uhn.fhir.jpa.dao.FulltextSearchSvcImpl;
import ca.uhn.fhir.jpa.dao.IFulltextSearchSvc;
import ca.uhn.fhir.jpa.dao.search.HSearchSortHelperImpl;
import ca.uhn.fhir.jpa.dao.search.IHSearchSortHelper;
import ca.uhn.fhir.jpa.delete.ThreadSafeResourceDeleterSvc;
import ca.uhn.fhir.jpa.graphql.GraphQLProvider;
import ca.uhn.fhir.jpa.interceptor.CascadingDeleteInterceptor;
import ca.uhn.fhir.jpa.interceptor.validation.RepositoryValidatingInterceptor;
import ca.uhn.fhir.jpa.ips.provider.IpsOperationProvider;
import ca.uhn.fhir.jpa.packages.IPackageInstallerSvc;
import ca.uhn.fhir.jpa.packages.PackageInstallationSpec;
import ca.uhn.fhir.jpa.partition.PartitionManagementProvider;
import ca.uhn.fhir.jpa.provider.*;
import ca.uhn.fhir.jpa.provider.dstu3.JpaConformanceProviderDstu3;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.search.IStaleSearchDeletingSvc;
import ca.uhn.fhir.jpa.search.StaleSearchDeletingSvcImpl;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.jpa.starter.annotations.OnCorsPresent;
import ca.uhn.fhir.jpa.starter.annotations.OnImplementationGuidesPresent;
import ca.uhn.fhir.jpa.starter.common.validation.IRepositoryValidationInterceptorFactory;
import ca.uhn.fhir.jpa.starter.terminology.config.TerminologyCodeConfigProperties;
import ca.uhn.fhir.jpa.starter.terminology.config.TerminologyPagingConfigProperties;
import ca.uhn.fhir.jpa.starter.transfor.config.TransformDataOperationConfigProperties;
import ca.uhn.fhir.jpa.starter.transfor.operation.ResourceTransEngineOperationProvider;
import ca.uhn.fhir.jpa.starter.transfor.operation.ResourceTransformProvider;
import ca.uhn.fhir.jpa.starter.transfor.operation.ResourceTransforOperationProvider;
import ca.uhn.fhir.jpa.starter.validation.config.CustomValidationBaseConfigProperties;
import ca.uhn.fhir.jpa.starter.validation.config.CustomValidationRemoteConfigProperties;
import ca.uhn.fhir.jpa.starter.terminology.config.TerminologySearchConfigProperties;
import ca.uhn.fhir.jpa.starter.terminology.operation.ImplementGuideOperationProvider;
import ca.uhn.fhir.jpa.starter.terminology.paging.TerminologyPagingProvider;
import ca.uhn.fhir.jpa.starter.validation.support.DevInMemoryTerminologyServerValidationSupport;
import ca.uhn.fhir.jpa.starter.validation.support.DevRemoteTerminologyServiceValidationSupport;
import ca.uhn.fhir.jpa.starter.util.EnvironmentHelper;
import ca.uhn.fhir.jpa.subscription.util.SubscriptionDebugLogInterceptor;
import ca.uhn.fhir.jpa.util.ResourceCountCache;
import ca.uhn.fhir.jpa.validation.JpaValidationSupportChain;
import ca.uhn.fhir.mdm.provider.MdmProviderLoader;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.narrative2.NullNarrativeGenerator;
import ca.uhn.fhir.rest.api.IResourceSupportedSvc;
import ca.uhn.fhir.rest.api.SearchTotalModeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.openapi.OpenApiInterceptor;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.*;
import ca.uhn.fhir.rest.server.interceptor.*;
import ca.uhn.fhir.rest.server.interceptor.partition.RequestTenantPartitionInterceptor;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import ca.uhn.fhir.rest.server.tenant.UrlBaseTenantIdentificationStrategy;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;
import ca.uhn.fhir.validation.IValidatorModule;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import com.google.common.base.Strings;
import org.hl7.fhir.common.hapi.validation.support.*;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.web.cors.CorsConfiguration;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.*;

import static ca.uhn.fhir.jpa.starter.common.validation.IRepositoryValidationInterceptorFactory.ENABLE_REPOSITORY_VALIDATING_INTERCEPTOR;

@Configuration
//allow users to configure custom packages to scan for additional beans
@ComponentScan(basePackages = { "${hapi.fhir.custom-bean-packages:}" })
@Import(
    ThreadPoolFactoryConfig.class
)
public class StarterJpaConfig {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(StarterJpaConfig.class);

	@Bean
	public IFulltextSearchSvc fullTextSearchSvc() {
		return new FulltextSearchSvcImpl();
	}

	@Bean
	public IStaleSearchDeletingSvc staleSearchDeletingSvc() {
		return new StaleSearchDeletingSvcImpl();
	}

	@Primary
	@Bean
	public CachingValidationSupport validationSupportChain(JpaValidationSupportChain theJpaValidationSupportChain) {
		return ValidationSupportConfigUtil.newCachingValidationSupport(theJpaValidationSupportChain);
	}


	@Autowired
	private ConfigurableEnvironment configurableEnvironment;


	// 2023. 10. 19. Terminology Configuration 전용
	@Autowired
	private TerminologyCodeConfigProperties terminologyCodeConfigProperties;

	@Autowired
	private TerminologyPagingConfigProperties terminologyPagingConfigProperties;

	@Autowired
	private TerminologySearchConfigProperties terminologySearchConfigProperties;

	@Autowired
	private CustomValidationBaseConfigProperties customValidationBaseConfigProperties;

	@Autowired
	private CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties;

	@Autowired
	TransformDataOperationConfigProperties transformDataOperationConfigProperties;

	@Bean
	// 2023. 11. 10. Resource 변환 Provider 구성
	public ResourceTransforOperationProvider resourceTransforOperationProvider(FhirContext fhirContext){
		ResourceTransforOperationProvider resourceTransforOperationProvider = new ResourceTransforOperationProvider(fhirContext);
		return resourceTransforOperationProvider;
	}

	@Bean
	// 2023. 11. 27. 자체구현한 알고리즘의 Transform Engine 을 활용한 Resource 변환 Provider 구성
	public ResourceTransEngineOperationProvider resourceTransEngineOperationProvider(FhirContext fhirContext){
		ResourceTransEngineOperationProvider resourceTransEngineOperationProvider = new ResourceTransEngineOperationProvider(fhirContext);
		return resourceTransEngineOperationProvider;
	}

	@Bean
	// 2023. 12. 15. 자체구현한 알고리즘을 활용한 Transform Engine + Reference Engine 변환 Provider 구성
	public ResourceTransformProvider resourceTransEngineProvider(FhirContext fhirContext){
		ResourceTransformProvider resourceTransformProvider = new ResourceTransformProvider(fhirContext);
		return resourceTransformProvider;
	}

	/**
	 * Customize the default/max page sizes for search results. You can set these however
	 * you want, although very large page sizes will require a lot of RAM.
	 */
	@Bean
	public DatabaseBackedPagingProvider databaseBackedPagingProvider(AppProperties appProperties) {
		DatabaseBackedPagingProvider pagingProvider = new DatabaseBackedPagingProvider();
		pagingProvider.setDefaultPageSize(appProperties.getDefault_page_size());
		pagingProvider.setMaximumPageSize(appProperties.getMax_page_size());
		return pagingProvider;
	}


	@Bean
	public IResourceSupportedSvc resourceSupportedSvc(IDaoRegistry theDaoRegistry) {
		return new DaoRegistryResourceSupportedSvc(theDaoRegistry);
	}

	@Bean(name = "myResourceCountsCache")
	public ResourceCountCache resourceCountsCache(IFhirSystemDao<?, ?> theSystemDao) {
		return ResourceCountCacheUtil.newResourceCountCache(theSystemDao);
	}

	@Primary
	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource myDataSource, ConfigurableListableBeanFactory myConfigurableListableBeanFactory, FhirContext theFhirContext) {
		LocalContainerEntityManagerFactoryBean retVal = HapiEntityManagerFactoryUtil.newEntityManagerFactory(myConfigurableListableBeanFactory, theFhirContext);
		retVal.setPersistenceUnitName("HAPI_PU");

		try {
			retVal.setDataSource(myDataSource);
		} catch (Exception e) {
			throw new ConfigurationException("Could not set the data source due to a configuration issue", e);
		}
		retVal.setJpaProperties(EnvironmentHelper.getHibernateProperties(configurableEnvironment, myConfigurableListableBeanFactory));
		return retVal;
	}

	@Bean
	@Primary
	public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
		JpaTransactionManager retVal = new JpaTransactionManager();
		retVal.setEntityManagerFactory(entityManagerFactory);
		return retVal;
	}

	@Bean
	public IHSearchSortHelper hSearchSortHelper(ISearchParamRegistry mySearchParamRegistry) {
		return new HSearchSortHelperImpl(mySearchParamRegistry);
	}


	@Bean
	@ConditionalOnProperty(prefix = "hapi.fhir", name = ENABLE_REPOSITORY_VALIDATING_INTERCEPTOR, havingValue = "true")
	public RepositoryValidatingInterceptor repositoryValidatingInterceptor(IRepositoryValidationInterceptorFactory factory) {
		return factory.buildUsingStoredStructureDefinitions();
	}

	@Bean
	public LoggingInterceptor loggingInterceptor(AppProperties appProperties) {

		/*
		 * Add some logging for each request
		 */

		LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
		loggingInterceptor.setLoggerName(appProperties.getLogger().getName());
		loggingInterceptor.setMessageFormat(appProperties.getLogger().getFormat());
		loggingInterceptor.setErrorMessageFormat(appProperties.getLogger().getError_format());
		loggingInterceptor.setLogExceptions(appProperties.getLogger().getLog_exceptions());
		return loggingInterceptor;
	}

	@Bean("packageInstaller")
	@Primary
	@Conditional(OnImplementationGuidesPresent.class)
	public IPackageInstallerSvc packageInstaller(AppProperties appProperties, JobDefinition<ReindexJobParameters> reindexJobParametersJobDefinition, JobDefinitionRegistry jobDefinitionRegistry, IPackageInstallerSvc packageInstallerSvc)
	{
		jobDefinitionRegistry.addJobDefinitionIfNotRegistered(reindexJobParametersJobDefinition);

		if (appProperties.getImplementationGuides() != null) {
			Map<String, PackageInstallationSpec> guides = appProperties.getImplementationGuides();
			for (Map.Entry<String, PackageInstallationSpec> guidesEntry : guides.entrySet()) {
				PackageInstallationSpec packageInstallationSpec = guidesEntry.getValue();
				if (appProperties.getInstall_transitive_ig_dependencies()) {

					packageInstallationSpec.addDependencyExclude("hl7.fhir.r2.core")
							.addDependencyExclude("hl7.fhir.r3.core")
							.addDependencyExclude("hl7.fhir.r4.core")
							.addDependencyExclude("hl7.fhir.r5.core");
				}
				packageInstallerSvc.install(packageInstallationSpec);
			}
		}
		return packageInstallerSvc;
	}

	@Bean
	@Conditional(OnCorsPresent.class)
	public CorsInterceptor corsInterceptor(AppProperties appProperties) {
		// Define your CORS configuration. This is an example
		// showing a typical setup. You should customize this
		// to your specific needs
		ourLog.info("CORS is enabled on this server");
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedHeader(HttpHeaders.ORIGIN);
		config.addAllowedHeader(HttpHeaders.ACCEPT);
		config.addAllowedHeader(HttpHeaders.CONTENT_TYPE);
		config.addAllowedHeader(HttpHeaders.AUTHORIZATION);
		config.addAllowedHeader(HttpHeaders.CACHE_CONTROL);
		config.addAllowedHeader("x-fhir-starter");
		config.addAllowedHeader("X-Requested-With");
		config.addAllowedHeader("Prefer");

		List<String> allAllowedCORSOrigins = appProperties.getCors().getAllowed_origin();
		allAllowedCORSOrigins.forEach(config::addAllowedOriginPattern);
		ourLog.info("CORS allows the following origins: " + String.join(", ", allAllowedCORSOrigins));

		config.addExposedHeader("Location");
		config.addExposedHeader("Content-Location");
		config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
		config.setAllowCredentials(appProperties.getCors().getAllow_Credentials());

		// Create the interceptor and register it
		return new CorsInterceptor(config);

	}

	@Bean
	public RestfulServer restfulServer(IFhirSystemDao<?, ?> fhirSystemDao, AppProperties appProperties, DaoRegistry daoRegistry, Optional<MdmProviderLoader> mdmProviderProvider, IJpaSystemProvider jpaSystemProvider, ResourceProviderFactory resourceProviderFactory, JpaStorageSettings jpaStorageSettings, ISearchParamRegistry searchParamRegistry, IValidationSupport theValidationSupport, DatabaseBackedPagingProvider databaseBackedPagingProvider, LoggingInterceptor loggingInterceptor, Optional<TerminologyUploaderProvider> terminologyUploaderProvider, Optional<SubscriptionTriggeringProvider> subscriptionTriggeringProvider, Optional<CorsInterceptor> corsInterceptor, IInterceptorBroadcaster interceptorBroadcaster, Optional<BinaryAccessProvider> binaryAccessProvider, BinaryStorageInterceptor binaryStorageInterceptor, IValidatorModule validatorModule, Optional<GraphQLProvider> graphQLProvider, BulkDataExportProvider bulkDataExportProvider, BulkDataImportProvider bulkDataImportProvider, ValueSetOperationProvider theValueSetOperationProvider, ReindexProvider reindexProvider, PartitionManagementProvider partitionManagementProvider, Optional<RepositoryValidatingInterceptor> repositoryValidatingInterceptor, IPackageInstallerSvc packageInstallerSvc, ThreadSafeResourceDeleterSvc theThreadSafeResourceDeleterSvc, ApplicationContext appContext, Optional<IpsOperationProvider> theIpsOperationProvider
	 , ResourceTransforOperationProvider resourceTransforOperationProvider, ResourceTransEngineOperationProvider resourceTransEngineOperationProvider, ResourceTransformProvider resourceTransformProvider
	) {

		ourLog.info(" >> restful Server Start...!!");

		RestfulServer fhirServer = new RestfulServer(fhirSystemDao.getContext());

		List<String> supportedResourceTypes = appProperties.getSupported_resource_types();

		if (!supportedResourceTypes.isEmpty()) {
			if (!supportedResourceTypes.contains("SearchParameter")) {
				supportedResourceTypes.add("SearchParameter");
			}
			daoRegistry.setSupportedResourceTypes(supportedResourceTypes);
		}

		if (appProperties.getNarrative_enabled()) {
			fhirSystemDao.getContext().setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());
		} else {
			fhirSystemDao.getContext().setNarrativeGenerator(new NullNarrativeGenerator());
		}

		if (appProperties.getMdm_enabled()) mdmProviderProvider.get().loadProvider();

		fhirServer.registerProviders(resourceProviderFactory.createProviders());
		fhirServer.registerProvider(jpaSystemProvider);
		fhirServer.setServerConformanceProvider(calculateConformanceProvider(fhirSystemDao, fhirServer, jpaStorageSettings, searchParamRegistry, theValidationSupport));

		/*
		 * ETag Support
		 */

		if (!appProperties.getEtag_support_enabled()) fhirServer.setETagSupport(ETagSupportEnum.DISABLED);


		/*
		 * Default to JSON and pretty printing
		 */
		fhirServer.setDefaultPrettyPrint(appProperties.getDefault_pretty_print());

		/*
		 * Default encoding
		 */
		fhirServer.setDefaultResponseEncoding(appProperties.getDefault_encoding());

		/*
		 * This configures the server to page search results to and from
		 * the database, instead of only paging them to memory. This may mean
		 * a performance hit when performing searches that return lots of results,
		 * but makes the server much more scalable.
		 */

		fhirServer.setPagingProvider(databaseBackedPagingProvider);

		/*
		 * This interceptor formats the output using nice colourful
		 * HTML output when the request is detected to come from a
		 * browser.
		 */
		fhirServer.registerInterceptor(new ResponseHighlighterInterceptor());

		if (appProperties.getFhirpath_interceptor_enabled()) {
			fhirServer.registerInterceptor(new FhirPathFilterInterceptor());
		}

		fhirServer.registerInterceptor(loggingInterceptor);

		/*
		 * If you are hosting this server at a specific DNS name, the server will try to
		 * figure out the FHIR base URL based on what the web container tells it, but
		 * this doesn't always work. If you are setting links in your search bundles that
		 * just refer to "localhost", you might want to use a server address strategy:
		 */
		String serverAddress = appProperties.getServer_address();
		if (!Strings.isNullOrEmpty(serverAddress)) {
			fhirServer.setServerAddressStrategy(new HardcodedServerAddressStrategy(serverAddress));
		} else if (appProperties.getUse_apache_address_strategy()) {
			boolean useHttps = appProperties.getUse_apache_address_strategy_https();
			fhirServer.setServerAddressStrategy(useHttps ? ApacheProxyAddressStrategy.forHttps() : ApacheProxyAddressStrategy.forHttp());
		} else {
			fhirServer.setServerAddressStrategy(new IncomingRequestAddressStrategy());
		}

		/*
		 * If you are using DSTU3+, you may want to add a terminology uploader, which allows
		 * uploading of external terminologies such as Snomed CT. Note that this uploader
		 * does not have any security attached (any anonymous user may use it by default)
		 * so it is a potential security vulnerability. Consider using an AuthorizationInterceptor
		 * with this feature.
		 */
		if (fhirSystemDao.getContext().getVersion().getVersion().isEqualOrNewerThan(FhirVersionEnum.DSTU3)) { // <-- ENABLED RIGHT NOW
			fhirServer.registerProvider(terminologyUploaderProvider.get());
		}

		// If you want to enable the $trigger-subscription operation to allow
		// manual triggering of a subscription delivery, enable this provider
		if (true) { // <-- ENABLED RIGHT NOW
			fhirServer.registerProvider(subscriptionTriggeringProvider.get());
		}

		corsInterceptor.ifPresent(fhirServer::registerInterceptor);

		if (jpaStorageSettings.getSupportedSubscriptionTypes().size() > 0) {
			// Subscription debug logging
			fhirServer.registerInterceptor(new SubscriptionDebugLogInterceptor());
		}

		if (appProperties.getAllow_cascading_deletes()) {
			CascadingDeleteInterceptor cascadingDeleteInterceptor = new CascadingDeleteInterceptor(fhirSystemDao.getContext(), daoRegistry, interceptorBroadcaster, theThreadSafeResourceDeleterSvc);
			fhirServer.registerInterceptor(cascadingDeleteInterceptor);
		}

		// Binary Storage
		if (appProperties.getBinary_storage_enabled() && binaryAccessProvider.isPresent()) {
			fhirServer.registerProvider(binaryAccessProvider.get());
			fhirServer.registerInterceptor(binaryStorageInterceptor);
		}

		/*
		// Terminology 원격 서버에서 모든 CodeSystem을 가져오기 위한 목적으로 개발.
		// NOT WORKING... 아마 Terminology Server의 Interceptor 가 강제 summary 처리해버리는듯
		List<IBaseResource> remoteAllConformanceResourceList = remoteTermSvc.fetchAllConformanceResources();
		for(IBaseResource eachResource : remoteAllConformanceResourceList){
			ourLog.info("interest ... : " + eachResource.getIdElement().toString());
			if(eachResource.fhirType().equals("CodeSystem")){
				CodeSystem cd = (CodeSystem) eachResource;
				ourLog.info("CodeSystem : " + cd.getName() + " 의 Concept Size : "  + cd.getConcept().size());
			}
		}*/
		// Structure Definition 의 대한 세부적인 설정 구성.
		if(customValidationBaseConfigProperties.isEnableValidation()) {
			FhirContext ctx = fhirServer.getFhirContext();
			ourLog.info(" > Instance 기반의 Validation Service Configuration Start");
			// 1. PrePopulation Validaton 정의
			Map<String, PackageInstallationSpec> ad = appProperties.getImplementationGuides();
			PrePopulatedValidationSupport prePopulatedSupport = new PrePopulatedValidationSupport(ctx);

			// 2. 서버 내 Structure Definion, ValieSet 조회
			// 2.1. StructureDef
			IBaseCoding serachCodeForStructureDef = new Coding();
			serachCodeForStructureDef.setCode("active");
			TokenParam searchToken = new TokenParam(serachCodeForStructureDef);
			SearchParameterMap searchParameterMapForStructureDef = new SearchParameterMap().add(StructureDefinition.SP_STATUS, searchToken);
			searchParameterMapForStructureDef.setSearchTotalMode(SearchTotalModeEnum.ACCURATE);
			searchParameterMapForStructureDef.setCount(1000);

			IFhirResourceDao structureDefinitionResourceProvider = daoRegistry.getResourceDao("StructureDefinition");
			IBundleProvider results = structureDefinitionResourceProvider.search(searchParameterMapForStructureDef);

			// 2.2. ValueSet
			IBaseCoding serachCodeForValueSet = new Coding();
			serachCodeForValueSet.setCode("active");
			TokenParam searchTokenForValueSet = new TokenParam(serachCodeForValueSet);
			SearchParameterMap searchParameterMapForValueSet = new SearchParameterMap().add(ValueSet.SP_STATUS, searchTokenForValueSet);
			searchParameterMapForValueSet.setSearchTotalMode(SearchTotalModeEnum.ACCURATE);
			searchParameterMapForValueSet.setCount(1000);

			IFhirResourceDao resourceProviderForValueSet = daoRegistry.getResourceDao("ValueSet");
			IBundleProvider resultsValueSet = resourceProviderForValueSet.search(searchParameterMapForValueSet);

			// 2.3. CodeSystem
			IBaseCoding serachCodeForCodeSystem = new Coding();
			serachCodeForCodeSystem.setCode("active");
			TokenParam searchTokenForCodeSystem = new TokenParam(serachCodeForCodeSystem);
			SearchParameterMap searchParameterMapForCodeSystem = new SearchParameterMap().add(CodeSystem.SP_STATUS, searchTokenForCodeSystem);
			searchParameterMapForCodeSystem.setSearchTotalMode(SearchTotalModeEnum.ACCURATE);
			searchParameterMapForCodeSystem.setCount(1000);

			IFhirResourceDao resourceProviderForCodeSystem = daoRegistry.getResourceDao("CodeSystem");
			IBundleProvider resultsCodeSystem = resourceProviderForCodeSystem.search(searchParameterMapForCodeSystem);

			// 3. 적용
			for (IBaseResource eachResource : results.getAllResources()) {
				StructureDefinition def = (StructureDefinition) eachResource;
				ourLog.info(" > prePopulated StructureDefinition url : " + def.getUrl());
				prePopulatedSupport.addStructureDefinition(def);
			}

			for (IBaseResource eachResource : resultsValueSet.getAllResources()) {
				ValueSet vs = (ValueSet) eachResource;
				ourLog.info(" > prePopulated ValueSet url : " + vs.getUrl());
				prePopulatedSupport.addValueSet(vs);
			}

			for (IBaseResource eachResource : resultsCodeSystem.getAllResources()) {
				CodeSystem cs = (CodeSystem) eachResource;
				ourLog.info(" > prePopulated CodeSystem url : " + cs.getUrl());
				prePopulatedSupport.addCodeSystem(cs);
			}

			ValidationSupportChain validationSupportChain;
			if (customValidationRemoteConfigProperties.isRemoteTerminologyYn()) {
				// 해당 서버가 별개의 Remote Terminology 서버를 바라보도록 구성
				ourLog.info(" > Remote Terminology Server Enabled.. Server URL : " + customValidationRemoteConfigProperties.getRemoteURL());

				// Reomte 를 활용하는 경우 timeout을 조정한다.
				// ValueSet 등의 fetch 시 너무 느리게 반환 등 이슈
				ca.uhn.fhir.rest.client.api.IRestfulClientFactory factory = ctx.getRestfulClientFactory();
				factory.setSocketTimeout(10000000);
				factory.setConnectionRequestTimeout(10000000);
				factory.setConnectTimeout(10000000);
				ctx.setRestfulClientFactory(factory);

				DevRemoteTerminologyServiceValidationSupport remoteTermSvc = new DevRemoteTerminologyServiceValidationSupport(ctx);
				remoteTermSvc.setBaseUrl(customValidationRemoteConfigProperties.getRemoteURL());
				validationSupportChain = new ValidationSupportChain(
				   // Terminology Server 관련 Validation Chain 설정
					remoteTermSvc
					// FHIR 기본 구조형의 대한 Validation Chain 설정
				   ,new DefaultProfileValidationSupport(ctx)
					// 메모리기반의 서버 Validation 설정
					,new DevInMemoryTerminologyServerValidationSupport(ctx)
					// 메모리에 Validation을 위한 Structure, ValueSet, CodeSystem 등을 배치
					,prePopulatedSupport
					// 코드가 없는 경우의 대한 가용범위 정의
					,new UnknownCodeSystemWarningValidationSupport(ctx)
				);

			} else {
					validationSupportChain = new ValidationSupportChain(
					 new DefaultProfileValidationSupport(ctx)
					,new DevInMemoryTerminologyServerValidationSupport(ctx)
					,prePopulatedSupport
					,new UnknownCodeSystemWarningValidationSupport(ctx)
				);
			}

			validatorModule = new FhirInstanceValidator(validationSupportChain);
			// 2. Validator 등록
			// 2.1. Request Validating Interceptor 생성
			RequestValidatingInterceptor requestInterceptor = new RequestValidatingInterceptor();

			if (validatorModule != null) {
				requestInterceptor.addValidatorModule(validatorModule);
			}

			// 2.2. Interceptor 세부 설정
			//requestInterceptor.setFailOnSeverity(ResultSeverityEnum.ERROR);
			requestInterceptor.setAddResponseHeaderOnSeverity(ResultSeverityEnum.INFORMATION);
			//requestInterceptor.setResponseHeaderValue("${severity} ${line}: ${message}");
			requestInterceptor.setResponseHeaderValueNoIssues("No issues detected");
			fhirServer.registerInterceptor(requestInterceptor);

			ourLog.info(" > Instance Based Customed Validation Service Configuration End");
		}else{
			ourLog.info(" > Instance Based Customed Validation Service No Configurated. Cause User Option... (Check application.yaml service.terminology.validation.enabled)");
		}

		// GraphQL
		if (appProperties.getGraphql_enabled()) {
			if (fhirSystemDao.getContext().getVersion().getVersion().isEqualOrNewerThan(FhirVersionEnum.DSTU3)) {
				fhirServer.registerProvider(graphQLProvider.get());
			}
		}

		if (appProperties.getOpenapi_enabled()) {
			fhirServer.registerInterceptor(new OpenApiInterceptor());
		}

		// Bulk Export
		if (appProperties.getBulk_export_enabled()) {
			fhirServer.registerProvider(bulkDataExportProvider);
		}

		//Bulk Import
		if (appProperties.getBulk_import_enabled()) {
			fhirServer.registerProvider(bulkDataImportProvider);
		}

		// valueSet Operations i.e $expand
		fhirServer.registerProvider(theValueSetOperationProvider);

		//reindex Provider $reindex
		fhirServer.registerProvider(reindexProvider);

		// TODO. CONFIG) 해당 프로젝트에 적절한 Operation의 Custom 이 필요한 시점에 활용한다.
		// 2023. 10. 19. FHIR Implement Guilde 를 위한 IG를 추가한다.
		ImplementGuideOperationProvider testOperationProvider = new ImplementGuideOperationProvider(terminologyCodeConfigProperties);
		fhirServer.registerProvider(testOperationProvider);

		// 2023. 11. 07. Data를 FHIR 로 변환해주는 프로그램을 추가한다.
		if(transformDataOperationConfigProperties.isTransforEnabled()) {
			ourLog.info(" > Transfor Data Opened.. ");
			ourLog.info(" > Now Transfor Version : " + transformDataOperationConfigProperties.getServiceTarget());
			// cmc 기반의 Transfor 적용
			if ("cmc".equals(transformDataOperationConfigProperties.getServiceTarget())) {
				ourLog.info(" ㄴ> Cathoric Medical Centor Data exchange service Operation Provider registed. ");
			}

			// transfor 패턴 분류에 따라 다른 Operation Provider가 활용
			if("engine+ref".equals(transformDataOperationConfigProperties.getTypeOfTransformPattern())){
				fhirServer.registerProvider(resourceTransformProvider);
			}else if("engine".equals(transformDataOperationConfigProperties.getTypeOfTransformPattern())){
				fhirServer.registerProvider(resourceTransEngineOperationProvider);
			}else if("client".equals(transformDataOperationConfigProperties.getTypeOfTransformPattern())){
				fhirServer.registerProvider(resourceTransforOperationProvider);
			}
		}

		// TODO. CONFIG) 사용자 요구에 따라 paging 조정
		fhirServer.setPagingProvider(new TerminologyPagingProvider(terminologyPagingConfigProperties));

		// TODO. CONFIG) 특정 리소스 집합의 대해서 ResourceProviders 를 도입하여 해당 리소스의 동작을 수정하거나 제한
		//fhirServer.setResourceProviders(new SampleResourceProvider());

		// Partitioning
		if (appProperties.getPartitioning() != null) {
			fhirServer.registerInterceptor(new RequestTenantPartitionInterceptor());
			fhirServer.setTenantIdentificationStrategy(new UrlBaseTenantIdentificationStrategy());
			fhirServer.registerProviders(partitionManagementProvider);
		}
		repositoryValidatingInterceptor.ifPresent(fhirServer::registerInterceptor);

		// register custom interceptors
		registerCustomInterceptors(fhirServer, appContext, appProperties.getCustomInterceptorClasses());

		//register the IPS Provider
		if (!theIpsOperationProvider.isEmpty()) {
			fhirServer.registerProvider(theIpsOperationProvider.get());
		}

		return fhirServer;
	}

	/**
	 * check the properties for custom interceptor classes and registers them.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void registerCustomInterceptors(RestfulServer fhirServer, ApplicationContext theAppContext, List<String> customInterceptorClasses) {

		if (customInterceptorClasses == null) {
			return;
		}

		for (String className : customInterceptorClasses) {
			Class clazz;
			try {
				clazz = Class.forName(className);
			} catch (ClassNotFoundException e) {
				throw new ConfigurationException("Interceptor class was not found on classpath: " + className, e);
			}

			// first check if the class a Bean in the app context
			Object interceptor = null;
			try {
				interceptor = theAppContext.getBean(clazz);
			} catch (NoSuchBeanDefinitionException ex) {
				// no op - if it's not a bean we'll try to create it
			}

			// if not a bean, instantiate the interceptor via reflection
			if (interceptor == null) {
				try {
					interceptor = clazz.getConstructor().newInstance();
				} catch (Exception e) {
					throw new ConfigurationException("Unable to instantiate interceptor class : " + className, e);
				}
			}
			fhirServer.registerInterceptor(interceptor);
		}
	}

	public static IServerConformanceProvider<?> calculateConformanceProvider(IFhirSystemDao fhirSystemDao, RestfulServer fhirServer, JpaStorageSettings jpaStorageSettings, ISearchParamRegistry searchParamRegistry, IValidationSupport theValidationSupport) {
		FhirVersionEnum fhirVersion = fhirSystemDao.getContext().getVersion().getVersion();
		if (fhirVersion == FhirVersionEnum.DSTU2) {
			JpaConformanceProviderDstu2 confProvider = new JpaConformanceProviderDstu2(fhirServer, fhirSystemDao, jpaStorageSettings);
			confProvider.setImplementationDescription("HAPI FHIR DSTU2 Server");
			return confProvider;
		} else if (fhirVersion == FhirVersionEnum.DSTU3) {

			JpaConformanceProviderDstu3 confProvider = new JpaConformanceProviderDstu3(fhirServer, fhirSystemDao, jpaStorageSettings, searchParamRegistry);
			confProvider.setImplementationDescription("HAPI FHIR DSTU3 Server");
			return confProvider;
		} else if (fhirVersion == FhirVersionEnum.R4) {

			JpaCapabilityStatementProvider confProvider = new JpaCapabilityStatementProvider(fhirServer, fhirSystemDao, jpaStorageSettings, searchParamRegistry, theValidationSupport);
			confProvider.setImplementationDescription("HAPI FHIR R4 Server");
			return confProvider;
		} else if (fhirVersion == FhirVersionEnum.R4B) {

			JpaCapabilityStatementProvider confProvider = new JpaCapabilityStatementProvider(fhirServer, fhirSystemDao, jpaStorageSettings, searchParamRegistry, theValidationSupport);
			confProvider.setImplementationDescription("HAPI FHIR R4B Server");
			return confProvider;
		} else if (fhirVersion == FhirVersionEnum.R5) {

			JpaCapabilityStatementProvider confProvider = new JpaCapabilityStatementProvider(fhirServer, fhirSystemDao, jpaStorageSettings, searchParamRegistry, theValidationSupport);
			confProvider.setImplementationDescription("HAPI FHIR R5 Server");
			return confProvider;
		} else {
			throw new IllegalStateException();
		}
	}

}


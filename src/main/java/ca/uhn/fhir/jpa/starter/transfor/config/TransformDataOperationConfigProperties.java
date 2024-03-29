package ca.uhn.fhir.jpa.starter.transfor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.LinkedHashSet;
import java.util.Set;

/** 2023. 11. 07.
 *  EMR 기반 등의 데이터의 대한 변환 후 FHIR로 적재하는 작업의 대한 설정을 정의한다.
 */
@Configuration
@Primary
public class TransformDataOperationConfigProperties {

	@Value("${service.transfor.enabled}")
	private boolean transforEnabled;

	@Value("${service.transfor.type}")
	private String typeOfTransformPattern;

	@Value("${service.transfor.target}")
	private String serviceTarget;

	@Value("${service.transfor.logging}")
	private boolean transforLogging;

	@Value("${service.transfor.referencesearch}")
	private boolean searchReferenceinRepoEnabled;

	@Value("${service.transfor.ignorenoencounter}")
	private boolean transforIgnoreHasNoEncounter;

	@Value("${service.transfor.mergeallwithnoinsertmergerule}")
	private boolean transforMergeAllWithNoInsertMergeRule;

	@Value("${service.transfor.cache.useyn}")
	private boolean transformCacheEnabled;

	@Value("${service.transfor.performance.thread.enabled}")
	private boolean threadEnabled;

	@Value("${service.transfor.performance.thread.count}")
	private int threadPoolSize;

	// local(각 요청별 독립적인 cache Poll 활용), global(서버 내 모든 리퀘스트에 같은 cache Poll 활용), external(외부 cache 정보 활용)
	@Value("${service.transfor.cache.type}")
	private String transformCacheType;

	@Value("${service.transfor.debug.performance.trackingtime}")
	private boolean debugPerformanceTrackingTimeEach;

	@Value("${service.transfor.debug.performance.printstacktime}")
	private boolean debugPerformancePrintOperationTimeStack;

	// 해당 순서대로 변환하는 source 데이터의 가장 뒤쪽에 배치된다.
	private Set<String> resourceLowerSortingReferenceSet;

	// 해당 순서대로 변환하는 source 데이터의 가장 앞쪽에 배치된다.
	private Set<String> resourceUpperSortingReferenceSet;

	@Value("${service.transfor.map.location}")
	private String transformMapLocation;

	public TransformDataOperationConfigProperties(){
		// 순서상 먼저 생성되어야하는 리소스 요소
		resourceUpperSortingReferenceSet = new LinkedHashSet<>();
		resourceUpperSortingReferenceSet.add("organization");
		resourceUpperSortingReferenceSet.add("practitioner");
		resourceUpperSortingReferenceSet.add("practitionerrole");
		resourceUpperSortingReferenceSet.add("medication");
		resourceUpperSortingReferenceSet.add("patient");

		// 순서상 가장 나중에 생성되어야 하는 리소스 요소
		resourceLowerSortingReferenceSet = new LinkedHashSet<>();
		resourceUpperSortingReferenceSet.add("encounter");
		resourceUpperSortingReferenceSet.add("condition");
		resourceUpperSortingReferenceSet.add("medicationrequest");
		resourceLowerSortingReferenceSet.add("procedure");
		resourceUpperSortingReferenceSet.add("servicerequest");
		resourceLowerSortingReferenceSet.add("observation");
		resourceLowerSortingReferenceSet.add("diagnosticreport");
	}

	public boolean isTransforEnabled() {
		return transforEnabled;
	}

	public String getTypeOfTransformPattern() { return typeOfTransformPattern; }

	public String getServiceTarget() {
		return serviceTarget;
	}

	public boolean isTransforLogging() { return transforLogging; }

	public boolean isTransforIgnoreHasNoEncounter() {	return transforIgnoreHasNoEncounter; }

	public boolean isTransforMergeAllWithNoInsertMergeRule() {	return transforMergeAllWithNoInsertMergeRule; }

	public Set<String> getResourceLowerSortingReferenceSet() {
		return resourceLowerSortingReferenceSet;
	}

	public Set<String> getResourceUpperSortingReferenceSet() {
		return resourceUpperSortingReferenceSet;
	}

	public boolean isSearchReferenceinRepoEnabled() {
		return searchReferenceinRepoEnabled;
	}

	public boolean isTransformCacheEnabled() {	return transformCacheEnabled; }

	public String getTransformCacheType() {	return transformCacheType; }

	public boolean isThreadEnabled() {
		return threadEnabled;
	}

	public int getThreadPoolSize() {
		return threadPoolSize;
	}

	public boolean isDebugPerformanceTrackingTimeEach() {
		return debugPerformanceTrackingTimeEach;
	}

	public boolean isDebugPerformancePrintOperationTimeStack() {
		return debugPerformancePrintOperationTimeStack;
	}

	public String getTransformMapLocation() {	return transformMapLocation; }
}

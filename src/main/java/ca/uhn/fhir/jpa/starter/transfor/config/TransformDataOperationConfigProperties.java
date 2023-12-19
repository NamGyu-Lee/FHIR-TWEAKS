package ca.uhn.fhir.jpa.starter.transfor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;


/** 2023. 11. 07.
 *  EMR 기반 등의 데이터의 대한 변환 후 FHIR로 적재하는 작업의 대한 설정을 정의한다.
 */
@Configuration
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

	// 해당 순서대로 변환하는 source 데이터의 가장 뒤쪽에 배치된다.
	private Set<String> resourceLowerSortingReferenceSet;

	// 해당 순서대로 변환하는 source 데이터의 가장 앞쪽에 배치된다.
	private Set<String> resourceUpperSortingReferenceSet;

	public TransformDataOperationConfigProperties(){
		// 순서상 먼저 생성되어야하는 리소스 요소
		resourceUpperSortingReferenceSet = new LinkedHashSet<>();
		resourceUpperSortingReferenceSet.add("organization");
		resourceUpperSortingReferenceSet.add("practitioner");
		resourceUpperSortingReferenceSet.add("practitionerrole");
		resourceUpperSortingReferenceSet.add("practitionData");
		resourceUpperSortingReferenceSet.add("medication");
		resourceUpperSortingReferenceSet.add("patient");

		// 순서상 가장 나중에 생성되어야 하는 리소스 요소
		resourceLowerSortingReferenceSet = new LinkedHashSet<>();
		resourceUpperSortingReferenceSet.add("encounter");
		resourceUpperSortingReferenceSet.add("condition");
		resourceUpperSortingReferenceSet.add("medicationrequest");
		resourceLowerSortingReferenceSet.add("procedure");
		resourceLowerSortingReferenceSet.add("imagestudy");
		resourceUpperSortingReferenceSet.add("servicerequest");
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

	public Set<String> getResourceLowerSortingReferenceSet() {
		return resourceLowerSortingReferenceSet;
	}

	public Set<String> getResourceUpperSortingReferenceSet() {
		return resourceUpperSortingReferenceSet;
	}

	public boolean isSearchReferenceinRepoEnabled() {
		return searchReferenceinRepoEnabled;
	}
}

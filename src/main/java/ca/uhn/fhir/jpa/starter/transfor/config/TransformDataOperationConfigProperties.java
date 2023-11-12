package ca.uhn.fhir.jpa.starter.transfor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;


/** 2023. 11. 07.
 *  EMR 기반 등의 데이터의 대한 변환 후 FHIR로 적재하는 작업의 대한 설정을 정의한다.
 */
@Configuration
public class TransformDataOperationConfigProperties {

	@Value("${service.transfor.enabled}")
	private boolean transforEnabled;

	@Value("${service.transfor.target}")
	private String serviceTarget;

	// 해당 순서대로 변환하는 source 데이터의 가장 뒤쪽에 배치된다.
	private Set<String> resourceLowerSortingReferenceSet;

	// 해당 순서대로 변환하는 source 데이터의 가장 앞쪽에 배치된다.
	private Set<String> resourceUpperSortingReferenceSet;

	public TransformDataOperationConfigProperties(){
		// 순서상 먼저 생성되어야하는 리소스 요소
		resourceUpperSortingReferenceSet = new HashSet<>();
		resourceUpperSortingReferenceSet.add("Organization");
		resourceUpperSortingReferenceSet.add("Patient");
		resourceUpperSortingReferenceSet.add("Encounter");
		resourceUpperSortingReferenceSet.add("Condition");

		// 순서상 가장 나중에 생성되어야 하는 리소스 요소
		resourceLowerSortingReferenceSet = new HashSet<>();
		resourceLowerSortingReferenceSet.add("Procedure");
		resourceLowerSortingReferenceSet.add("ImageStudy");
	}

	public boolean isTransforEnabled() {
		return transforEnabled;
	}

	public String getServiceTarget() {
		return serviceTarget;
	}

	public Set<String> getResourceLowerSortingReferenceSet() {
		return resourceLowerSortingReferenceSet;
	}

	public Set<String> getResourceUpperSortingReferenceSet() {
		return resourceUpperSortingReferenceSet;
	}
}

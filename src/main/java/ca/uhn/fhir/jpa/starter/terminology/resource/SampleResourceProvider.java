package ca.uhn.fhir.jpa.starter.terminology.resource;

import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.valueset.BundleEntryTransactionMethodEnum;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.ValidationModeEnum;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Organization;

import java.util.ArrayList;
import java.util.List;


// TODO) CONFIG) 리소스단위의 사용자 요구사항에 맞는 CRUD의 구현 필요 시 활용
/**
 * 2023. 10.
 * 리소스의 대한 CRUD의 대하여 프로젝트의 측면에서 구성을 중간에서 정의할 수 있다.
 * 작업 후 StarterJPAConfig 에서 provider를 등록해야한다.
 * Ex) fhirServer.setResourceProviders(sampleResourceProvider);
 *
 * ※ 단 해당 구성을 활용한다는 의미는 FHIR에서 자체적으로 제공하는 유저 인터페이스를
 *   활용하기위한 구현을 해당 클래스에서 모두 정형화 해주어야 하는 단점이 존재한다.
 *
 *   구현되지않는다면 리소스의 대한 일괄 카운트 체크가 일어나는 유저 인터페이스 기동 시점에
 *   오류가 발생하여 유저에게 정상적인 이미지를 보여주지 않는다.
 */
public class SampleResourceProvider implements IResourceProvider {

	// 대상이 되는 리소스 클래스를 리턴해주어야 한다.
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return Organization.class;
	}

	/**
	 * Spring JPA Data 를 활용하여 중간에서 캐치해서 implement에 활용하는 방식
	 * 전형적인 lucene 엔진 기반 서치 + Hibernate JPA 구성방식.
	 * interceptor 과 흡사하다 Detail. https://hapifhir.io/hapi-fhir/docs/server_plain/resource_providers.html
	 * <p>
	 * 아래와같이 선언하면 결과를 이와같이 받는다
	 * {
	 * "resourceType": "Organization",
	 * "identifier": [ {
	 * "system": "urn:mrns",
	 * "value": "12345"
	 * } ]
	 * }
	 *
	 * @param theId the the id
	 * @return the organization
	 */
	@Read()
	public Organization getOragnizationById(@IdParam IdType theId) {
		Organization retOrganization = new Organization();

		retOrganization.addIdentifier().setSystem("urn:mrns").setValue("12345");

		return retOrganization;
	}

	@Validate
	public MethodOutcome validateOrganization(
		@ResourceParam Organization thePatient, @Validate.Mode ValidationModeEnum theMode, @Validate.Profile String theProfile
	) {
		// 1. validation here

		// 2. 결과전달
		OperationOutcome outcome = new OperationOutcome();
		outcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.WARNING).setDiagnostics("One minor issue detected");
		MethodOutcome retMethodOutcome = new MethodOutcome();
		retMethodOutcome.setOperationOutcome(outcome);
		return retMethodOutcome;
	}

	@History
	public List<Organization> getOrganizationHistory(
		@IdParam IdType theId, @Since InstantType theSince, @At DateRangeParam theAt) {
		List<Organization> retOrganizationList = new ArrayList<>();

		// Pure 기반의 소스구성이기에 데이터를 가져와서 처리하는 과정이 적절히 구현되어야한다.
		// 1. 대상의 히스토리 리소스를 서버에서 조회
		//for(search List... ){
		// 2. 그 리소스 중 각 버전별로 조회기준 정의
		Organization organization = new Organization();
		organization.setId(theId.withVersion("1"));
		if (organization.isDeleted()) {               // 소거된 대상은 조회 안되게 구성하는 예시
			organization = new Organization();
			ResourceMetadataKeyEnum.DELETED_AT.put(organization, InstantType.withCurrentTime());
			ResourceMetadataKeyEnum.ENTRY_TRANSACTION_METHOD.put(organization, BundleEntryTransactionMethodEnum.DELETE);
			retOrganizationList.add(organization);
		} else {
			// 조회기준이 맞으면 충족시키기
			retOrganizationList.add(organization);
		}
		//}

		return retOrganizationList;
	}

	/**
	 * 2023. 10.
	 * Organization을 활용하여 데이터를 Compartment 로 조회를 하는 경우
	 * http://fhir.example.com/Patient/123/Condition 이런 패턴이다
	 *
	 * @param theOranizationId the the oranization id
	 * @return the list
	 */
	@Search(compartmentName = "Condition")
	public List<IBaseResource> getOrganizationChainingConditionSearch(
		@IdParam IdType theOranizationId
	) {
		return new ArrayList<IBaseResource>();
	}
}


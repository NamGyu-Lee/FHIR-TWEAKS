package ca.uhn.fhir.jpa.starter.terminology.resource;

import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Organization;

import java.util.ArrayList;
import java.util.List;

/**
 *  2023. 10.
 *  FHIR에서 데이터를 조회하는 시점에 Search 관련 설정을 수정한다.
 */
public class TerminologyResourceProvider implements IResourceProvider {

	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return CodeSystem.class;
	}

}

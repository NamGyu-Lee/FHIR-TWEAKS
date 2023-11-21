package ca.uhn.fhir.jpa.starter.transfor.service.base;

import lombok.NonNull;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Patient;
import org.json.JSONObject;

/**
 * Transform Engine 을 활용하여 사용자의 데이터를 변환한다.
 */
public interface TransforService {

	/**
	 * Transform plat data to fhir resource base resource.
	 *
	 * @param organizationId the organization id
	 * @param source         the source
	 * @return the base resource
	 */
	public IBaseResource transformPlatDataToFhirResource(@NonNull String organizationId, @NonNull String mapScript, @NonNull JSONObject source);
}

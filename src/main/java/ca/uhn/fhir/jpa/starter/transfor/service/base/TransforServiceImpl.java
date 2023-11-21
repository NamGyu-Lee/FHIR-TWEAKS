package ca.uhn.fhir.jpa.starter.transfor.service.base;

import ca.uhn.fhir.jpa.starter.transfor.base.core.TransformEngine;
import lombok.NonNull;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.json.JSONObject;


public class TransforServiceImpl implements TransforService {

	private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(TransforService.class);

	@Override
	public IBaseResource transformPlatDataToFhirResource(@NonNull String organizationId,
																		  @NonNull String mapScript,
																		  @NonNull JSONObject source) {
		TransformEngine engine = new TransformEngine();
		return engine.transformDataToResource(mapScript, source);
	}

}

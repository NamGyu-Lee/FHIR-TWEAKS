package ca.uhn.fhir.jpa.starter.terminology.util;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.provider.ValueSetOperationProvider;
import ca.uhn.fhir.parser.DataFormatException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FHIRUtils {

	private static final Logger ourLog = LoggerFactory.getLogger(ValueSetOperationProvider.class);

	private final static FhirContext ctx = FhirContext.forR4();

	/** 2023. 02. 22. Resource 를 String 으로 치환한다.
	 * @param theResource
	 * @return
	 */
	public static String resourceToString(IBaseResource theResource) throws DataFormatException {
		return ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(theResource);
	}

	/** 2023. 04. 12. Bundle 을 포함한 Resource 를 String 으로 치환한다.
	 * @param theBundle
	 * @return
	 */
	public static String bundleToString(Bundle theBundle) throws DataFormatException {
		return ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(theBundle);
	}

	/** 2023. 02. 22. String 을 Resource 로 치환한다.
	 * @param strResource
	 * @return
	 */
	public static IBaseResource StringResourceToResource(String strResource) throws DataFormatException{
		return ctx.newJsonParser().parseResource(strResource);
	}

	public static IBaseResource StringBundleToBundle(String strBundle) throws DataFormatException{
		return ctx.newJsonParser().parseResource(strBundle);
	}
}

package ca.uhn.fhir.jpa.starter;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.starter.transfor.util.TransformUtil;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.r4.context.IWorkerContext;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class SampleValidationTest {

	@Test
	public void REMOTE_Validation_TEST(){
		Patient pat = new Patient();
		HumanName name = new HumanName();
		name.setText("아이에");
		List<HumanName> humanNameList = new ArrayList<>();
		humanNameList.add(name);
		pat.setName(humanNameList);

		FhirContext ctx = FhirContext.forR4();
		IGenericClient client = ctx.newRestfulGenericClient("http://fhrdev.cmcnu.or.kr/fhir");

		ctx.newValidator();
		FhirValidator validator = ctx.newValidator();
		ValidationResult result = validator.validateWithResult(pat);
		MethodOutcome come = client.validate().resource(pat).execute();
		OperationOutcome outcome = (OperationOutcome) come.getOperationOutcome();
		System.out.println(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(outcome));
		System.out.println(outcome.getIssue().get(0).toString());
	}
}

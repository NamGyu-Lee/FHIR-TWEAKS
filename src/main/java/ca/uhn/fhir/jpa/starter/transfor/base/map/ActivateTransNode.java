package ca.uhn.fhir.jpa.starter.transfor.base.map;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;


@Getter
@Setter
public class ActivateTransNode {
	RuleNode ruleNode;

	JSONObject source;

	JSONObject target;
}

package ca.uhn.fhir.jpa.starter.transfor.base.map;

import ca.uhn.fhir.jpa.starter.transfor.base.code.ErrorHandleType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceNode {

	private List<ReferenceParamNode> referenceParamNodeList;

	private String targetResource;

	private ErrorHandleType errorHandleType;

}

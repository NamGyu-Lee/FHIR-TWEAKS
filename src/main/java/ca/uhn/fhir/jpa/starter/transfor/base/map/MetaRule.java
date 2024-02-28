package ca.uhn.fhir.jpa.starter.transfor.base.map;

import ca.uhn.fhir.jpa.starter.transfor.base.code.ErrorHandleType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 2023. 12. 18.
 * 해당 Resource의 메타 정보를 관리한다.
 *
 */
@Getter
@Setter
@AllArgsConstructor
public class MetaRule {

	private String mapTargetType;

	private List<ReferenceNode> referenceNodeList;

	private ErrorHandleType errorHandleType;

	private Set<String> cacheDataKey;

	private Set<String> mergeDataKey;

	public MetaRule(){
		referenceNodeList = new ArrayList<>();
		cacheDataKey = new HashSet<>();
	}

}

package ca.uhn.fhir.jpa.starter.transfor.base.map;

import ca.uhn.fhir.jpa.starter.transfor.base.code.ErrorHandleType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.sf.saxon.expr.instruct.Copy;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

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
		referenceNodeList = new CopyOnWriteArrayList<>();
		cacheDataKey = Collections.synchronizedSet(new HashSet<>());
	}

}

package ca.uhn.fhir.jpa.starter.transfor.base.reference.structure;


import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Reference;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
public class ReferenceCache implements Serializable {

	private Map<String, String> keyMap;

	private IBaseResource resource;

	private Reference reference;

	public boolean isCollectedResource(Map<String, String> searchKeyMap){
		boolean retBool = false;
		for (Map.Entry<String, String> entry : searchKeyMap.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();

			if(keyMap.containsKey(key)){
				if (keyMap.get(key).equals(value)) {
					retBool = true;
				}else{
					retBool = false;
				}
			}
		}
		return retBool;
	}
}

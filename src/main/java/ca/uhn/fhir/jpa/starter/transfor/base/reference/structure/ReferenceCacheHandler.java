package ca.uhn.fhir.jpa.starter.transfor.base.reference.structure;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class ReferenceCacheHandler {

	private Map<String, List<ReferenceCache>> cacheMap;

	public ReferenceCacheHandler(){
		cacheMap = new HashMap<>();
	}

	public void putCache(String resourceType, ReferenceCache cache){
		if(cacheMap.get(resourceType) == null){
			List<ReferenceCache> cacheList = new ArrayList<>();
			cacheList.add(cache);
			cacheMap.put(resourceType, cacheList);

		}else if(cacheMap.get(resourceType).size() >= 1){
			List<ReferenceCache> cacheList = cacheMap.get(resourceType);
			cacheList.add(cache);
			cacheMap.put(resourceType, cacheList);
		}
	}

	public ReferenceCache searchCache(String resourceType, Map<String, String> searchMap){
		if(cacheMap.get(resourceType) == null){
			return null;
		}else {
			List<ReferenceCache> cacheList = cacheMap.get(resourceType);
			for (ReferenceCache cache : cacheList) {
				if (cache.isCollectedResource(searchMap)) {
					return cache;
				}
			}
		}
		return null;
	}
}

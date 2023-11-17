package ca.uhn.fhir.jpa.starter.transfor.dto.base;

import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Reference;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** 2023. 11. 13.
 *  데이터 생성과정에서 매칭서비스를 구성
 *
 *  SingleTon Pattern 을 활용하여 구현.
 */
@Getter
@Setter
public class ReferenceDataMatcher {
	private Map<String, ReferenceDataSet> mappingData;

	public ReferenceDataMatcher(){
		mappingData = new HashMap<>();
	}

	public ReferenceDataMatcher(Map<String, ReferenceDataSet> mappingData){
		this.mappingData = mappingData;
	}

	public ReferenceDataSet searchMapperWithKeyType(String key){
		return mappingData.get(key);
	}

	public ReferenceDataSet searchMapperWithMapType(Map<String, String> requestConditionMap){
		for(String keySet : mappingData.keySet()){
			ReferenceDataSet ref = mappingData.get(keySet);
			Map<String, String> identifierMap = ref.getIdentifierSet();
			boolean conditionChecker = false;

			// 메인 레퍼런스는 조회하지 않는다.
			if(keySet.equals("Standard-Ref")){
				continue;
			}

			// 조회할 상태 키셋과 메인 키셋중에 부분적으로 맞으면 리턴한다
			for(String conditionKey : identifierMap.keySet()){
				if(requestConditionMap.get(conditionKey) == null){
					conditionChecker = true;
				}else{
					if(requestConditionMap.get(conditionKey).equals(identifierMap.get(conditionKey))){
						conditionChecker = true;
					}else{
						conditionChecker = false;
						break;
					}
				}
			}
			if(conditionChecker){
				return ref;
			}
		}
		//throw new IllegalAccessError("조합키 구성이 기존에 활용한 적이 없는 코드 값입니다.");
		return null;
	}

	public String searchMapperWithMapTypeRetKeyStr(Map<String, String> requestConditionMap){
		for(String keySet : mappingData.keySet()){
			ReferenceDataSet ref = mappingData.get(keySet);
			Map<String, String> identifierMap = ref.getIdentifierSet();
			boolean conditionChecker = false;

			// 메인 레퍼런스는 조회하지 않는다.
			if(keySet.equals("Standard-Ref")){
				continue;
			}

			// 조회할 상태 키셋과 메인 키셋중에 부분적으로 맞으면 리턴한다.
			for(String conditionKey : identifierMap.keySet()){
				if(requestConditionMap.get(conditionKey) == null){
					conditionChecker = true;
				}else{
					if(requestConditionMap.get(conditionKey).equals(identifierMap.get(conditionKey))){
						conditionChecker = true;
					}else{
						conditionChecker = false;
						break;
					}
				}
			}
			if(conditionChecker){
				return keySet;
			}
		}
		//throw new IllegalAccessError("조합키 구성이 기존에 활용한 적이 없는 코드 값입니다.");
		return null;
	}

	public void inputMappingData(String identifierId, Map<String, String> identifierSet, Map<String, Reference> refList){
		ReferenceDataSet inputDs = ReferenceDataSet.builder().identifierId(identifierId).identifierSet(identifierSet).referenceList(refList).build();

		// 1. 상태확인
		if(searchMapperWithMapType(identifierSet)!=null){
			if(!searchMapperWithMapType(identifierSet).getIdentifierId().equals("Standard-Ref")){
				//throw new IllegalArgumentException("이미 선언된 MappingData 입니다. " + searchMapperWithMapType(identifierSet).getIdentifierId() + " Dataset : " + identifierSet);
			}
		}

		// 2. 생성
		mappingData.put(identifierId, inputDs);
	}

	public String inputMappingData(LinkedHashMap<String, String> identifierSet, Map<String, Reference> refList){

		// Create Key
		String identifierId = "";
		for(String key : identifierSet.keySet()){
			if(identifierId == ""){
				identifierId = identifierSet.get(key);
			}else{
				identifierId = identifierId + "." + identifierSet.get(key);
			}
		}

		this.inputMappingData(identifierId, identifierSet, refList);

		return identifierId;
	}

	public void setReference(String identifierId, String referenceType, Reference reference){
		ReferenceDataSet ds = mappingData.get(identifierId);

		Map<String, Reference> ref = ds.getReferenceList();
		ref.put(referenceType, reference);

		ds.setReferenceList(ref);
		mappingData.put(identifierId, ds);
	}

	public void clearReference(){
		ReferenceDataSet refStandard = mappingData.get("Standard-Ref");
		mappingData.clear();
		mappingData.put("Standard-Ref", refStandard);
	}

	/**
	 * Debug Only.
	 * @param identifierId the identifier id
	 */
	@Deprecated
	public void printAllReference(String identifierId){
		ReferenceDataSet def = mappingData.get(identifierId);
		for(String key : def.getReferenceList().keySet()){
			System.out.println(key + " : " + def.getReferenceList().get(key).getReference());
		}
		System.out.println("------------------------------------------");
	}

	/**	 * Debug Only.
	 * @param identifierId the identifier id
	 */
	@Deprecated
	public void printAllConditionMap(String identifierId){
		System.out.println("---------- Condition Map In : " + identifierId);
		ReferenceDataSet def = mappingData.get(identifierId);
		for(String key : def.getIdentifierSet().keySet()){
			System.out.println(key + " : " + def.getReferenceList().get(key).getReference());
		}
		System.out.println("------------------------------------------");
	}
}
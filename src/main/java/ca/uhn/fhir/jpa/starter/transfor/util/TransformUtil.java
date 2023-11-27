package ca.uhn.fhir.jpa.starter.transfor.util;

import ca.uhn.fhir.jpa.starter.transfor.code.ResourceNameSummaryCode;
import ca.uhn.fhir.jpa.starter.transfor.config.TransformDataOperationConfigProperties;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

/** 2023. 11. 27.
 * Transform 과정에서 Engine  기반, Client 기반 모두 활용 가능한 편의성 함수들의 대하여
 * 정의한다.
 */
public class TransformUtil {

	private TransformDataOperationConfigProperties transformDataOperationConfigProperties;

	public TransformUtil(TransformDataOperationConfigProperties transformDataOperationConfigProperties){
		this.transformDataOperationConfigProperties = transformDataOperationConfigProperties;
	}

	// 2023. 11. 13. 요구로 온 데이터들의 대하여 래퍼런스 구조에 맞게 Sorting 처리한다.
	public Queue<Map.Entry<String, JsonElement>> sortingCreateResourceArgument(JsonObject jsonObject){
		Queue<Map.Entry<String, JsonElement>> upperSortingQueue = new LinkedList<>();
		Queue<Map.Entry<String, JsonElement>> lowerSortingQueue = new LinkedList<>();
		Queue<Map.Entry<String, JsonElement>> nonSortingQueue = new LinkedList<>();
		Queue<Map.Entry<String, JsonElement>> sortedQueue = new LinkedList<>();

		// Upper
		for(String upperString : transformDataOperationConfigProperties.getResourceUpperSortingReferenceSet()){
			for(Map.Entry<String, JsonElement> eachEntry : jsonObject.entrySet()){
				if(eachEntry.getKey().equals(upperString)){
					upperSortingQueue.add(eachEntry);
				}
			}
		}

		// Lower
		for(String lowerString : transformDataOperationConfigProperties.getResourceLowerSortingReferenceSet()){
			for(Map.Entry<String, JsonElement> eachEntry : jsonObject.entrySet()){
				if(eachEntry.getKey().equals(lowerString)){
					lowerSortingQueue.add(eachEntry);
				}
			}
		}

		// Non
		Set<String> nonSortingList = new HashSet<>();
		nonSortingList.addAll(transformDataOperationConfigProperties.getResourceUpperSortingReferenceSet());
		nonSortingList.addAll(transformDataOperationConfigProperties.getResourceLowerSortingReferenceSet());
		for(Map.Entry<String, JsonElement> eachEntry : jsonObject.entrySet()){
			boolean isAlreadySorted = false;
			for(String nonString : nonSortingList){
				if(eachEntry.getKey().equals(nonString)){
					isAlreadySorted = true;
				}
			}
			if(isAlreadySorted != true){
				nonSortingQueue.add(eachEntry);
			}
		}

		// 1.2. merge
		while(upperSortingQueue.size() != 0){
			sortedQueue.add(upperSortingQueue.poll());
		}
		while(nonSortingQueue.size() != 0){
			sortedQueue.add(nonSortingQueue.poll());
		}
		while(lowerSortingQueue.size() != 0){
			sortedQueue.add(lowerSortingQueue.poll());
		}

		return sortedQueue;
	}


	// 2023. 11. 14. Json Object 를 Map 으로 치환한다.
	public static Map<String, String> convertJsonObjectToMap(JsonObject jsonObject) {
		Gson gson = new Gson();

		// Gson이 Return 을 바로 Map<String, String> 을 보내더라도, 숫자형 값들의 대하여는 Double Type으로 반환하여 Map에 넣는 이슈가 있어 추가작업
		//  + 변수가 정수여도 Double 형변환이 일어나(ex.. cretno=1.0) 이를 검증하고 치환하는 로직을 추가
		Map<String, Object> reqData = gson.fromJson(jsonObject, Map.class);
		Map<String, String> data = reqData.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> convertObjectToString(e.getValue())));
		return data;
	}

	// Object를 String으로 치환하여 준다.
	public static String convertObjectToString(Object value) {
		if (value == null) {
			return "";
		}
		if (value instanceof Number) {
			Number number = (Number) value;
			// 정수와 실수를 구분
			if (number.doubleValue() == number.longValue()) {
				return Long.toString(number.longValue());
			} else {
				return Double.toString(number.doubleValue());
			}
		}
		return value.toString();
	}

	/**
	 * Create resource id string.
 	 * 기존 디자인의 확장버전. Map에서 (Key)를 확인하여 가져온다.
	 * @param resourceType  the ResourceType
	 * @param requestMap    the request map
	 * @param identifierSet the identifier set
	 * @return the string
	 * @throws IllegalArgumentException the illegal argument exception
	 */
	public static String createResourceId(String resourceType, LinkedHashSet<String> identifierSet, JSONObject requestMap) throws IllegalArgumentException{
			try {
				String retIdentifier = "";
				retIdentifier = ResourceNameSummaryCode.findSummaryName(resourceType).getSummaryName();

				for (String keyElement : identifierSet) {
					retIdentifier = retIdentifier + "." + requestMap.get(keyElement);
				}

				return retIdentifier;
			}catch(JSONException e){
				throw new IllegalArgumentException("Source에서 값을 찾을 수 없습니다. " + requestMap + "   > " + identifierSet);
			}
	}

	/**
	 * Json 내에 있는 계층구조 를 따라가서 조회해준다.
	 *
	 * @param jsonObject the json object
	 * @param keyPath    the key path
	 * @return the nested value
	 * @throws JSONException the json exception
	 */
	public static Object getNestedValueInJson(JSONObject jsonObject, String keyPath) throws org.json.JSONException {
		String[] keys = keyPath.split("\\.");
		JSONObject tempObj = jsonObject;
		for (int i = 0; i < keys.length - 1; i++) {
			tempObj = tempObj.getJSONObject(keys[i]);
		}
		return tempObj.get(keys[keys.length - 1]);
	}
}

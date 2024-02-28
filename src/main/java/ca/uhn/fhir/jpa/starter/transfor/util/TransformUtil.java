package ca.uhn.fhir.jpa.starter.transfor.util;

import ca.uhn.fhir.jpa.starter.transfor.code.ResourceNameSummaryCode;
import ca.uhn.fhir.jpa.starter.transfor.config.TransformDataOperationConfigProperties;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	/**
	 * 2024. 02. 27.
	 * Merge json object pattern.
	 *
	 * @param keySet          키로 구성할 키 셋.
	 * @param mergeTargetKeySet     병합할 키 셋. 존재하지 않으면 키 외 전체를 boundKeySet으로 본다.
	 * @param jsonElementList 대상(반드시 동일한 Json element 의 연속으로 본다)
	 */
	public static List<JsonObject> mergeJsonObjectPattern(Set<String> keySet, Set<String> mergeTargetKeySet, List<JsonElement> jsonElementList) {
		Map<String, JsonObject> mergedMap = new HashMap<>();

		// Bound Key Set 이 존재하지 않으면 키를 제외한 모든 값이 바인드 키 셋으로 정의한다.
		if (mergeTargetKeySet.size() <= 0) {
			Set<String> allKeyObjects = jsonElementList.get(0).getAsJsonObject().keySet();
			for (String arg : allKeyObjects){
				String argNoSeparateText = arg.replaceAll("_[0-9]", "");
				if (!keySet.contains(argNoSeparateText)) {
					mergeTargetKeySet.add(argNoSeparateText);
				}
			}
		}

		Map<String, Integer> arraySizeMap = new HashMap<>();
		for (JsonElement element : jsonElementList){
			JsonObject jsonObj = element.getAsJsonObject();
			String keyValue = "";
			for (String key : keySet) {
				keyValue = keyValue + "|" + jsonObj.get(key);
			}

			// 인덱싱 탐색
			boolean isAlreadyHas = mergedMap.containsKey(keyValue);
			if (isAlreadyHas){
				// 존재하면 병합
				JsonObject befObject = mergedMap.get(keyValue);
				int arrangeSize = arraySizeMap.get(keyValue);
				if(arrangeSize == 1){
					// 첫번째 중첩 시 첫번째 행의 데이터도 _N 값 추가
					mergeJsonObjects(befObject, mergeJsonObject(mergeTargetKeySet, arrangeSize, befObject));
					// 1행 추가에 따른 _N 이 없는 행 소거
					for(String key : mergeTargetKeySet){
						befObject.remove(key);
					}
				}
				mergeJsonObjects(befObject, mergeJsonObject(mergeTargetKeySet, arrangeSize + 1, jsonObj));
				mergedMap.put(keyValue, befObject);
				arraySizeMap.put(keyValue, arrangeSize + 1);
			} else {
				// 비존재 시 추가
				mergedMap.put(keyValue, jsonObj);
				arraySizeMap.put(keyValue, 1);
			}
		}

		List<JsonObject> valuesList = mergedMap.values().stream().collect(Collectors.toList());

		return valuesList;
	}

	/**
	 * 두개의 JsonArray 의 대하여 Set<String> 을 대상으로 컬럼이 있으면 _segNumber 로 생성해준다.
	 *
	 * @param boundKeySet the bound key set
	 * @param segNumber   the seg number
	 * @param source      the source
	 * @return the json object
	 */
	private static JsonObject mergeJsonObject(Set<String> boundKeySet, int segNumber, JsonObject source) {
		JsonObject retObject = new JsonObject();
		Set<String> sourceSet = source.keySet();
		for (String eachKey : sourceSet) {
			if(boundKeySet.contains(eachKey) && segNumber == 1) {
				System.out.println("segNumber Operation..! : " + eachKey);
				retObject.add(eachKey + "_" + String.valueOf(segNumber), source.get(eachKey));
			}else if(boundKeySet.contains(eachKey) && segNumber >= 2){
				retObject.add(eachKey + "_" + String.valueOf(segNumber), source.get(eachKey));
			}
		}
		return retObject;
	}

	/**
	 * JSON Object 끼리 병합시켜준다.
	 *
	 * @param target the target
	 * @param source the source
	 */
	public static void mergeJsonObjects(JsonObject target, JsonObject source) {
		source.entrySet().forEach(entry -> target.add(entry.getKey(), entry.getValue()));
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
	 *
	 * @param resourceType  the ResourceType
	 * @param requestMap    the request map
	 * @param partOfKeySet the part Of KeySet
	 * @return the string
	 * @throws IllegalArgumentException the illegal argument exception
	 */
	public static String createResourceId(String resourceType, LinkedHashSet<String> partOfKeySet, JSONObject requestMap) throws IllegalArgumentException{
			try {
				String retIdentifier = "";
				retIdentifier = ResourceNameSummaryCode.findSummaryName(resourceType).getSummaryName();

				for (String keyElement : partOfKeySet) {
					if(StringUtils.isBlank(retIdentifier)) {
						retIdentifier = requestMap.getString(keyElement);
					}else {
						retIdentifier = retIdentifier + "." + requestMap.getString(keyElement);
					}
				}

				return retIdentifier;
			}catch(JSONException e){
				throw new IllegalArgumentException("Source에서 값을 찾을 수 없습니다. " + requestMap + "   > " + partOfKeySet);
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

		System.out.println("DEV!!! : " + jsonObject.toString());

		for (int i = 0; i < keys.length - 1; i++) {
			tempObj = tempObj.getJSONObject(keys[i]);
		}
		return tempObj.get(keys[keys.length - 1]);
	}

	/** 2023. 11. 28. Map 을 조회한다.
	 * Get map string.
	 *
	 * @param mapType the map type
	 * @return the string
	 */
	public static String getMap(String mapType){
		LinkedList<String> linkedList = splitByDot(mapType);

		String location = "";
		Iterator<String> iterator = linkedList.iterator();
		while(iterator.hasNext()){
			if(location.length()== 0){
				// 폴더는 항상 소문자.
				location = iterator.next().toLowerCase();
			}else{
				location = location +"/"+ iterator.next();
			}
		}
		location = location + ".txt";

 		ClassPathResource resource = new ClassPathResource(location);
		try {
			InputStream inputStream = new ClassPathResource(location).getInputStream();
			File file = File.createTempFile("maptempfile", ".dat");
			FileUtils.copyInputStreamToFile(inputStream, file);

			Path path = file.toPath();

			List<String> content = Files.readAllLines(path);
			String retString = "";
			for(String eachLine : content) {
				// # 처리된 주석은 그 줄에서 소거한다.
				if(eachLine.contains("#")){
					eachLine = eachLine.substring(0, eachLine.indexOf("#"));
					if(eachLine.length() <= 0 || eachLine.equals("\n")){
						continue;
					}
				}

				retString = retString + eachLine + "\n";
			}

			return retString;
		} catch (IOException e) {
			throw new IllegalArgumentException(mapType + " 이 정의되지 않았거나, 파일 호출 과정에서 오류가 발생하였습니다.");
		}
	}

	/**
	 * Split by dot linked list.
	 *
	 * @param input the input
	 * @return the linked list
	 */
	public static LinkedList<String> splitByDot(String input) {
		LinkedList<String> list = new LinkedList<>();
		String[] parts = input.split("\\.");
		for (String part : parts) {
			list.add(part);
		}
		return list;
	}

	public static String convertDateTimeSourceToTarget(String sourceDate, String sourceDateType, String targetDateType){
		SimpleDateFormat inputFormat = new SimpleDateFormat(sourceDateType);
		try {
			Date date = inputFormat.parse(sourceDate);
			SimpleDateFormat outputFormat = new SimpleDateFormat(targetDateType);
			return outputFormat.format(date);
		}catch(java.text.ParseException e){
			throw new IllegalArgumentException("잘못된 날짜 구조를 가지고 있어 오류가 발생하였습니다.");
		}
	}
}

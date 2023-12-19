package ca.uhn.fhir.jpa.starter.transfor.base.util;

import ca.uhn.fhir.jpa.starter.transfor.base.code.RuleType;
import ca.uhn.fhir.jpa.starter.transfor.base.code.TransactionType;
import ca.uhn.fhir.jpa.starter.transfor.base.core.TransformEngine;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Mapper의 룰의 동작과 수행을 정의한다.
 */
public class RuleUtils {

	public static RuleType classifyRuleType(String line) {
		for (RuleType type : RuleType.values()) {
			if (type.matches(line)) {
				return type;
			}
		}
		return RuleType.CREATE; // 기본적으로 CREATE로 처리
	}

	public static TransactionType classifyTransactionType(String line){
		for (TransactionType type : TransactionType.values()) {
			if (type.matches(line)) {
				return type;
			}
		}
		return TransactionType.COPY; // 기본적으로 COPY로 처리
	}

	public static String getTargetElementName(String ruleText){
		return ruleText.substring(0, ruleText.indexOf("="));
	}

	public static String getSourceReferenceName(String ruleText){
		return ruleText.substring(ruleText.indexOf("=")+1);
	}

	public static String getArrayTypeObjectNameSource(String arg){
		Pattern pattern = Pattern.compile("\\((.*?)\\)\\.(.*)");
		Matcher matcher = pattern.matcher(arg);

		if(matcher.find()){
			return matcher.group(1);
		}else{
			throw new IllegalArgumentException(" > Rule 데이터가 정상적이지 않습니다. Array 구조를 구성할 때 잘못된 값이 들어왔습니다.");
		}
	}

	public static String getArrayTypeObjectNameTarget(String arg){
		Pattern pattern = Pattern.compile("\\((.*?)\\)\\.(.*)");
		Matcher matcher = pattern.matcher(arg);

		if(matcher.find()){
			return matcher.group(2);
		}else{
			throw new IllegalArgumentException(" > Rule 데이터가 정상적이지 않습니다. Array 구조를 구성할 때 잘못된 값이 들어왔습니다.");
		}
	}

	public static void mergeJsonObjects(JSONObject destination, JSONObject source) throws JSONException {
			Iterator it = source.keys();
			while (it.hasNext()) {
				String key = (String)it.next();
				destination.put(key, source.get(key));
			}
	}

}

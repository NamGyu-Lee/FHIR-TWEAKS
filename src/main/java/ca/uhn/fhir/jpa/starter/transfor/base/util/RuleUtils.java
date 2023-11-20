package ca.uhn.fhir.jpa.starter.transfor.base.util;

import ca.uhn.fhir.jpa.starter.transfor.base.code.RuleType;
import ca.uhn.fhir.jpa.starter.transfor.base.code.TransactionType;

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

}

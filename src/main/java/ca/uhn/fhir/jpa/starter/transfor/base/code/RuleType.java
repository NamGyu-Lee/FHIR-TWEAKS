package ca.uhn.fhir.jpa.starter.transfor.base.code;

import java.util.regex.Pattern;

/** 2023. 12.
 *  JSON 변환 맵의 가장 기본적인 룰의 구조의 대하여 정의
 */
public enum RuleType {
	CREATE_ARRAY("\\([^)]*\\)\\..*"),
	TRANS(".*=.*"),
	CREATE("^[^(=]*$"); // 기본 규칙으로 남겨둡니다.

	private final Pattern pattern;

	RuleType(String regex) {
		this.pattern = Pattern.compile(regex);
	}

	public boolean matches(String line) {
		return this.pattern.matcher(line).matches();
	}

	public Pattern getPattern(){
		return pattern;
	}
}
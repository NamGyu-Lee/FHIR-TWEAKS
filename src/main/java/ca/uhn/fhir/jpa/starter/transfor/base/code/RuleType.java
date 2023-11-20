package ca.uhn.fhir.jpa.starter.transfor.base.code;

import java.util.regex.Pattern;

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
}
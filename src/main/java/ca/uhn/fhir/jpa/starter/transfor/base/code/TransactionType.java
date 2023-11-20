package ca.uhn.fhir.jpa.starter.transfor.base.code;

import java.util.regex.Pattern;

/**
 * 룰의 분류를 정의한다.
 */
public enum TransactionType {
	COPY(".*=.*"),
	CREATE("^[^(=]*$");

	private final Pattern pattern;

	TransactionType(String regex) {
		this.pattern = Pattern.compile(regex);
	}

	public boolean matches(String line) {
		return this.pattern.matcher(line).matches();
	}
}

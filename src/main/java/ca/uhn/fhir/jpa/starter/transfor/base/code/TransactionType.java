package ca.uhn.fhir.jpa.starter.transfor.base.code;

import lombok.Getter;

import java.util.regex.Pattern;

/**
 * 세부적인 동작의 대한 룰의 분류를 정의한다.
 */
@Getter
public enum TransactionType {

	TRANSLATION("^\\w+=TRANSLATE\\((?:'?\\w+'?|'.+?')\\s*,\\s*(?:'?\\w+'?|'.+?')\\)$"),
	SPLIT("^\\w+=\\s*SPLIT\\(\\w+\\s*,\\s*\\w+(\\s*,\\s*\\w+)?\\)$"),
	MERGE("^\\w+\\s*=\\s*MERGE\\(.*\\)$"),
	UUID("^\\w+\\s*=\\s*UUID\\(.*\\)$"),
	REFERENCE("(\\w+)=REF\\((\\w+), \\$(\\w+)\\)"),
	DATE("^\\w+=DATE\\(.*\\)$"),
	CREATE_SINGLESTRING("'.+?'"),
	COPY_WITH_DEFAULT("^\\w+=NVL\\(.*\\)$"),
	CASE("^\\w+=CASE\\(.*\\)$"),
	COPY_STRING(".*='([^']*)'"),
	COPY(".*=.*"),
	CREATE("^[^(=]*$")
	;

	private final Pattern pattern;

	TransactionType(String regex) {
		this.pattern = Pattern.compile(regex);
	}

	public boolean matches(String line) {
		return this.pattern.matcher(line).matches();
	}


}

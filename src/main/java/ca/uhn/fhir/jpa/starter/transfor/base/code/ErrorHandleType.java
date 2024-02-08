package ca.uhn.fhir.jpa.starter.transfor.base.code;

/**
 * 2023. 12. 18. Reference 에 데이터를 열람하는 시점에
 * 데이터가 없거나, 오류가 발생한 경우의 대한 대응방안을 정의한다.
 * 무시인 경우 reference 자체가 소거되서 FHIR 최종결과에 반영
 */
public enum ErrorHandleType {
	IGNORE("ignore"),
	WARNING("warning"),
	EXCEPTION("exception")
	;

	private String status;

	ErrorHandleType(String status){
		this.status = status;
	}

	public String getStatus() {
		return status;
	}

	public static ErrorHandleType searchErrorHandleType(String arg) throws IllegalArgumentException{
		for(ErrorHandleType errorHandleType : ErrorHandleType.values()){
			if(errorHandleType.getStatus().equals(arg)){
				return errorHandleType;
			}
		}
		throw new IllegalArgumentException("[ERR] 에러 처리 방식이 정의되지 않은 값을 맵에서 활용하였습니다. 값 : " + arg);
	}
}

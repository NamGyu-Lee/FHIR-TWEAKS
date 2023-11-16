package ca.uhn.fhir.jpa.starter.transfor.service.cmc;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

public enum PractitionerSpecialty {

	_00("00", "일반의"),
	_01("01", "내과"),
	_02("02", "신경과"),
	_03("03", "정신건강의학과"),
	_04("04", "외과"),
	_05("05", "정형외과"),
	_06("06", "신경외과"),
	_07("07", "흉부외과"),
	_08("08", "성형외과"),
	_09("09", "마취통증의학과"),
	_10("10", "산부인과"),
	_11("11", "소아청소년과"),
	_12("12", "안과"),
	_13("13", "이비인후과"),
	_14("14", "피부과"),
	_15("15", "비뇨의학과"),
	_16("16", "영상의학과"),
	_17("17", "방사선종양학과"),
	_18("18", "병리과"),
	_19("19", "진단검사의학과"),
	_20("20", "결핵과"),
	_21("21", "재활의학과"),
	_22("22", "핵의학과"),
	_23("23", "가정의학과"),
	_24("24", "응급의학과"),
	_25("25", "직업환경의학과"),
	_26("26", "예방의학과"),
	_27("27", "기타1(치과)"),
	_28("28", "기타4(한방)"),
	_31("31", "기타2"),
	_40("40", "기타2"),
	_41("41", "보건"),
	_42("42", "기타3"),
	_43("43", "보건기관치과"),
	_44("44", "보건기관한방"),
	_49("49", "치과"),
	_50("50", "구강악안면외과"),
	_51("51", "치과보철과"),
	_52("52", "치과교정과"),
	_53("53", "소아치과"),
	_54("54", "치주과"),
	_55("55", "치과보존과"),
	_56("56", "구강내과"),
	_57("57", "영상치의학과"),
	_58("58", "구강병리과"),
	_59("59", "예방치과"),
	_60("60", "치과소계"),
	_61("61", "통합치의학과"),
	_80("80", "한방내과"),
	_81("81", "한방부인과"),
	_82("82", "한방소아과"),
	_83("83", "한방안·이비인후·피부과"),
	_84("84", "한방신경정신과"),
	_85("85", "침구과"),
	_86("86", "한방재활의학과"),
	_87("87", "사상체질과"),
	_88("88", "한방응급"),
	_89("89", "한방응급"),
	_90("90", "한방소계"),
	;

	private String deptCode;
	private String deptName;

	public static final String CODESYSTEM = "http://www.hl7korea.or.kr/CodeSystem/hira-medical-department";

	PractitionerSpecialty(String deptCode, String deptName){
		this.deptCode = deptCode;
		this.deptName = deptName;
	}

	public CodeableConcept getCodeableConcept() {
		return new CodeableConcept(new Coding(CODESYSTEM, this.deptCode, this.deptName));
	}

	public CodeableConcept getCodeableConcept(String text) {
		return getCodeableConcept().setText(text);
	}
}

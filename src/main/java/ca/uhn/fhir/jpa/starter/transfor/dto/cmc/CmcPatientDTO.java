package ca.uhn.fhir.jpa.starter.transfor.dto.cmc;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 *  2023. 11. 07. CMC 에서 정의하고있는 Patient 데이터의 대해 구성하는 것을 설명한다.
 */
@Getter
@Setter
public class CmcPatientDTO extends CmcCommonDTO implements Serializable {
	public String instcd;

	public String pid;

	public String sex_cd;

	public String hng_nm;

	public String eng_nm;

	public String brth_dd;

	public String home_telno;

	public String prtb_telno;

	public String telno;

	public String zipcd;

	public String addr;

	public String detl_addr;

	public String forger_yn;

	public String nati_cd;

	public String pspt_no;

	public String relign_cd;

	public String proc_corp_cd;

	public String hosp_addr;

	public String hosp_nm;

}

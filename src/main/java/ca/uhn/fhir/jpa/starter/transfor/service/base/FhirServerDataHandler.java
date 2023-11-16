package ca.uhn.fhir.jpa.starter.transfor.service.base;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;

/** 2023. 11. 14.
 *  FHIR의 데이터를 JPA 기반으로 적재 및 조회하는 기능을 정의한다.
 * The type Fhir server data handler.
 */
public class FhirServerDataHandler {

	private DaoRegistry myDaoRegistry;

	public FhirServerDataHandler(DaoRegistry myDaoRegistry){
		this.myDaoRegistry = myDaoRegistry;
	}

	public IFhirResourceDao getResourceProvider(String resourceName){
		return myDaoRegistry.getResourceDao("CodeSystem");
	}




}

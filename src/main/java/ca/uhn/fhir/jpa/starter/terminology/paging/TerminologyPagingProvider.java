package ca.uhn.fhir.jpa.starter.terminology.paging;

import ca.uhn.fhir.jpa.starter.terminology.config.TerminologyPagingConfigProperties;
import ca.uhn.fhir.rest.server.FifoMemoryPagingProvider;

//TODO) CONFIG) PAGING 처리를 프로젝트 조건에 맞게 수정해야함

/** 2023. 10.
 *  서버의 페이징 처리를 지원한다.
 *
 *  DatabasebackendPagingProvider :  searched result is also cached to the database and the client may base the cached search result set.
 *   - DB에서 캐시해놓고 있다가 활용함
 *
 *  FifoMemoryPagingProvider : HAPI FHIR server search is persisted on the server memory and when pages are fetched the server returns the results from the cached memory (unless the cache overflowed and the old result set is no longer available)
     - 서버메모리에 persistance entity 를 활용하는 방식.
       hit rate 단위로 핸들링한다
 */

// public class TerminologyPagingProvider extends DatabaseBackedPagingProvider {
public class TerminologyPagingProvider extends FifoMemoryPagingProvider{

  	private TerminologyPagingConfigProperties terminologyPagingConfigProperties;

	public TerminologyPagingProvider(TerminologyPagingConfigProperties terminologyPagingConfigProperties) {
		super(terminologyPagingConfigProperties.getFifoPagingSize());
		this.terminologyPagingConfigProperties = terminologyPagingConfigProperties;

		this.setDefaultPageSize(this.terminologyPagingConfigProperties.getPagingDefaultSize());
		this.setMaximumPageSize(this.terminologyPagingConfigProperties.getPagingMaxSize());
	}
}

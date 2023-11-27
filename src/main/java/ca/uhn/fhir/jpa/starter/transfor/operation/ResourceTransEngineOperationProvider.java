package ca.uhn.fhir.jpa.starter.transfor.operation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.provider.BaseJpaProvider;
import ca.uhn.fhir.jpa.starter.transfor.base.core.TransformEngine;
import ca.uhn.fhir.jpa.starter.transfor.config.TransformDataOperationConfigProperties;
import ca.uhn.fhir.jpa.starter.transfor.dto.base.ReferenceDataMatcher;
import ca.uhn.fhir.jpa.starter.transfor.util.TransformUtil;
import ca.uhn.fhir.jpa.starter.validation.config.CustomValidationRemoteConfigProperties;
import ca.uhn.fhir.rest.annotation.Operation;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/** 2023 . 11. 27.
 *  FHIR의 데이터 생성의 대하여 TransformEngine 의 기능을 활용하여 서비스를 구성한다.
 *  사용자에게 서비스를 제공하는 Controller 역할을 수행한다.
 */
public class ResourceTransEngineOperationProvider extends BaseJpaProvider {

	private static final Logger ourLog = LoggerFactory.getLogger(ResourceTransEngineOperationProvider.class);

	ReferenceDataMatcher referenceDataMatcher = new ReferenceDataMatcher();

	private TransformEngine transformEngine ;

	private TransformUtil transformUtil;

	private FhirContext fn;

	public ResourceTransEngineOperationProvider(FhirContext fn){
		this.fn = fn;
		transformEngine = new TransformEngine(customValidationRemoteConfigProperties);
		transformUtil = new TransformUtil(transformDataOperationConfigProperties);
	}

	private TransformDataOperationConfigProperties transformDataOperationConfigProperties;
	@Autowired
	void setTransformDataOperationConfigProperties(TransformDataOperationConfigProperties transformDataOperationConfigProperties){
		this.transformDataOperationConfigProperties = transformDataOperationConfigProperties;
	}

	private CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties;
	@Autowired
	void setCustomValidationRemoteConfigProperties(CustomValidationRemoteConfigProperties customValidationRemoteConfigProperties){
		this.customValidationRemoteConfigProperties = customValidationRemoteConfigProperties;
	}

	@Operation(
		name="$tranform-resource-basic",
		idempotent = false,
		manualRequest = true,
		manualResponse = true
	)
	public void transforResourceStandardService(HttpServletRequest theServletRequest, HttpServletResponse theResponse) throws IOException {
		String retMessage = "-";
		ourLog.info(" > Create CMC Standard Data Transfor initalized.. ");

		FhirContext fn = this.getContext();

		try {
			byte[] bytes = IOUtils.toByteArray(theServletRequest.getInputStream());
			String bodyData = new String(bytes);

			JsonParser jsonParser = new JsonParser();
			JsonElement jsonElement = jsonParser.parse(bodyData);
			JsonObject jsonObject = jsonElement.getAsJsonObject();

			Queue<Map.Entry<String, JsonElement>> sortedQueue = transformUtil.sortingCreateResourceArgument(jsonObject);

			// 실질적인 변환 부분
			// 해당 영역부터 개별 대상자로 한정
			Map<String, String> noSearchArg = new HashMap<>();
			noSearchArg.put("Don't Search Main Volumns", "9999999");
			referenceDataMatcher.inputMappingData("Standard-Ref", noSearchArg, new HashMap<>());
			while(sortedQueue.size() != 0){
				Map.Entry<String, JsonElement> entry = sortedQueue.poll();
			}

		}catch(Exception e){
			e.printStackTrace();
			retMessage = e.getMessage();
		}finally{
			theResponse.setContentType("text/plain");
			theResponse.getWriter().write(retMessage);
			theResponse.getWriter().close();
		}
	}

}

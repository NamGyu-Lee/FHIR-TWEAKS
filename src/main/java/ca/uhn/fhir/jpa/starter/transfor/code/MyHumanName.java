package ca.uhn.fhir.jpa.starter.transfor.code;

import ca.uhn.fhir.jpa.starter.transfor.operation.resourceTransforOperationProvider;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/** 2023. 11. 10. FHIR 의 데이터를 Transform 할 때 Source 역할을 목적으로 만든 클래스
 *
 *  StructureMap Utili trasnform 테스트용.
 * The type My human name.
 */
public class MyHumanName extends Base {

	private Map<String, Base> data;

	private IIdType id;

	private static final Logger ourLog = LoggerFactory.getLogger(resourceTransforOperationProvider.class);

	public MyHumanName(Map<String, Base> data) {
		// TODO. JSON OBJ - > MAP
		this.data = data;
	}

	@Override
	public String fhirType() {
		return this.getClass().getName();
	}

	@Override
	protected void listChildren(List<Property> list) {

	}

	@Override
	public String getIdBase() {
		return id.getIdPart();
	}

	@Override
	public void setIdBase(String s) {
		id.setValue(s);
	}

	@Override
	public Base copy() {
		return this;
	}

	@Override
	public Base setProperty(String name, Base value) throws FHIRException {
		// 검증된 구조. Base<-Condition 구현체 참조.
		ourLog.info(" [DEV] MyHumanName Set Property Request 1) name :" + name + " 2) value : " + value);
		this.data.put(name, value);
		return value;
	}

	public Base[] getProperty(int hash, String name, boolean checkValid) throws FHIRException {
		// TODO 배열처리. Map<String, List<Base>)?
		ourLog.info(" [DEV] MyHumanName Get Property Request 1) hash :" + hash + " 2) name : " + name + " 3) checkValid : " + checkValid);
		if (checkValid) {
			if(name.equals("MyHumanName")){
				Base[] base = new Base[3];
				base[0] = this.data.get("given");
				base[1] = this.data.get("family");
				base[2] = this.data.get("text");
				return base;
			}

			Base[] base = new Base[1];
			base[0] = this.data.get(name);
			return base;
		} else {
			return null;
		}
	}

}

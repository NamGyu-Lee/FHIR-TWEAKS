package ca.uhn.fhir.jpa.starter.transfor.base.reference.structure;


import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.util.Map;

/**
 *  2023. 12. Reference Cache 를 Entity 2차 캐시에서 구성하면 어떨지 해서 구성한 Entity.
 *  생각해보니 독립적 Transform 구성이 되지않아 보류함.
 *
 */
@Entity
@Slf4j
/*
@Table(
	name="dataentry",
	uniqueConstraints={
		@UniqueConstraint(
			columnNames={"svcreqkey"}
		)
	}
)*/
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TypeDef(name = "jsonb", typeClass = JsonStringType.class)
public class ReferenceCacheEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@Type(type = "jsonb")
	@Column(name = "resource", columnDefinition = "longtext")
	private Map<String, Object> resource;

}

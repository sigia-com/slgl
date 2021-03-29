package io.slgl.api.repository;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonIgnoreProperties("@link")
public class NodeEntity {

	@JsonProperty("@id")
	private String id;

	@JsonProperty("@type")
	private Object type;

	@JsonProperty("@state_source")
	private String stateSource;

	private String created;
	private String fileSha3;
	private String objectSha3;
	private String stateSha3;
	private String camouflageSha3;

    public void setExtendsType(String extendsType) {
		type = ImmutableMap.of("extends_type", extendsType);
    }
}

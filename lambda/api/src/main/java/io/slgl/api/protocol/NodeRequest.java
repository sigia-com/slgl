package io.slgl.api.protocol;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.type.TypeReference;
import io.slgl.api.camouflage.model.Camouflage;
import io.slgl.api.domain.RequestItemObject;
import io.slgl.api.error.UnrecognizedFieldException;
import io.slgl.api.model.TypeEntity;
import io.slgl.api.service.RequestItemObjectFactory;
import io.slgl.api.utils.json.UncheckedObjectMapper;
import io.slgl.api.validator.ValidNodeId;
import io.slgl.api.validator.ValidStateSource;
import io.slgl.api.validator.ValidationProperty;
import io.slgl.permission.context.EvaluationContext;
import io.slgl.permission.context.EvaluationContextObject;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.slgl.api.utils.json.UncheckedObjectMapper.MAPPER;

@Getter
@Setter
@Accessors(chain = true)
@JsonTypeName("node")
public class NodeRequest implements EvaluationContextObject, ApiRequestItem {

	@JsonProperty("@id")
	@ValidNodeId
	private String id;

	@JsonProperty("@file")
	private byte[] file;

	@JsonProperty("@authorizations")
	private List<String> authorizations;

	@JsonProperty("@camouflage")
	@Valid
	private Camouflage camouflage;

	@JsonProperty("@state_source")
	@ValidStateSource
	private String stateSource;

	@ValidationProperty("@type")
	@JsonIgnore
	@ValidNodeId
	private String type;

	@ValidationProperty("@type")
	@JsonIgnore
	@Valid
	private TypeEntity inlineType;

	@ValidationProperty("")
	@JsonIgnore
	private Map<String, List<@Valid NodeRequest>> inlineLinks = new HashMap<>();

	@JsonIgnore
	private Map<String, Object> data = new HashMap<>();

	@JsonIgnore
	private String rawJson;

	@JsonAnyGetter
	public Map<String, Object> getData() {
		return data;
	}

	@JsonAnySetter
	public NodeRequest setData(String name, Object value) {
		if (name.startsWith("@") || name.startsWith("$")) {
			throw new UnrecognizedFieldException(name);
		}

		if (name.startsWith("#")) {
			inlineLinks.put(name, UncheckedObjectMapper.MAPPER.nestedConvertValue(value, new TypeReference<>() {
			}));

		} else {
			data.put(name, value);
		}

		return this;
	}

	@JsonSetter("@type")
	public NodeRequest setType(Object value) {
		if (value instanceof String) {
			type = (String)value;
			inlineType = null;
		} else {
			type = null;
			inlineType = UncheckedObjectMapper.MAPPER.nestedConvertValue(value, TypeEntity.class);
		}

		return this;
	}

	public String getType() {
		return type;
	}

	public TypeEntity getInlineType() {
	    return inlineType;
	}

	@JsonValue
	public Map<String, Object> toMap() {
		Map<String, Object> result = new LinkedHashMap<>();

		if (id != null) {
			result.put("@id", id);
		}
		if (type != null) {
			result.put("@type", type);
		}
		if (authorizations != null) {
			result.put("@authorizations", authorizations);
		}
		if (camouflage != null) {
			result.put("@camouflage", camouflage);
		}

		result.putAll(inlineLinks);
		result.putAll(data);

		return result;
	}

	public static NodeRequest fromJson(String json) {
		NodeRequest object = MAPPER.readValue(json, NodeRequest.class);
		object.setRawJson(json);

		return object;
	}

	public String getRawJson() {
		return rawJson;
	}

	public NodeRequest regenerateRawJson() {
		this.rawJson = null;
		setRawJson(MAPPER.writeValueAsString(this));
		return this;
	}

	@Override
	public EvaluationContext asEvaluationContext() {
		return EvaluationContext.of(toMap());
	}


	@Override
	public RequestItemObject createWriteObject(RequestItemObjectFactory factory) {
		return factory.writeObjectFor(this);
	}
}

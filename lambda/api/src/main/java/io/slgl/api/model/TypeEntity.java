package io.slgl.api.model;

import com.fasterxml.jackson.annotation.*;
import io.slgl.api.utils.json.UncheckedObjectMapper;
import io.slgl.api.validator.ValidAnchor;
import io.slgl.api.validator.ValidNodeId;
import io.slgl.api.validator.ValidType;
import io.slgl.api.validator.ValidationProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
@JsonIgnoreProperties(value = {"@type"})
@ValidType
public class TypeEntity {

	private String extendsType;

	private List<String> stateProperties;

	@Valid
	private List<AnchorEntity> anchors;

	@JsonProperty("#templates")
	@Valid
	private List<TemplateEntity> templates;

	@JsonProperty("permissions")
	@Valid
	private List<PermissionEntity> permissions;

	@Getter
	@Setter
	public static class AnchorEntity {

		@NotNull
		@ValidAnchor
		private String id;

		@ValidationProperty("type")
		@JsonIgnore
		@ValidNodeId
		private String type;

		@ValidationProperty("type")
		@JsonIgnore
		@Valid
		private TypeEntity inlineType;

		@Min(value = 1)
		private Integer maxSize;

		@JsonSetter("type")
		void setTypeForJson(Object value) {
			if (value instanceof String) {
				type = (String)value;
				inlineType = null;
			} else {
				type = null;
				inlineType = UncheckedObjectMapper.MAPPER.nestedConvertValue(value, TypeEntity.class);
			}
		}

		@JsonGetter("type")
		Object getTypeForJson() {
			return inlineType != null ? inlineType : type;
		}

        public boolean hasType() {
			return type != null || inlineType != null;
        }
	}
}

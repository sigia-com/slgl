package io.slgl.client.node.permission;

import com.fasterxml.jackson.annotation.*;
import io.slgl.client.jsonlogic.CustomJsonLogic;
import io.slgl.client.jsonlogic.JsonLogic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.Arrays.asList;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class Permission {

	@JsonProperty("id")
	private final String id;

	@JsonProperty("allow")
	private final List<Allow> allow;

	@JsonProperty("extends_id")
	private final String extendsId;

	@JsonProperty("require")
	private final Requirements require;

	@JsonProperty("require_logic")
	private final JsonLogic requireLogic;

	@JsonProperty("evaluate_state_access_as_user")
	private final String evaluateStateAccessAsUser;

	@JsonCreator
	public Permission(
			@JsonProperty("id") String id,
			@JsonProperty("allow") List<Allow> allow,
			@JsonProperty("extends_id") String extendsId,
			@JsonProperty("require") Requirements require,
			@JsonProperty("require_logic") JsonLogic requireLogic,
			@JsonProperty("evaluate_state_access_as_user") String evaluateStateAccessAsUser
	) {
		this.id = id;
		this.allow = allow;
		this.extendsId = extendsId;
		this.require = require;
		this.requireLogic = requireLogic;
		this.evaluateStateAccessAsUser = evaluateStateAccessAsUser;
	}

	public String getId() {
		return id;
	}

	public List<Allow> getAllow() {
		return this.allow;
	}

	public String getExtendsId() {
		return extendsId;
	}

	public Requirements getRequire() {
		return require;
	}

	public JsonLogic getRequireLogic() {
		return this.requireLogic;
	}

	public String getEvaluateStateAccessAsUser() {
		return evaluateStateAccessAsUser;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Permission that = (Permission) o;
		return Objects.equals(id, that.id) &&
				Objects.equals(allow, that.allow) &&
				Objects.equals(extendsId, that.extendsId) &&
				Objects.equals(require, that.require) &&
				Objects.equals(requireLogic, that.requireLogic) &&
				Objects.equals(evaluateStateAccessAsUser, that.evaluateStateAccessAsUser);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, allow, extendsId, require, requireLogic, evaluateStateAccessAsUser);
	}

	public static class Builder {

		private String id;
		private List<Allow> allow;

		private String extendsId;

		private Requirements.Builder requireBuilder = Requirements.builder();
		private JsonLogic requireLogic;

		private String evaluateStateAccessAsUser;

		public Builder id(String id) {
			this.id =  id;
			return this;
		}

		public Builder allow(Allow... allows) {
			return allow(asList(allows));
		}

		public Builder allow(List<Allow> allows) {
			if (allow == null) {
				allow = new ArrayList<>();
			}

			allow.addAll(allows);
			return this;
		}

		public Builder extendsId(String extendsId) {
			this.extendsId = extendsId;
			return this;
		}

		public Builder alwaysAllowed() {
			requireBuilder = Requirements.builder();
			requireLogic(null);
			return this;
		}

		public Builder requireAll(Requirement... requirements) {
			require().require(requirements);
			return this;
		}

		public Builder requireAll(Iterable<Requirement> requirements) {
			require().require(requirements);
			return this;
		}

		public Builder require(Requirements requirements) {
			require().require(requirements);
			return this;
		}

		public Builder require(Requirements.Builder requirements) {
			require().require(requirements);
			return this;
		}

		private Requirements.Builder require() {
			return requireBuilder;
		}

		public Builder requireLogic(Object requireLogic) {
			this.requireLogic = requireLogic != null ? new CustomJsonLogic(requireLogic) : null;
			return this;
		}

		public Builder requireLogic(JsonLogic requireLogic) {
			this.requireLogic = requireLogic;
			return this;
		}

		public Builder evaluateStateAccessAsUser(String evaluateStateAccessAsUser) {
			this.evaluateStateAccessAsUser = evaluateStateAccessAsUser;
			return this;
		}

		@JsonValue
		public Permission build() {
			return new Permission(id, allow, extendsId, requireBuilder.build(), requireLogic, evaluateStateAccessAsUser);
		}

	}
}

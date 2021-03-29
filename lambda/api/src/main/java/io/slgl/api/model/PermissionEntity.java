package io.slgl.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.slgl.api.model.PermissionEntity.Requirements.SimpleRequirements;
import io.slgl.api.validator.MustBeCurrentUser;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;

@Getter
@Setter
@Accessors(chain = true)
public class PermissionEntity {

	private String id;

	@NotEmpty
	private List<Allow> allow;

	private String extendsId;

	private Requirements require;

	private Object requireLogic;

	@MustBeCurrentUser
	private String evaluateStateAccessAsUser;

	public PermissionEntity setRequire(Map<String, Requirement> require) {
		return setRequire(new SimpleRequirements(require));
	}

	public PermissionEntity setRequire(Requirements require) {
		this.require = require;
		return this;
	}

	@Setter
	@Getter
	@Accessors(chain = true)
	@EqualsAndHashCode
	public static class Allow {

		private AllowAction action;
		private String anchor;

		public boolean doesAllow(AllowAction action) {
			return doesAllow(action, null);
		}
		public boolean doesAllow(AllowAction action, String anchor) {
			if (this.action == AllowAction.ALL && this.anchor == null) {
				return true;
			}

			return this.action == action && Objects.equals(this.anchor, anchor);
		}

		public boolean doesAllow(Allow allow) {
			return doesAllow(allow.getAction(), allow.getAnchor());
		}
	}

	public enum AllowAction {
		@JsonProperty("all")
		ALL(false),
		@JsonProperty("link_to_anchor")
		LINK_TO_ANCHOR(true),
		@JsonProperty("unlink_from_anchor")
		UNLINK_FROM_ANCHOR(true),
		@JsonProperty("read_state")
		READ_STATE(false);

		@Getter
		private final boolean requireAnchor;

		AllowAction(boolean requireAnchor) {
			this.requireAnchor = requireAnchor;
		}
	}

	public interface Requirements extends Iterable<PathRequirement> {

		Stream<PathRequirement> stream();
		List<Map<String, Requirement>> getRequirements();

		@Override
		default Iterator<PathRequirement> iterator() {
			return stream().iterator();
		}

		default List<Requirement> get(String path) {
			return stream()
					.filter(it -> it.getPath().equals(path))
					.map(PathRequirement::getRequirement)
					.collect(Collectors.toList());
		}

		@JsonCreator
		static Requirements simple(Map<String, Requirement> requirement) {
			return new SimpleRequirements(requirement);
		}

		@JsonCreator
		static Requirements list(List<Map<String, Requirement>> requirements) {
			return new RequirementsList(requirements);
		}

		@Data
		class SimpleRequirements implements Requirements {

			@JsonValue
			private final Map<String, Requirement> requirements;

			@Override
			public Stream<PathRequirement> stream() {
				return PathRequirement.stream(requirements);
			}

			@Override
			public List<Map<String, Requirement>> getRequirements() {
				return singletonList(requirements);
			}
		}

		@Data
		class RequirementsList implements Requirements {
			@JsonValue
			private final List<Map<String, Requirement>> requirements;

			@Override
			public Stream<PathRequirement> stream() {
				return requirements.stream().flatMap(PathRequirement::stream);
			}

			@Override
			public List<Map<String, Requirement>> getRequirements() {
				return requirements;
			}
		}
	}

	@Data
	@Accessors(chain = true)
	@EqualsAndHashCode
	public static class Requirement {

		private String op;

		private String aggregate;
		private String as;

		private Object value;
		private String var;
	}

	@Data
	@EqualsAndHashCode
	public static class PathRequirement {
		private final String path;
		private final Requirement requirement;

		public static PathRequirement from(Map.Entry<String, Requirement> entry) {
			return new PathRequirement(entry.getKey(), entry.getValue());
		}

		private static Stream<PathRequirement> stream(Map<String, Requirement> requirements) {
			return requirements.entrySet().stream().map(PathRequirement::from);
		}
	}
}

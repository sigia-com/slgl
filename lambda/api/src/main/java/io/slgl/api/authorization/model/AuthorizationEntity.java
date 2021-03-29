package io.slgl.api.authorization.model;

import io.slgl.api.model.PermissionEntity;
import io.slgl.api.validator.ValidJsonLogic;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Null;
import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
public class AuthorizationEntity {

	@Valid
	@NotEmpty
	private List<Authorize> authorize;

	@Valid
	private PermissionEntity.Requirements require;

	@ValidJsonLogic
	private Object requireLogic;

	@Null
	private List<Object> authorizationPrincipals;
}

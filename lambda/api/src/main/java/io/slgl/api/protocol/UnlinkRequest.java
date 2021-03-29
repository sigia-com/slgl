package io.slgl.api.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.slgl.api.domain.RequestItemObject;
import io.slgl.api.service.RequestItemObjectFactory;
import io.slgl.api.validator.ValidLinkId;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Setter
@Accessors(chain = true)
@JsonTypeName("unlink")
public class UnlinkRequest implements ApiRequestItem {

    @JsonProperty("id")
    @ValidLinkId
    private String id;

    @JsonProperty("authorizations")
    private List<String> authorizations;

    @Override
    public RequestItemObject createWriteObject(RequestItemObjectFactory factory) {
        return factory.writeObjectFor(this);
    }
}

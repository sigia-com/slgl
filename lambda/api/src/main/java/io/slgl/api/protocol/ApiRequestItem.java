package io.slgl.api.protocol;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.slgl.api.domain.RequestItemObject;
import io.slgl.api.service.RequestItemObjectFactory;


@JsonSubTypes({
        @JsonSubTypes.Type(value = NodeRequest.class),
        @JsonSubTypes.Type(value = LinkRequest.class),
        @JsonSubTypes.Type(value = UnlinkRequest.class),
})
public interface ApiRequestItem {
    RequestItemObject createWriteObject(RequestItemObjectFactory factory);

}

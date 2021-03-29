package io.slgl.api.protocol;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class ReadNodeRequest {

	private String id;
	private ShowState showState;
	private List<String> authorizations;
}

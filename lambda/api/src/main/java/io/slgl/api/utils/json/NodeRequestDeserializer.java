package io.slgl.api.utils.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.slgl.api.protocol.NodeRequest;

import java.io.IOException;

public class NodeRequestDeserializer extends StdDeserializer<NodeRequest> implements ResolvableDeserializer {

	private final JsonDeserializer<NodeRequest> delegate;

	public NodeRequestDeserializer(JsonDeserializer<NodeRequest> delegate) {
		super(NodeRequest.class);
		this.delegate = delegate;

	}

	@Override
	public NodeRequest deserialize(JsonParser parser, DeserializationContext context) throws IOException {
		var mapper = (ObjectMapper) parser.getCodec();
		JsonNode node = parser.readValueAsTree();

		var traverse = node.traverse();
		traverse.nextToken();

		NodeRequest result = delegate.deserialize(traverse, context);
		result.setRawJson(mapper.writeValueAsString(node));
		return result;
	}

	@Override
	public void resolve(DeserializationContext ctxt) throws JsonMappingException {
		((ResolvableDeserializer) delegate).resolve(ctxt);
	}
}

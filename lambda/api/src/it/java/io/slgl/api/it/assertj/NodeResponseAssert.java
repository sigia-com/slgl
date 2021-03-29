package io.slgl.api.it.assertj;

import io.slgl.client.node.NodeResponse;
import org.assertj.core.api.AbstractObjectAssert;

import static io.slgl.api.it.utils.MapUtils.get;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

public class NodeResponseAssert extends AbstractObjectAssert<NodeResponseAssert, NodeResponse> {

	public NodeResponseAssert(NodeResponse actual) {
		super(actual, NodeResponseAssert.class);
	}

	public NodeResponseAssert hasValueAtStatePath(Object value, Object... path) {
		extracting(actual -> get(actual.getState(), path), type(Object.class))
				.isEqualTo(value);
		return this;
	}
}

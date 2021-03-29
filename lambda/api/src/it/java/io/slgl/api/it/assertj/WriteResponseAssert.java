package io.slgl.api.it.assertj;

import io.slgl.client.node.LinkResponse;
import io.slgl.client.node.NodeResponse;
import io.slgl.client.node.WriteResponse;
import io.slgl.client.node.WriteResponseItem;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.ListAssert;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.atIndex;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

public class WriteResponseAssert extends AbstractObjectAssert<WriteResponseAssert, WriteResponse> {

	public WriteResponseAssert(WriteResponse actual) {
		super(actual, WriteResponseAssert.class);
	}

	public WriteResponseAssert hasResponsesSize(int size) {
		assertThatResponseItems().hasSize(size);
		return this;
	}

	public WriteResponseAssert hasLink(String sourceNode, String targetNode, String targetAnchor) {
		assertThatResponseItems()
				.filteredOn(LinkResponse.class::isInstance)
				.as("Has link response with (sourceNode=`%s`, targetNode=`%s`, targetAnchor=`%s`)", sourceNode, targetNode, targetAnchor)
				.anySatisfy(item -> {
					var link = (LinkResponse) item;
					assertThat(link.getSourceNode()).isEqualTo(sourceNode);
					assertThat(link.getTargetNode()).isEqualTo(targetNode);
					assertThat(link.getTargetAnchor()).isEqualTo(targetAnchor);
				});
		return this;
	}

	public WriteResponseAssert hasNodeWithId(String nodeId) {
		assertThatResponseItems()
				.filteredOn(NodeResponse.class::isInstance)
				.as("Has node with (@id=`%s`)", nodeId)
				.anySatisfy(item -> {
					var node = (NodeResponse) item;
					assertThat(node.getId()).isEqualTo(nodeId);
				});
		return this;
	}

	public WriteResponseAssert hasNodeWithId(Pattern nodeId) {
		assertThatResponseItems()
				.filteredOn(NodeResponse.class::isInstance)
				.as("Has node with (@id=/%s/)", nodeId)
				.anySatisfy(item -> {
					var node = (NodeResponse) item;
					assertThat(node.getId()).matches(nodeId);
				});
		return this;
	}

	public WriteResponseAssert hasLinkAtIndex(int index) {
		assertThatResponseItems().satisfies(
				item -> assertThat(item).isInstanceOf(LinkResponse.class),
				atIndex(index)
		);
		return this;
	}

	public WriteResponseAssert hasNodeAtIndex(int index) {
		assertThatResponseItems().satisfies(
				item -> assertThat(item).isInstanceOf(NodeResponse.class),
				atIndex(index)
		);
		return this;
	}

	private ListAssert<WriteResponseItem> assertThatResponseItems() {
		return extracting(WriteResponse::getResponses, list(WriteResponseItem.class));
	}
}

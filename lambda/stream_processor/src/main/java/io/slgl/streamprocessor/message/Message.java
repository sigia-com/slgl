package io.slgl.streamprocessor.message;

import com.google.common.collect.ImmutableList;
import lombok.Getter;

import java.util.List;

@Getter
public class Message {

    private final List<Block> blocks;
    private final List<Document> documents;

    public Message() {
        this(ImmutableList.of(), ImmutableList.of());
    }

    public Message(List<Block> blocks, List<Document> documents) {
        this.blocks = ImmutableList.copyOf(blocks);
        this.documents = ImmutableList.copyOf(documents);
    }

    public Message addBlock(Block block) {
        return new Message(ImmutableList.<Block>builder().addAll(blocks).add(block).build(), documents);
    }

    public Message addDocument(Document document) {
        return new Message(blocks, ImmutableList.<Document>builder().addAll(documents).add(document).build());
    }
}

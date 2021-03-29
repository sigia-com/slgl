package io.slgl.client;

import io.slgl.client.node.WriteRequest;

public interface SlglRequestListener {

    WriteRequest onWriteRequest(WriteRequest request);
}

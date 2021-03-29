package io.slgl.client;

import io.slgl.client.node.WriteResponse;

public interface SlglResponseListener {

    void onWriteResponse(WriteResponse response);
}

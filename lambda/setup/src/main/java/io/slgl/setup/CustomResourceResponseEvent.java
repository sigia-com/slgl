package io.slgl.setup;

import com.amazonaws.services.lambda.runtime.Context;
import lombok.Data;
import lombok.experimental.Accessors;

import static com.google.common.base.MoreObjects.firstNonNull;

@Data
@Accessors(chain = true)
public class CustomResourceResponseEvent {

    public String Status;
    public String Reason;

    public String PhysicalResourceId;
    public String StackId;
    public String RequestId;
    public String LogicalResourceId;

    public DataResponse Data;

    public static CustomResourceResponseEvent buildSuccess(Context context, CustomResourceRequestEvent request, DataResponse data) {
        return build(context, request)
                .setStatus("SUCCESS")
                .setData(data);
    }

    public static CustomResourceResponseEvent buildFailed(Exception e, Context context, CustomResourceRequestEvent request) {
        return build(context, request)
                .setStatus("FAILED")
                .setReason(e.toString());
    }

    private static CustomResourceResponseEvent build(Context context, CustomResourceRequestEvent request) {
        return new CustomResourceResponseEvent()
                .setPhysicalResourceId(firstNonNull(request.getPhysicalResourceId(), context.getLogStreamName()))
                .setStackId(request.getStackId())
                .setRequestId(request.getRequestId())
                .setLogicalResourceId(request.getLogicalResourceId());
    }

    @Data
    @Accessors(chain = true)
    public static class DataResponse {

        public String DefaultAdminApiKey;
    }
}

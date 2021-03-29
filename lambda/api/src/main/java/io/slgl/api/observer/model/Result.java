package io.slgl.api.observer.model;

public interface Result {


    default boolean isSuccess() {
        return this instanceof SuccessResult;
    }

    default boolean isError() {
        return this instanceof ErrorResult;
    }

    String getMessage();

    Result log();

    Result log(String msg);
}

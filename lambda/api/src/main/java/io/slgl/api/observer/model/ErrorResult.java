package io.slgl.api.observer.model;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class ErrorResult implements Result {

    private final Throwable cause;
    private final String message;

    public ErrorResult(Throwable cause) {
        this.cause = cause;
        this.message = cause.getLocalizedMessage();
    }

    public ErrorResult(String message, Throwable cause) {
        this.message = String.format("%s, caused by: %s", message, cause.getLocalizedMessage());
        this.cause = cause;
    }

    @Override
    public Result log() {
        log.info(getMessage(), getCause());
        return this;
    }

    @Override
    public Result log(String msg) {
        log.info(String.format("%s: %s", msg, getMessage()), getCause());
        return this;
    }
}

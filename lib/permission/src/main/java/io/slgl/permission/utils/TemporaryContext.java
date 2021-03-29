package io.slgl.permission.utils;

public interface TemporaryContext extends AutoCloseable {
    @Override
    void close();

}

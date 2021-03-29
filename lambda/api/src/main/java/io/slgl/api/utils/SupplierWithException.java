package io.slgl.api.utils;

@FunctionalInterface
public interface SupplierWithException<T, E extends Exception> {
    T apply() throws E;
}

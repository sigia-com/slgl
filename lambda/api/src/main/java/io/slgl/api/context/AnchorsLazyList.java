package io.slgl.api.context;

import io.slgl.permission.context.FirstAndLastElement;

import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class AnchorsLazyList<T> extends LazyList<T> implements FirstAndLastElement {

    private Supplier<T> firstElementSupplier;
    private T firstElement;

    private Supplier<T> lastElementSupplier;
    private T lastElement;

    public AnchorsLazyList(Supplier<List<? extends T>> listSupplier, Supplier<T> firstElementSupplier, Supplier<T> lastElementSupplier) {
        super(listSupplier);
        this.firstElementSupplier = requireNonNull(firstElementSupplier);
        this.lastElementSupplier = requireNonNull(lastElementSupplier);
    }

    public T getFirstElement() {
        if (firstElementSupplier != null) {
            firstElement = firstElementSupplier.get();
            firstElementSupplier = null;
        }

        return firstElement;
    }

    public T getLastElement() {
        if (lastElementSupplier != null) {
            lastElement = lastElementSupplier.get();
            lastElementSupplier = null;
        }

        return lastElement;
    }
}

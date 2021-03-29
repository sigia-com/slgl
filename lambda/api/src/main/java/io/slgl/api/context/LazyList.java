package io.slgl.api.context;

import java.util.AbstractList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class LazyList<T> extends AbstractList<T> {

    private Supplier<List<? extends T>> supplier;
    private List<? extends T> delegate;

    public LazyList(Supplier<List<? extends T>> supplier) {
        this.supplier = requireNonNull(supplier);
    }

    @Override
    public T get(int index) {
        initialize();
        return delegate.get(index);
    }

    @Override
    public int size() {
        initialize();
        return delegate.size();
    }

    private void initialize() {
        if (supplier != null) {
            delegate = Objects.requireNonNull(supplier.get(), "list supplier should never return null");
            supplier = null;
        }
    }
}

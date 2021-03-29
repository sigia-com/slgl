package io.slgl.api.utils;

import com.amazon.ion.IonValue;
import com.fasterxml.jackson.dataformat.ion.ionvalue.IonValueMapper;

import java.io.IOException;

public class UncheckedIonValueMapper {

    private final IonValueMapper delegate;

    public UncheckedIonValueMapper(IonValueMapper delegate) {
        this.delegate = delegate;
    }

    public <T> T parse(IonValue value, Class<T> clazz) {
        try {
            return delegate.parse(value, clazz);
        } catch (IOException e) {
            throw new IonMapperException(e);
        }
    }

    public <T> IonValue serialize(T o) {
        try {
            return delegate.serialize(o);
        } catch (IOException e) {
            throw new IonMapperException(e);
        }
    }

    public static class IonMapperException extends RuntimeException {
        public IonMapperException(Throwable cause) {
            super(cause);
        }
    }
}

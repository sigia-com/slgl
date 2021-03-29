package io.slgl.api.config;

import com.amazon.ion.system.IonSystemBuilder;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.dataformat.ion.ionvalue.IonValueMapper;
import io.slgl.api.ExecutionContext;
import io.slgl.api.ExecutionContextModule;
import io.slgl.api.type.BuiltinType;
import io.slgl.api.utils.UncheckedIonValueMapper;

public class LedgerModule implements ExecutionContextModule {

    @Override
    public void configure() {
        ExecutionContext.put(UncheckedIonValueMapper.class, uncheckedIonValueMapper());
        ExecutionContext.requireModule(AwsModule.class);

        BuiltinType.loadTypes();
    }

    private UncheckedIonValueMapper uncheckedIonValueMapper() {
        IonValueMapper ionMapper = new IonValueMapper(
                IonSystemBuilder.standard().build(),
                PropertyNamingStrategy.SNAKE_CASE);

        ionMapper.setVisibility(ionMapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        return new UncheckedIonValueMapper(ionMapper);
    }
}

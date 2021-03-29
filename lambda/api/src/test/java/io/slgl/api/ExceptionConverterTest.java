package io.slgl.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.slgl.api.error.ApiException;
import io.slgl.api.error.ValidationException;
import io.slgl.api.protocol.NodeRequest;
import io.slgl.api.utils.ErrorCode;
import io.slgl.api.utils.PathPrefix;
import io.slgl.api.utils.json.UncheckedObjectMapper;
import io.slgl.api.validator.ValidationProperty;
import io.slgl.api.validator.ValidatorService;
import lombok.Data;
import lombok.experimental.Accessors;
import org.assertj.core.api.Assertions;
import org.hibernate.validator.constraints.Length;
import org.junit.jupiter.api.Test;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionConverterTest {

    private final ExceptionConverter exceptionConverter = new ExceptionConverter();

    @Test
    public void shouldConvertValidationException() {
        // given
        ValidationTest testObject = new ValidationTest()
                .setNotNullField(null)
                .setMaxLengthField("too-long-value")
                .setMinValueField(-10);

        ValidationException e = validate(testObject);

        // when
        ApiException apiException = exceptionConverter.convertToApiException(e);

        // then
        assertThat(apiException.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR.getCode());
        assertThat(apiException.getFieldErrors())
                .hasSize(3)
                .anySatisfy(fieldError -> {
                    assertThat(fieldError.getField()).isEqualTo("not_null_field");
                    assertThat(fieldError.getCode()).isEqualTo("not_null");
                })
                .anySatisfy(fieldError -> {
                    assertThat(fieldError.getField()).isEqualTo("max_length_field");
                    assertThat(fieldError.getCode()).isEqualTo("length");
                })
                .anySatisfy(fieldError -> {
                    assertThat(fieldError.getField()).isEqualTo("min_value_field");
                    assertThat(fieldError.getCode()).isEqualTo("min");
                });
    }

    @Test
    public void shouldConvertValidationExceptionWithPathPrefix() {
        // given
        ValidationTest testObject = new ValidationTest()
                .setNotNullField(null);

        ValidationException e = validate(testObject, new PathPrefix("#anchor", 0));

        // when
        ApiException apiException = exceptionConverter.convertToApiException(e);

        // then
        assertThat(apiException.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR.getCode());
        assertThat(apiException.getFieldErrors())
                .hasSize(1)
                .anySatisfy(fieldError -> {
                    assertThat(fieldError.getField()).isEqualTo("#anchor[0].not_null_field");
                    assertThat(fieldError.getCode()).isEqualTo("not_null");
                });
    }

    @Test
    public void shouldConvertValidationExceptionForObjectWithNestedObject() {
        // given
        ValidationTestWithNestedObject testObject = new ValidationTestWithNestedObject()
                .setNestedObject(new ValidationTest().setNotNullField(null));

        ValidationException e = validate(testObject);

        // when
        ApiException apiException = exceptionConverter.convertToApiException(e);

        // then
        assertThat(apiException.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR.getCode());
        assertThat(apiException.getFieldErrors())
                .hasSize(1)
                .anySatisfy(fieldError -> {
                    assertThat(fieldError.getField()).isEqualTo("nested_object.not_null_field");
                    assertThat(fieldError.getCode()).isEqualTo("not_null");
                });
    }

    @Test
    public void shouldConvertValidationExceptionForObjectWithList() {
        // given
        ValidationTestWithList testObject = new ValidationTestWithList()
                .setList(ImmutableList.of(
                        new ValidationTest().setNotNullField("valid-value"),
                        new ValidationTest().setNotNullField(null),
                        new ValidationTest().setNotNullField("valid-value")
                ));

        ValidationException e = validate(testObject);

        // when
        ApiException apiException = exceptionConverter.convertToApiException(e);

        // then
        assertThat(apiException.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR.getCode());
        assertThat(apiException.getFieldErrors())
                .hasSize(1)
                .anySatisfy(fieldError -> {
                    assertThat(fieldError.getField()).isEqualTo("list[1].not_null_field");
                    assertThat(fieldError.getCode()).isEqualTo("not_null");
                });
    }

    @Test
    public void shouldConvertValidationExceptionForObjectWithMap() {
        // given
        ValidationTestWithMap testObject = new ValidationTestWithMap()
                .setMap(ImmutableMap.of(
                        "first", new ValidationTest().setNotNullField("valid-value"),
                        "second", new ValidationTest().setNotNullField(null),
                        "third", new ValidationTest().setNotNullField("valid-value")
                ));

        ValidationException e = validate(testObject);

        // when
        ApiException apiException = exceptionConverter.convertToApiException(e);

        // then
        assertThat(apiException.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR.getCode());
        assertThat(apiException.getFieldErrors())
                .hasSize(1)
                .anySatisfy(fieldError -> {
                    assertThat(fieldError.getField()).isEqualTo("map.second.not_null_field");
                    assertThat(fieldError.getCode()).isEqualTo("not_null");
                });
    }

    @Test
    public void shouldConvertValidationExceptionForObjectWithInlineMapOfLists() {
        // given
        var testObject = new ValidationTestWithInlineMapOfLists()
                .setInlineMapOfLists(ImmutableMap.of(
                        "first", ImmutableList.of(
                                new ValidationTest().setNotNullField(null)
                        )
                ));

        ValidationException e = validate(testObject);

        // when
        ApiException apiException = exceptionConverter.convertToApiException(e);

        // then
        assertThat(apiException.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR.getCode());
        assertThat(apiException.getFieldErrors())
                .hasSize(1)
                .anySatisfy(fieldError -> {
                    assertThat(fieldError.getField()).isEqualTo("first[0].not_null_field");
                    assertThat(fieldError.getCode()).isEqualTo("not_null");
                });
    }

    private ValidationException validate(Object testObject) {
        return validate(testObject, null);
    }

    private ValidationException validate(Object testObject, PathPrefix pathPrefix) {
        ValidatorService validatorService = new ValidatorService();

        try {
            if (pathPrefix != null) {
                validatorService.validate(testObject, pathPrefix);
            } else {
                validatorService.validate(testObject);
            }

            return Assertions.fail("Expected validation error");
        } catch (ValidationException e) {
            return e;
        }
    }

    @Test
    public void shouldConvertNestedParseException() {
        // given
        String json = "{\n" +
                "  \"@id\" : \"http://example.com/node\",\n" +
                "  \"@type\" : {\n" +
                "    \"anchors\" : [ {\n" +
                "      \"id\" : \"#child\",\n" +
                "      \"type\" : {\n" +
                "        \"foo\" : \"bar\"\n" +
                "      }\n" +
                "    } ]\n" +
                "  }\n" +
                "}\n";

        Exception e = parseNodeRequest(json);

        // when
        ApiException apiException = exceptionConverter.convertToApiException(e);

        // then
        assertThat(apiException.getCode()).isEqualTo(ErrorCode.UNABLE_TO_PARSE_UNRECOGNIZED_FIELD.getCode());
        assertThat(apiException.getMessage()).isEqualTo(ErrorCode.UNABLE_TO_PARSE_UNRECOGNIZED_FIELD.getMessage("@type.anchors[0].type.foo"));
    }

    private Exception parseNodeRequest(String json) {
        try {
            UncheckedObjectMapper.MAPPER.readValue(json, NodeRequest.class);

            return Assertions.fail("Expected validation error");
        } catch (Exception e) {
            return e;
        }
    }

    @Data
    @Accessors(chain = true)
    public static class ValidationTest {

        @NotNull
        public String notNullField;

        @Length(max = 10)
        public String maxLengthField;

        @Min(value = 0)
        public int minValueField;
    }

    @Data
    @Accessors(chain = true)
    public static class ValidationTestWithNestedObject {

        @Valid
        private ValidationTest nestedObject;
    }

    @Data
    @Accessors(chain = true)
    public static class ValidationTestWithList {

        @Valid
        private List<ValidationTest> list;
    }

    @Data
    @Accessors(chain = true)
    public static class ValidationTestWithMap {

        @Valid
        private Map<String, ValidationTest> map;
    }

    @Data
    @Accessors(chain = true)
    public static class ValidationTestWithInlineMapOfLists {

        @ValidationProperty("")
        private Map<String, List<@Valid ValidationTest>> inlineMapOfLists;
    }
}

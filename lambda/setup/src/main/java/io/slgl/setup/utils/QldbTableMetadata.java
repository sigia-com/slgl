package io.slgl.setup.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QldbTableMetadata {

    private String name;
    private List<QldbIndexMetadata> indexes;
}

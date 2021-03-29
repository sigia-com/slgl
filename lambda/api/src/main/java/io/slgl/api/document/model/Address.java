package io.slgl.api.document.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@Accessors(chain = true)
public class Address {
    private String line1;
    private String line2;
    private String zip;
    private String city;
    private String state;
    private String country;
}

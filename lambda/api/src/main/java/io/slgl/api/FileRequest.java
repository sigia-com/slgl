package io.slgl.api;

import lombok.Data;

import java.util.List;

@Data
public class FileRequest {
    private String id;
    private String link;

    private Object type;

    private List<String> authorizations;

    private String filename;
    private String fileBase64;
}

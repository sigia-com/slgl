package io.slgl.api.utils;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.HashMap;
import java.util.Map;

public class S3ClientFactory {

    private final Map<Region, S3Client> regionToClient = new HashMap<>();

    public S3Client getS3Client(Region region) {
        S3Client s3Client = regionToClient.get(region);

        if (s3Client == null) {

            s3Client = S3Client.builder()
                    .region(region)
                    .build();

            regionToClient.put(region, s3Client);
        }

        return s3Client;
    }
}

package io.slgl.api.observer.service;

import io.slgl.api.observer.model.ObserverData;
import io.slgl.api.observer.model.Result;

public interface StorageUploader {
    Result upload(ObserverData observerData, String pgpPublicKey);
}

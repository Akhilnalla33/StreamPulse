package com.streampulse.ingestion.exception;

public class MissingTenantException extends RuntimeException {

    public MissingTenantException() {
        super("Request is missing the X-Tenant-Id header; requests must go through api-gateway");
    }
}

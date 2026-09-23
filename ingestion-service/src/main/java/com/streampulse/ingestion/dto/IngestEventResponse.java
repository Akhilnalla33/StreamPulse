package com.streampulse.ingestion.dto;

public record IngestEventResponse(String eventId, String status) {

    public static IngestEventResponse accepted(String eventId) {
        return new IngestEventResponse(eventId, "ACCEPTED");
    }
}

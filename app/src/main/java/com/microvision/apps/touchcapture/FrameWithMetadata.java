package com.microvision.apps.touchcapture;

public class FrameWithMetadata {
    String metadata;
    byte[] frame;

    FrameWithMetadata(byte [] frame, String metadata) {
        this.frame = frame;
        this.metadata = metadata;
    }
}

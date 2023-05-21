package com.krestelev.antalyabus.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserRequest {
    private String stopId;
    private String busId;
    private boolean observing;
    @Builder.Default
    private int interval = 3;
}

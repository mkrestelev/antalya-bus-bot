package com.krestelev.antalyabus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class AntalyaKartResponse {

    @JsonProperty("busList")
    private List<Bus> buses;
    private StopInfo stopInfo;

    @Data
    public static class Bus {
        String plate;
        String displayRouteCode;
        Integer stopDiff;
        Integer timeDiff;
    }

    @Data
    public static class StopInfo {
        String busStopName;
    }

}
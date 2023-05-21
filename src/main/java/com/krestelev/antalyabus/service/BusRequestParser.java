package com.krestelev.antalyabus.service;

import com.krestelev.antalyabus.dto.UserRequest;
import com.krestelev.antalyabus.dto.UserRequest.UserRequestBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class BusRequestParser {

    private static final String TRACKING_KEY = "t";

    public UserRequest parseMessage(String message) {
        String[] split = message.split(StringUtils.SPACE);
        UserRequestBuilder requestBuilder = UserRequest.builder();
        if (split.length >= 1) {
            requestBuilder.stopId(split[0]);
        }
        if (split.length >= 2) {
            requestBuilder.busId(split[1]);
        }
        if (split.length >= 3 && TRACKING_KEY.equals(split[2])) {
            requestBuilder.observing(true);
        }
        if (split.length >= 4) {
            requestBuilder.interval(Integer.parseInt(split[3]));
        }
        return requestBuilder.build();
    }
}

package com.krestelev.antalyabus.service;

import com.krestelev.antalyabus.dto.UserRequest;
import com.krestelev.antalyabus.dto.AntalyaKartResponse;
import com.krestelev.antalyabus.dto.AntalyaKartResponse.Bus;
import com.krestelev.antalyabus.exception.BusException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusService {

    public static final String CLOSEST_BUS_URI = "/bus/closest";
    public static final String DEFAULT_REGION = "026";

    private final RestTemplate restTemplate;

    @Value("${kart.base-uri}")
    private String baseUri;

    public List<Bus> getBuses(UserRequest userRequest) {
        String stopId = userRequest.getStopId();
        AntalyaKartResponse response = performGetBusesRequest(userRequest);
        validateResponse(response, stopId);
        return getFilteredBuses(userRequest, response);
    }

    private static List<Bus> getFilteredBuses(UserRequest userRequest, AntalyaKartResponse response) {
        List<Bus> buses = new ArrayList<>(response.getBuses());
        if (StringUtils.isNotEmpty(userRequest.getBusId())) {
            buses.removeIf(bus -> !bus.getDisplayRouteCode().endsWith(userRequest.getBusId()));
        }
        buses.sort(Comparator.comparing(Bus::getTimeDiff));
        return buses;
    }

    private void validateResponse(AntalyaKartResponse response, String stopId) {
        if (response == null) {
            throw new BusException("Error occurred, try a bit later");
        }
        if (StringUtils.isEmpty(response.getStopInfo().getBusStopName())) {
            throw new BusException(String.format(
                "There is no bus stop with number %s in the system, check entered number", stopId));
        }
        if (CollectionUtils.isEmpty(response.getBuses())) {
            throw new BusException(String.format(
                "Currently there is no buses for bus stop %s, try a bit later", stopId));
        }
    }

    private AntalyaKartResponse performGetBusesRequest(UserRequest userRequest) {
        String uri = baseUri + CLOSEST_BUS_URI + "?region={region}&busStopId={busStopId}";
        return restTemplate.getForObject(uri, AntalyaKartResponse.class, DEFAULT_REGION, userRequest.getStopId());
    }
}
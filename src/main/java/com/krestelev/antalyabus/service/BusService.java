package com.krestelev.antalyabus.service;

import com.krestelev.antalyabus.dto.UserRequest;
import com.krestelev.antalyabus.dto.AntalyaKartResponse;
import com.krestelev.antalyabus.dto.AntalyaKartResponse.Bus;
import com.krestelev.antalyabus.exception.BusException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.Precision;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusService {

    private static final Pair<Double, Double> TUNEKTEPE_BUS_STATION_COORDINATES = Pair.of(36.8308009, 30.5962667);
    private static final String CLOSEST_BUS_URI = "/bus/closest";
    private static final String DEFAULT_REGION = "026";

    private final RestTemplate restTemplate;

    @Value("${kart.base-uri}")
    private String baseUri;

    public List<Bus> getBuses(UserRequest userRequest) {
        String stopId = userRequest.getStopId();
        AntalyaKartResponse response = performGetBusesRequest(userRequest);
        validateResponse(response, stopId);
        List<Bus> filteredBuses = getFilteredBuses(userRequest, response);
        validateResult(stopId, filteredBuses);
        return filteredBuses;
    }

    private static List<Bus> getFilteredBuses(UserRequest userRequest, AntalyaKartResponse response) {
        List<Bus> buses = new ArrayList<>(response.getBuses());
        if (StringUtils.isNotEmpty(userRequest.getBusId())) {
            buses.removeIf(bus -> !bus.getDisplayRouteCode().endsWith(userRequest.getBusId()));
        }
        buses.removeIf(BusService::busHasNotDeparted);
        buses.sort(Comparator.comparing(Bus::getTimeDiff));
        return buses;
    }

    public static boolean busHasNotDeparted(Bus bus) {
        double epsilon = 0.002d;
        return Precision.equals(bus.getLat(), TUNEKTEPE_BUS_STATION_COORDINATES.getLeft(), epsilon)
            && Precision.equals(bus.getLng(), TUNEKTEPE_BUS_STATION_COORDINATES.getRight(), epsilon);
    }

    private void validateResponse(AntalyaKartResponse response, String stopId) {
        if (response == null) {
            throw new BusException("Error occurred, try a bit later");
        }
        if (StringUtils.isEmpty(response.getStopInfo().getBusStopName())) {
            throw new BusException(String.format(
                "There is no bus stop with number %s in the system, check entered number", stopId));
        }
    }

    private static void validateResult(String stopId, List<Bus> buses) {
        if (CollectionUtils.isEmpty(buses)) {
            throw new BusException(String.format(
                "Currently there is no buses for bus stop %s, try a bit later", stopId));
        }
    }

    private AntalyaKartResponse performGetBusesRequest(UserRequest userRequest) {
        String uri = baseUri + CLOSEST_BUS_URI + "?region={region}&busStopId={busStopId}";
        return restTemplate.getForObject(uri, AntalyaKartResponse.class, DEFAULT_REGION, userRequest.getStopId());
    }
}
package com.krestelev.antalyabus.service;

import com.krestelev.antalyabus.dto.AntalyaKartResponse.Bus;
import com.krestelev.antalyabus.dto.UserRequest;
import com.krestelev.antalyabus.exception.BusException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
public class Bot extends TelegramLongPollingBot {

    private static final int MILLIS_IN_SECOND = 60_000;

    private final BusService busService;
    private final BusRequestParser busRequestParser;

    @Value("${bot.username}")
    String botUsername;

    public Bot(@Value("${bot.token}") String botToken, BusService busService, BusRequestParser busRequestParser) {
        super(botToken);
        this.busService = busService;
        this.busRequestParser = busRequestParser;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String userInput = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            String userName = update.getMessage().getChat().getFirstName();

            switch (userInput) {
                case "/start" -> handleStartCommand(chatId, userName);
                case "/help" -> handleHelpCommand(chatId);
                default -> handleGetUpcomingBusesRequest(userInput, chatId);
            }
        }
    }

    private void handleGetUpcomingBusesRequest(String userInput, long chatId) {
        UserRequest request = busRequestParser.parseMessage(userInput);
        try {
            List<Bus> buses = busService.getBuses(request);
            if (request.isObserving()) {
                Bus observedBus = buses.get(0);
                if (observedBus.getTimeDiff() <= request.getInterval()) {
                    sendMessage(chatId, getTrackIsFinishedMessage(observedBus));
                    return;
                }
                Thread thread = new Thread(() -> observeBus(request, observedBus, chatId));
                thread.start();
                return;
            }
            sendMessage(chatId, convertUpcomingBusesMessage(buses));
        } catch (BusException e) {
            sendMessage(chatId, e.getMessage());
        }
    }

    @SneakyThrows
    private void observeBus(UserRequest userRequest, Bus observedBus, long chatId) {
        String plate = observedBus.getPlate();
        Optional<Bus> busOptional = busService.getBuses(userRequest).stream()
            .filter(bus -> bus.getPlate().equals(plate))
            .findAny();
        if (busOptional.isPresent() && busOptional.get().getTimeDiff() > userRequest.getInterval()) {
            sendMessage(chatId, convertUpcomingBusMessage(busOptional.get()));
            Thread.sleep((long) userRequest.getInterval() * MILLIS_IN_SECOND);
            observeBus(userRequest, observedBus, chatId);
        } else {
            sendMessage(chatId, getTrackIsFinishedMessage(observedBus));
        }
    }

    private void handleStartCommand(Long chatId, String name) {
        String answer = String.format("""
            Hi, %s!
            I will help you to find and track buses.
            Use /help to know how to do this.
            """, name);
        sendMessage(chatId, answer);
    }

    private void handleHelpCommand(Long chatId) {
        String answer = """
            1. To get buses, enter bus stop number:
               10010
            2. To get a particular bus, add it's number:
               10010 18 (here 18 - bus VS18)
            3. To track this bus, add "t":
               10010 18 t (default tracking interval - 3 min)
            4. To track with custom interval, add it's value:
               10010 18 t 5
            """;
        sendMessage(chatId, answer);
    }

    private void sendMessage(Long chatId, String textToSend) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Error occurred while sending message, details: {}", e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    private String convertUpcomingBusesMessage(List<Bus> buses) {
        return buses.stream()
            .map(bus -> bus.getDisplayRouteCode() + " - " + bus.getTimeDiff() + " min")
            .collect(Collectors.joining("\n"));
    }

    private String convertUpcomingBusMessage(Bus bus) {
        return "Bus " + bus.getDisplayRouteCode() + " will come in " + bus.getTimeDiff() + " min";
    }

    private String getTrackIsFinishedMessage(Bus bus) {
        return "Bus " + bus.getDisplayRouteCode() + " is about to arrive!";
    }

}

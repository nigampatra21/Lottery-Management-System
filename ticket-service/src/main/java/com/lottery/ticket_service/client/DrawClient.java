package com.lottery.ticket_service.client;

import com.lottery.ticket_service.exception.BusinessRuleException;
import com.lottery.ticket_service.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class DrawClient {

    private final RestClient restClient;

    public DrawClient(@Qualifier("drawRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /** Throws if the lottery already has a draw. A 404 from draw-service means none yet. */
    public void requireNoDraw(Long lotteryId, String authHeader, String message) {
        try {
            restClient.get()
                    .uri("/api/draws/lottery/{id}", lotteryId)
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            return;                                   // no draw: allowed
        } catch (RestClientException e) {
            // Fail closed: if we can't be sure, don't sell
            throw new ServiceUnavailableException(
                    "Could not verify the draw status. Please try again later.");
        }
        throw new BusinessRuleException(message);
    }
}
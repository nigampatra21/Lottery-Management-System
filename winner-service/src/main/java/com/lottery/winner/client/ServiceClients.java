package com.lottery.winner.client;

import com.lottery.winner.dto.AdminTicketPage;
import com.lottery.winner.dto.DrawView;
import com.lottery.winner.dto.PrizeTierView;
import com.lottery.winner.exception.BusinessRuleException;
import com.lottery.winner.exception.ResourceNotFoundException;
import com.lottery.winner.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.function.Supplier;

@Component
public class ServiceClients {

    private final RestClient lottery;
    private final RestClient draw;
    private final RestClient ticket;

    public ServiceClients(@Qualifier("lotteryRestClient") RestClient lottery,
                          @Qualifier("drawRestClient") RestClient draw,
                          @Qualifier("ticketRestClient") RestClient ticket) {
        this.lottery = lottery;
        this.draw = draw;
        this.ticket = ticket;
    }

    public DrawView getDraw(String drawCode, String auth) {
        return call(() -> draw.get().uri("/api/draws/{id}", drawCode)
                .header(HttpHeaders.AUTHORIZATION, auth)
                .retrieve().body(DrawView.class), "Draw not found");
    }

    public List<PrizeTierView> getPrizeTiers(Long lotteryId, String auth) {
        return call(() -> lottery.get().uri("/api/lotteries/{id}/prize-tiers", lotteryId)
                .header(HttpHeaders.AUTHORIZATION, auth)
                .retrieve()
                .body(new ParameterizedTypeReference<List<PrizeTierView>>() {}),
                "Lottery not found");
    }

    public AdminTicketPage getTickets(Long lotteryId, Long afterId, String auth) {
        return call(() -> ticket.get()
                .uri("/api/admin/tickets/lottery/{id}?afterId={a}&limit=500", lotteryId, afterId)
                .header(HttpHeaders.AUTHORIZATION, auth)
                .retrieve().body(AdminTicketPage.class), "Lottery not found");
    }

    private <T> T call(Supplier<T> request, String notFoundMessage) {
        try {
            T result = request.get();
            if (result == null) {
                throw new ServiceUnavailableException("A dependent service returned no data");
            }
            return result;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException(notFoundMessage);
        } catch (HttpClientErrorException e) {
            throw new BusinessRuleException(
                    "A dependent service rejected the request (" + e.getStatusCode().value() + ")");
        } catch (RestClientException e) {
            throw new ServiceUnavailableException(
                    "A dependent service is currently unavailable. Please try again later.");
        }
    }
}
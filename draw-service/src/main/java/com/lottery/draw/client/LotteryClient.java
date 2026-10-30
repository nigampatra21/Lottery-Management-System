package com.lottery.draw.client;

import com.lottery.draw.dto.LotteryPurchaseInfo;
import com.lottery.draw.exception.BusinessRuleException;
import com.lottery.draw.exception.ResourceNotFoundException;
import com.lottery.draw.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class LotteryClient {

    private final RestClient restClient;

    public LotteryClient(@Qualifier("lotteryRestClient")  RestClient lotteryRestClient) {
        this.restClient = lotteryRestClient;
    }

    public LotteryPurchaseInfo getPurchaseInfo(Long lotteryId, String authHeader) {
        try {
            LotteryPurchaseInfo info = restClient.get()
                    .uri("/api/lotteries/{id}/purchase-info", lotteryId)
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .retrieve()
                    .body(LotteryPurchaseInfo.class);
            if (info == null) {
                throw new ServiceUnavailableException("Lottery service returned no data");
            }
            return info;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Lottery not found");
        } catch (HttpClientErrorException.BadRequest e) {
            throw new BusinessRuleException("Lottery rules are not configured for this lottery");
        } catch (RestClientException e) {
            throw new ServiceUnavailableException(
                    "Lottery service is currently unavailable. Please try again later.");
        }
    }
}
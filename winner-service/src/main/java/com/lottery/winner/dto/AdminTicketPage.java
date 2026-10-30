package com.lottery.winner.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AdminTicketPage(List<AdminTicketView> tickets, Long nextAfterId) {}
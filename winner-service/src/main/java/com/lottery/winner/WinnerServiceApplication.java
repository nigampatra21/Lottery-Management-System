package com.lottery.winner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WinnerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(WinnerServiceApplication.class, args);
	}

}

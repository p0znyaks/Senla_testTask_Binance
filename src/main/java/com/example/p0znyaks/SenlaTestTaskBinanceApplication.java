package com.example.p0znyaks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SenlaTestTaskBinanceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SenlaTestTaskBinanceApplication.class, args);
	}

}

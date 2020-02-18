package com.mempoolrecorder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@RefreshScope
@EnableFeignClients
@EnableCircuitBreaker
public class MempoolRecorderApplication {

	public static void main(String[] args) {
		SpringApplication.run(MempoolRecorderApplication.class, args);
	}

}

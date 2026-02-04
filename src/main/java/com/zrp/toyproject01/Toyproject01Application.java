package com.zrp.toyproject01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class Toyproject01Application {

	public static void main(String[] args) {
		SpringApplication.run(Toyproject01Application.class, args);
	}

}

package com.app.WsCancelations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WsCancelationsApplication {

	public static void main(String[] args) {
		SpringApplication.run(WsCancelationsApplication.class, args);
	}

}

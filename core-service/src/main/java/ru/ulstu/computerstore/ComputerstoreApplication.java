package ru.ulstu.computerstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ComputerstoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(ComputerstoreApplication.class, args);
	}

}

package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@EnableScheduling
public class DemoApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
		System.setProperty("MAIL_HOST", dotenv.get("MAIL_HOST", "smtp.gmail.com"));
		System.setProperty("MAIL_PORT", dotenv.get("MAIL_PORT", "587"));
		System.setProperty("MAIL_USERNAME", dotenv.get("MAIL_USERNAME", "ramakingthedev@gmail.com"));
		System.setProperty("MAIL_PASSWORD", dotenv.get("MAIL_PASSWORD", "mmrilcpdlwxzaclo"));
		System.setProperty("MAIL_FROM", dotenv.get("MAIL_FROM", "ramakingthedev@gmail.com"));
		SpringApplication.run(DemoApplication.class, args);
	}

}

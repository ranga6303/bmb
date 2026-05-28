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
		setPropertyFromDotenvIfPresent(dotenv, "MAIL_FROM");
		setPropertyFromDotenvIfPresent(dotenv, "RESEND_API_KEY");
		SpringApplication.run(DemoApplication.class, args);
	}

	private static void setPropertyFromDotenvIfPresent(Dotenv dotenv, String key) {
		String value = dotenv.get(key);
		if (value != null && !value.isBlank() && System.getenv(key) == null) {
			System.setProperty(key, value);
		}
	}

}

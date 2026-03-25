package com.systemforge.backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SystemForge — Enterprise Backend System Design Builder.
 *
 * <p>Entry point for the modular monolith. Each domain module (auth, user, system, etc.)
 * is a self-contained package, ready for microservice extraction in future phases.
 */
@SpringBootApplication(exclude = {
		com.openai.springboot.OpenAIClientAutoConfiguration.class
})
public class SystemForgeApplication {

	public static void main(String[] args) {
		// Load .env variables into System properties before Spring starts
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()
				.load();

		dotenv.entries().forEach(entry ->
				System.setProperty(entry.getKey(), entry.getValue())
		);

		SpringApplication.run(SystemForgeApplication.class, args);
	}
}
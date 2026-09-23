package com.rohobie.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
public class BillingApplication {

	public static void main(String[] args) {
		loadEnv();
		SpringApplication.run(BillingApplication.class, args);
	}

	private static void loadEnv() {
		Path[] paths = {
				Paths.get(".env"),
				Paths.get("../.env"),
				Paths.get("billing/.env")
		};
		for (Path path : paths) {
			if (Files.exists(path)) {
				try {
					List<String> lines = Files.readAllLines(path);
					for (String line : lines) {
						String trimmed = line.trim();
						if (!trimmed.isEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
							int eq = trimmed.indexOf('=');
							String key = trimmed.substring(0, eq).trim();
							String value = trimmed.substring(eq + 1).trim();
							if (System.getProperty(key) == null && System.getenv(key) == null) {
								System.setProperty(key, value);
							}
						}
					}
				} catch (IOException ignored) {
				}
				break;
			}
		}
	}

}

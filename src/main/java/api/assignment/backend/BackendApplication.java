package api.assignment.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		// Temporary debug: print ALL env vars to diagnose Railway injection
		System.out.println("=== ALL ENV VARS ===");
		System.getenv().forEach((k, v) -> {
			// Mask passwords/secrets
			String val = k.toLowerCase().contains("password") || k.toLowerCase().contains("secret")
					? "****" : v;
			System.out.println(k + "=" + val);
		});
		System.out.println("=== END ALL ENV VARS ===");
		SpringApplication.run(BackendApplication.class, args);
	}

}

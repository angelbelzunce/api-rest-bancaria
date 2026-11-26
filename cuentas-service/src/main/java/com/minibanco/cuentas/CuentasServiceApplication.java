package com.minibanco.cuentas;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@OpenAPIDefinition(info = @Info(
        title = "Mini-Banco - Accounts API",
        version = "1.0",
        description = "Accounts and transfers with idempotency, optimistic locking and Kafka events."
))

@EnableScheduling
@SpringBootApplication
public class CuentasServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CuentasServiceApplication.class, args);
	}

}

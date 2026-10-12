package com.tanay.incomestatement;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

import tools.jackson.databind.ObjectMapper;

@SpringBootApplication
public class IncomeStatementApplication {

	public static void main(String[] args) {
		SpringApplication.run(IncomeStatementApplication.class, args);
	}

	/**
	 * The ledger is read once at startup. Debit and credit strings such as "12450.75"
	 * become BigDecimal directly, so no amount passes through a floating-point type.
	 */
	@Bean
	Ledger ledger(ObjectMapper mapper, @Value("${ledger.path}") Resource file) throws IOException {
		try (InputStream in = file.getInputStream()) {
			return mapper.readValue(in, Ledger.class);
		}
	}

}

package com.tanay.incomestatement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

import tools.jackson.databind.ObjectMapper;

@SpringBootApplication
public class IncomeStatementApplication {

	private static final Logger log = LoggerFactory.getLogger(IncomeStatementApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(IncomeStatementApplication.class, args);
	}

	/**
	 * The ledger is read once at startup. A bad file never stops the app: whatever fails
	 * validation is left out and reported with every statement.
	 */
	@Bean
	Ledger ledger(ObjectMapper mapper, @Value("${ledger.path}") Resource file) {
		Ledger ledger = LedgerReader.read(mapper, file);
		log.info("Ledger loaded from {}: {} accounts and {} entries are valid, {} accounts and {} entries have errors",
				file.getDescription(), ledger.accounts().size(), ledger.journalEntries().size(),
				ledger.erroredAccounts().size(), ledger.erroredEntries().size());
		if (ledger.fileError() != null) {
			log.warn(ledger.fileError());
		}
		return ledger;
	}

}

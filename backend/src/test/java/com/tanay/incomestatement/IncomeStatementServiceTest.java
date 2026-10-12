package com.tanay.incomestatement;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Runs the service against the real ledger.json. Expected amounts are worked out by
 * hand from the journal entries, never copied from the service's output.
 */
@SpringBootTest
class IncomeStatementServiceTest {

	@Autowired
	private IncomeStatementService service;

	// TODO (by hand)

	private IncomeStatement statement(String start, String end) {
		return service.statement(LocalDate.parse(start), LocalDate.parse(end));
	}

}

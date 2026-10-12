package com.tanay.incomestatement;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

@Service
public class IncomeStatementService {

	private final Ledger ledger;

	public IncomeStatementService(Ledger ledger) {
		this.ledger = ledger;
	}

	/**
	 * The income statement for entries dated from start to end, both inclusive.
	 */
	public IncomeStatement statement(LocalDate start, LocalDate end) {
		throw new UnsupportedOperationException("TODO (by hand)");
	}

}

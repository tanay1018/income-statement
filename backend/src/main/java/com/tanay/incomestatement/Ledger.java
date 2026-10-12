package com.tanay.incomestatement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The contents of ledger.json: the chart of accounts and the journal entries.
 */
public record Ledger(
		String company,
		String currency,
		List<Account> accounts,
		@JsonProperty("journal_entries") List<JournalEntry> journalEntries) {

	public record Account(
			String number,
			String name,
			AccountType type,
			Subtype subtype,
			@JsonProperty("is_active") boolean active) {
	}

	public record JournalEntry(
			String id,
			LocalDate date,
			EntryStatus status,
			String memo,
			List<JournalLine> lines) {
	}

	public record JournalLine(
			String account,
			BigDecimal debit,
			BigDecimal credit) {
	}

	/**
	 * One line of a posted entry, with its account resolved.
	 */
	public record PostedLine(
			JournalEntry entry,
			Account account,
			BigDecimal debit,
			BigDecimal credit) {

		// TODO (by hand): amount(), positive when the line increases the account

	}

	// TODO (by hand): compact constructor that validates the ledger

	/**
	 * Every line of every posted entry dated from start to end, both inclusive.
	 */
	public List<PostedLine> postedLines(LocalDate start, LocalDate end) {
		throw new UnsupportedOperationException("TODO (by hand)");
	}

}

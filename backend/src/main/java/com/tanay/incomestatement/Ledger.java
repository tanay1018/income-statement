package com.tanay.incomestatement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The contents of ledger.json: the chart of accounts and the journal entries.
 */
public class Ledger {

	private final String company;
	private final String currency;
	private final List<Account> accounts;
	private final List<JournalEntry> journalEntries;
	// Built once when the ledger loads, so a journal line's account number can be resolved.
	private final Map<String, Account> accountsByNumber = new HashMap<>();

	@JsonCreator
	public Ledger(
			@JsonProperty("company") String company,
			@JsonProperty("currency") String currency,
			@JsonProperty("accounts") List<Account> accounts,
			@JsonProperty("journal_entries") List<JournalEntry> journalEntries) {
		this.company = company;
		this.currency = currency;
		this.accounts = accounts;
		this.journalEntries = journalEntries;
		for (Account account : accounts) {
			if (accountsByNumber.put(account.number(), account) != null) {
				throw new IllegalArgumentException("Account " + account.number() + " is listed twice");
			}
		}
		for (JournalEntry entry : journalEntries) {
			validate(entry);
		}
	}

	public String company() {
		return company;
	}

	public String currency() {
		return currency;
	}

	public List<Account> accounts() {
		return accounts;
	}

	public List<JournalEntry> journalEntries() {
		return journalEntries;
	}

	/**
	 * Every line of every posted entry dated from start to end, both inclusive.
	 */
	public List<PostedLine> postedLines(LocalDate start, LocalDate end) {
		List<PostedLine> postedLines = new ArrayList<>();
		for (JournalEntry entry : journalEntries) {
			if (entry.status() != EntryStatus.POSTED) {
				continue; // draft and void entries are not in the books
			}
			if (entry.date().isBefore(start) || entry.date().isAfter(end)) {
				continue;
			}
			for (JournalLine line : entry.lines()) {
				Account account = accountsByNumber.get(line.account());
				postedLines.add(new PostedLine(entry, account, line.debit(), line.credit()));
			}
		}
		return postedLines;
	}

	/**
	 * Rejects an entry that breaks double entry. Every entry is checked, whatever its
	 * status, so a bad file stops the app at startup and never produces a partial report.
	 */
	private void validate(JournalEntry entry) {
		if (entry.lines().size() < 2) {
			throw new IllegalArgumentException("Entry " + entry.id() + " has fewer than two lines");
		}
		BigDecimal debits = BigDecimal.ZERO;
		BigDecimal credits = BigDecimal.ZERO;
		for (JournalLine line : entry.lines()) {
			if (!accountsByNumber.containsKey(line.account())) {
				throw new IllegalArgumentException(
						"Entry " + entry.id() + " uses unknown account " + line.account());
			}
			if (line.debit().signum() < 0 || line.credit().signum() < 0) {
				throw new IllegalArgumentException("Entry " + entry.id() + " has a negative amount");
			}
			if (line.debit().scale() > 2 || line.credit().scale() > 2) {
				throw new IllegalArgumentException(
						"Entry " + entry.id() + " has an amount with more than two decimals");
			}
			if (line.debit().signum() > 0 && line.credit().signum() > 0) {
				throw new IllegalArgumentException(
						"Entry " + entry.id() + " has a line with both a debit and a credit");
			}
			debits = debits.add(line.debit());
			credits = credits.add(line.credit());
		}
		// compareTo, not equals: equals would treat 100.0 and 100.00 as different.
		if (debits.compareTo(credits) != 0) {
			throw new IllegalArgumentException(
					"Entry " + entry.id() + " does not balance: debits " + debits + ", credits " + credits);
		}
	}

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

		/** Positive when the line increases the account. */
		public BigDecimal amount() {
			if (account.type().isDebitNormal()) {
				return debit.subtract(credit);
			}
			return credit.subtract(debit);
		}

	}

}

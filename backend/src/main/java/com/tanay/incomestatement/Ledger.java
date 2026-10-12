package com.tanay.incomestatement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The accounts and journal entries that passed validation, and the ones that did not.
 * Only the valid ones are ever used in a statement. LedgerReader builds it from ledger.json.
 */
public class Ledger {

	private final String company;
	private final String currency;
	private final List<Account> accounts;
	private final List<JournalEntry> journalEntries;
	private final List<ErroredAccount> erroredAccounts;
	private final List<ErroredEntry> erroredEntries;
	private final String fileError;
	// Built once when the ledger loads, so a journal line's account number can be resolved.
	private final Map<String, Account> accountsByNumber = new HashMap<>();

	public Ledger(
			String company,
			String currency,
			List<Account> accounts,
			List<JournalEntry> journalEntries,
			List<ErroredAccount> erroredAccounts,
			List<ErroredEntry> erroredEntries,
			String fileError) {
		this.company = company;
		this.currency = currency;
		this.accounts = accounts;
		this.journalEntries = journalEntries;
		this.erroredAccounts = erroredAccounts;
		this.erroredEntries = erroredEntries;
		this.fileError = fileError;
		for (Account account : accounts) {
			accountsByNumber.put(account.number(), account);
		}
	}

	/** A ledger in which nothing is errored. */
	public Ledger(String company, String currency, List<Account> accounts, List<JournalEntry> journalEntries) {
		this(company, currency, accounts, journalEntries, List.of(), List.of(), null);
	}

	/** An empty ledger for a file that could not be read at all. */
	public static Ledger unreadable(String fileError) {
		return new Ledger(null, null, List.of(), List.of(), List.of(), List.of(), fileError);
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

	public List<ErroredAccount> erroredAccounts() {
		return erroredAccounts;
	}

	public List<ErroredEntry> erroredEntries() {
		return erroredEntries;
	}

	/** Why the file could not be read, or null when it could. */
	public String fileError() {
		return fileError;
	}

	public record Account(
			String number,
			String name,
			AccountType type,
			Subtype subtype,
			@JsonProperty("is_active") Boolean active) {
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
	 * An account that failed validation. position is its place in the file, starting at 1.
	 * number is as written in the file, and null when it has none.
	 */
	public record ErroredAccount(
			int position,
			String number,
			List<String> reasons) {
	}

	/**
	 * A journal entry that failed validation. position is its place in the file, starting
	 * at 1. id, date and memo are as written in the file, and null when missing.
	 */
	public record ErroredEntry(
			int position,
			String id,
			String date,
			String memo,
			List<String> reasons) {
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

}

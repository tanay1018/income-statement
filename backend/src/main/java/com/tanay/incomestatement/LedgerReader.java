package com.tanay.incomestatement;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.Resource;

import com.tanay.incomestatement.Ledger.Account;
import com.tanay.incomestatement.Ledger.ErroredAccount;
import com.tanay.incomestatement.Ledger.ErroredEntry;
import com.tanay.incomestatement.Ledger.JournalEntry;
import com.tanay.incomestatement.Ledger.JournalLine;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * Reads ledger.json and sorts every account and every journal entry into valid or errored.
 *
 * Each one is read and validated on its own, so one bad entry never stops the rest. An
 * errored entry is left out as a whole: dropping one line would leave it unbalanced.
 */
public final class LedgerReader {

	private final ObjectMapper mapper;
	private final List<Account> accounts = new ArrayList<>();
	private final Map<String, Account> accountsByNumber = new HashMap<>();
	private final List<ErroredAccount> erroredAccounts = new ArrayList<>();
	private final Set<String> erroredAccountNumbers = new HashSet<>();
	private final List<JournalEntry> entries = new ArrayList<>();
	private final List<ErroredEntry> erroredEntries = new ArrayList<>();

	private LedgerReader(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	public static Ledger read(ObjectMapper mapper, Resource file) {
		JsonNode root;
		try (InputStream in = file.getInputStream()) {
			// A JSON number such as 12450.75 is read as an exact decimal, never as a double.
			root = mapper.reader().with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(in);
		}
		catch (IOException | JacksonException e) {
			return Ledger.unreadable("The ledger file could not be read: " + e.getMessage().lines().findFirst().orElse(""));
		}
		if (!root.path("accounts").isArray() || !root.path("journal_entries").isArray()) {
			return Ledger.unreadable("The ledger file must have an accounts list and a journal_entries list");
		}

		LedgerReader reader = new LedgerReader(mapper);
		reader.readAccounts(root.get("accounts")); // accounts first: entries are checked against them
		reader.readEntries(root.get("journal_entries"));
		return new Ledger(
				text(root, "company"),
				text(root, "currency"),
				reader.accounts,
				reader.entries,
				reader.erroredAccounts,
				reader.erroredEntries,
				null);
	}

	// ---------- Accounts ----------

	private void readAccounts(JsonNode list) {
		int position = 0;
		for (JsonNode node : list) {
			position++;
			String number = text(node, "number");
			List<String> reasons = new ArrayList<>();
			try {
				Account account = mapper.treeToValue(node, Account.class);
				reasons.addAll(validate(account));
				if (reasons.isEmpty()) {
					accounts.add(account);
					accountsByNumber.put(account.number(), account);
					continue;
				}
			}
			catch (JacksonException e) {
				reasons.add(readFailure(e));
			}
			erroredAccounts.add(new ErroredAccount(position, number, reasons));
			if (number != null) {
				erroredAccountNumbers.add(number);
			}
		}
	}

	private List<String> validate(Account account) {
		List<String> reasons = new ArrayList<>();
		if (account.number() == null) {
			reasons.add("number is missing");
		}
		else if (accountsByNumber.containsKey(account.number())) {
			reasons.add("number " + account.number() + " is already used by an earlier account");
		}
		if (account.name() == null) {
			reasons.add("name is missing");
		}
		if (account.type() == null) {
			reasons.add("type is missing");
		}
		if (account.subtype() == null) {
			reasons.add("subtype is missing");
		}
		if (account.type() != null && account.subtype() != null && !account.type().fits(account.subtype())) {
			// An asset filed under Revenue, for example, would be reported with the wrong sign.
			reasons.add("type " + jsonName(account.type()) + " does not fit subtype " + jsonName(account.subtype()));
		}
		return reasons;
	}

	// ---------- Journal entries ----------

	private void readEntries(JsonNode list) {
		int position = 0;
		for (JsonNode node : list) {
			position++;
			List<String> reasons = new ArrayList<>();
			try {
				JournalEntry entry = mapper.treeToValue(node, JournalEntry.class);
				reasons.addAll(validate(entry));
				if (reasons.isEmpty()) {
					entries.add(entry);
					continue;
				}
			}
			catch (JacksonException e) {
				reasons.add(readFailure(e));
			}
			erroredEntries.add(new ErroredEntry(position, text(node, "id"), text(node, "date"), text(node, "memo"), reasons));
		}
	}

	/** Runs every check and returns every reason the entry is not valid. Empty means valid. */
	private List<String> validate(JournalEntry entry) {
		List<String> reasons = new ArrayList<>();
		if (entry.id() == null) {
			reasons.add("id is missing");
		}
		if (entry.date() == null) {
			reasons.add("date is missing");
		}
		if (entry.status() == null) {
			reasons.add("status is missing");
		}
		if (entry.lines() == null || entry.lines().size() < 2) {
			reasons.add("it has fewer than two lines");
			return reasons;
		}

		BigDecimal debits = BigDecimal.ZERO;
		BigDecimal credits = BigDecimal.ZERO;
		boolean everyLineHasAmounts = true;
		for (int i = 0; i < entry.lines().size(); i++) {
			JournalLine line = entry.lines().get(i);
			String label = "line " + (i + 1);

			if (line.account() == null) {
				reasons.add(label + " has no account");
			}
			else if (erroredAccountNumbers.contains(line.account())) {
				reasons.add(label + " uses account " + line.account() + ", which has errors");
			}
			else if (!accountsByNumber.containsKey(line.account())) {
				reasons.add(label + " uses account " + line.account() + ", which is not in the chart of accounts");
			}

			if (line.debit() == null) {
				reasons.add(label + " has no debit");
			}
			if (line.credit() == null) {
				reasons.add(label + " has no credit");
			}
			if (line.debit() == null || line.credit() == null) {
				everyLineHasAmounts = false;
				continue;
			}
			if (line.debit().signum() < 0 || line.credit().signum() < 0) {
				reasons.add(label + " has a negative amount");
			}
			if (line.debit().scale() > 2 || line.credit().scale() > 2) {
				reasons.add(label + " has an amount with more than two decimals");
			}
			if (line.debit().signum() > 0 && line.credit().signum() > 0) {
				reasons.add(label + " has both a debit and a credit");
			}
			debits = debits.add(line.debit());
			credits = credits.add(line.credit());
		}
		// compareTo, not equals: equals would treat 100.0 and 100.00 as different.
		if (everyLineHasAmounts && debits.compareTo(credits) != 0) {
			reasons.add("debits " + debits + " do not equal credits " + credits);
		}
		return reasons;
	}

	// ---------- Helpers ----------

	/**
	 * Jackson's error for a value it could not read, in plain words, such as:
	 * line 1 debit "abc" is not a valid amount.
	 */
	private static String readFailure(JacksonException e) {
		if (!(e instanceof InvalidFormatException bad) || bad.getPath().isEmpty()) {
			return e.getOriginalMessage();
		}
		List<JacksonException.Reference> path = bad.getPath();
		String field = path.get(path.size() - 1).getPropertyName();
		// For a field of a journal line the path is: lines, the line's index, the field.
		String line = path.size() == 3 ? "line " + (path.get(1).getIndex() + 1) + " " : "";
		String kind = field.equals("debit") || field.equals("credit") ? "amount" : field;
		return line + field + " \"" + bad.getValue() + "\" is not a valid " + kind;
	}

	/** OPERATING_REVENUE is written operating_revenue in the file. */
	private static String jsonName(Enum<?> constant) {
		return constant.name().toLowerCase();
	}

	/** A field as written in the file, for showing an errored item. Null when it is not there. */
	private static String text(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull() || value.isObject() || value.isArray()) {
			return null;
		}
		return value.asString();
	}

}

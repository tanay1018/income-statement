package com.tanay.incomestatement;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;

import com.tanay.incomestatement.Ledger.ErroredAccount;
import com.tanay.incomestatement.Ledger.ErroredEntry;
import com.tanay.incomestatement.Ledger.JournalEntry;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * What LedgerReader does with a file that has something wrong in it. Each test starts
 * from the small valid ledger below, breaks one thing, and checks what lands in
 * erroredEntries or erroredAccounts, and why.
 */
class LedgerReaderTest {

	private static final String VALID_LEDGER = """
			{
			  "company": "Test Co",
			  "currency": "USD",
			  "accounts": [
			    {"number": "1000", "name": "Cash", "type": "asset", "subtype": "balance_sheet", "is_active": true},
			    {"number": "4000", "name": "Sales", "type": "revenue", "subtype": "operating_revenue", "is_active": true},
			    {"number": "6100", "name": "Rent", "type": "expense", "subtype": "operating_expense", "is_active": true}
			  ],
			  "journal_entries": [
			    {"id": "JE-1", "date": "2026-01-10", "status": "posted", "memo": "Sale", "lines": [
			      {"account": "1000", "debit": "100.00", "credit": "0.00"},
			      {"account": "4000", "debit": "0.00", "credit": "100.00"}]},
			    {"id": "JE-2", "date": "2026-01-20", "status": "posted", "memo": "Rent", "lines": [
			      {"account": "6100", "debit": "40.00", "credit": "0.00"},
			      {"account": "1000", "debit": "0.00", "credit": "40.00"}]}
			  ]
			}
			""";

	private final ObjectMapper mapper = JsonMapper.builder().build();

	// ---------- A valid file ----------

	@Test
	void validLedgerHasNothingErrored() {
		Ledger ledger = read(validLedger());

		assertThat(ledger.company()).isEqualTo("Test Co");
		assertThat(ledger.currency()).isEqualTo("USD");
		assertThat(ledger.accounts()).hasSize(3);
		assertThat(entryIds(ledger)).containsExactly("JE-1", "JE-2");
		assertThat(ledger.erroredAccounts()).isEmpty();
		assertThat(ledger.erroredEntries()).isEmpty();
		assertThat(ledger.fileError()).isNull();
	}

	@Test
	void onlyTheBadEntryIsErroredAndTheRestStillLoad() {
		ObjectNode root = validLedger();
		line(root, 0, 0).put("debit", "90.00");

		Ledger ledger = read(root);

		assertThat(entryIds(ledger)).containsExactly("JE-2");
		assertThat(ledger.erroredEntries()).containsExactly(
				new ErroredEntry(1, "JE-1", "2026-01-10", "Sale", List.of("debits 90.00 do not equal credits 100.00")));
	}

	// ---------- Accounts on a line ----------

	@Test
	void lineWithAnAccountThatIsNotInTheChartErrorsTheEntry() {
		ObjectNode root = validLedger();
		line(root, 0, 1).put("account", "9999");

		assertThat(reasons(root)).containsExactly("line 2 uses account 9999, which is not in the chart of accounts");
	}

	@Test
	void lineWithNoAccountErrorsTheEntry() {
		ObjectNode root = validLedger();
		line(root, 0, 1).remove("account");

		assertThat(reasons(root)).containsExactly("line 2 has no account");
	}

	// ---------- Amounts ----------

	@Test
	void amountThatIsNotANumberErrorsTheEntry() {
		ObjectNode notANumber = validLedger();
		line(notANumber, 0, 0).put("debit", "abc");
		ObjectNode withCurrencySign = validLedger();
		line(withCurrencySign, 0, 1).put("credit", "$100.00");

		assertThat(reasons(notANumber)).containsExactly("line 1 debit \"abc\" is not a valid amount");
		assertThat(reasons(withCurrencySign)).containsExactly("line 2 credit \"$100.00\" is not a valid amount");
	}

	@Test
	void missingNullOrEmptyAmountErrorsTheEntry() {
		ObjectNode missing = validLedger();
		line(missing, 0, 0).remove("debit");
		ObjectNode isNull = validLedger();
		line(isNull, 0, 1).putNull("credit");
		ObjectNode empty = validLedger();
		line(empty, 0, 0).put("debit", "");

		assertThat(reasons(missing)).containsExactly("line 1 has no debit");
		assertThat(reasons(isNull)).containsExactly("line 2 has no credit");
		assertThat(reasons(empty)).containsExactly("line 1 has no debit");
	}

	@Test
	void negativeAmountErrorsTheEntry() {
		ObjectNode root = validLedger();
		line(root, 0, 0).put("debit", "-100.00");
		line(root, 0, 1).put("credit", "-100.00");

		assertThat(reasons(root)).containsExactly("line 1 has a negative amount", "line 2 has a negative amount");
	}

	@Test
	void amountWithMoreThanTwoDecimalsErrorsTheEntry() {
		ObjectNode root = validLedger();
		line(root, 0, 0).put("debit", "100.005");
		line(root, 0, 1).put("credit", "100.005");

		assertThat(reasons(root)).containsExactly(
				"line 1 has an amount with more than two decimals",
				"line 2 has an amount with more than two decimals");
	}

	@Test
	void lineWithBothADebitAndACreditErrorsTheEntry() {
		ObjectNode root = validLedger();
		line(root, 0, 0).put("credit", "100.00");
		line(root, 0, 1).put("debit", "100.00");

		assertThat(reasons(root)).containsExactly(
				"line 1 has both a debit and a credit",
				"line 2 has both a debit and a credit");
	}

	@Test
	void entryWhoseDebitsDoNotEqualItsCreditsIsErrored() {
		ObjectNode root = validLedger();
		line(root, 0, 0).put("debit", "90.00");

		assertThat(reasons(root)).containsExactly("debits 90.00 do not equal credits 100.00");
	}

	@Test
	void equalAmountsWrittenWithDifferentDecimalsBalance() {
		ObjectNode root = validLedger();
		line(root, 0, 0).put("debit", "100");
		line(root, 0, 1).put("credit", "100.0");

		assertThat(read(root).erroredEntries()).isEmpty();
	}

	@Test
	void amountsWrittenAsJsonNumbersAreReadExactly() {
		String json = VALID_LEDGER
				.replace("\"debit\": \"100.00\"", "\"debit\": 1234567.10")
				.replace("\"credit\": \"100.00\"", "\"credit\": 1234567.10");

		Ledger ledger = LedgerReader.read(mapper, resource(json));

		assertThat(ledger.erroredEntries()).isEmpty();
		assertThat(ledger.journalEntries().get(0).lines().get(0).debit()).isEqualTo(new BigDecimal("1234567.10"));
	}

	// ---------- Entry fields ----------

	@Test
	void missingDateStatusOrIdErrorsTheEntry() {
		ObjectNode noDate = validLedger();
		entry(noDate, 0).remove("date");
		ObjectNode noStatus = validLedger();
		entry(noStatus, 0).remove("status");
		ObjectNode noId = validLedger();
		entry(noId, 0).remove("id");

		assertThat(reasons(noDate)).containsExactly("date is missing");
		assertThat(reasons(noStatus)).containsExactly("status is missing");
		assertThat(reasons(noId)).containsExactly("id is missing");
	}

	@Test
	void entryWithNoIdIsReportedByItsPositionInTheFile() {
		ObjectNode root = validLedger();
		entry(root, 1).remove("id");

		ErroredEntry errored = read(root).erroredEntries().get(0);

		assertThat(errored.id()).isNull();
		assertThat(errored.position()).isEqualTo(2);
		assertThat(errored.memo()).isEqualTo("Rent");
	}

	@Test
	void dateThatIsNotARealDateErrorsTheEntry() {
		ObjectNode wrongFormat = validLedger();
		entry(wrongFormat, 0).put("date", "01/10/2026");
		ObjectNode impossible = validLedger();
		entry(impossible, 0).put("date", "2026-02-30");

		assertThat(reasons(wrongFormat)).containsExactly("date \"01/10/2026\" is not a valid date");
		assertThat(reasons(impossible)).containsExactly("date \"2026-02-30\" is not a valid date");
	}

	@Test
	void unknownStatusErrorsTheEntry() {
		ObjectNode root = validLedger();
		entry(root, 0).put("status", "approved");

		assertThat(reasons(root)).containsExactly("status \"approved\" is not a valid status");
	}

	@Test
	void entryWithFewerThanTwoLinesIsErrored() {
		ObjectNode noLines = validLedger();
		entry(noLines, 0).remove("lines");
		ObjectNode oneLine = validLedger();
		((ArrayNode) entry(oneLine, 0).get("lines")).remove(1);

		assertThat(reasons(noLines)).containsExactly("it has fewer than two lines");
		assertThat(reasons(oneLine)).containsExactly("it has fewer than two lines");
	}

	@Test
	void everyReasonIsReportedNotOnlyTheFirst() {
		ObjectNode root = validLedger();
		entry(root, 0).remove("date");
		entry(root, 0).remove("status");
		line(root, 0, 0).put("account", "9999");
		line(root, 0, 1).put("credit", "90.00");

		assertThat(reasons(root)).containsExactly(
				"date is missing",
				"status is missing",
				"line 1 uses account 9999, which is not in the chart of accounts",
				"debits 100.00 do not equal credits 90.00");
	}

	// ---------- Chart of accounts ----------

	@Test
	void accountWithNoTypeIsErroredAndSoAreTheEntriesThatUseIt() {
		ObjectNode root = validLedger();
		account(root, 1).remove("type");

		Ledger ledger = read(root);

		assertThat(ledger.erroredAccounts()).containsExactly(new ErroredAccount(2, "4000", List.of("type is missing")));
		assertThat(ledger.accounts()).extracting(Ledger.Account::number).containsExactly("1000", "6100");
		// JE-1 uses 4000 and is errored. JE-2 does not, and stays.
		assertThat(entryIds(ledger)).containsExactly("JE-2");
		assertThat(ledger.erroredEntries().get(0).reasons())
				.containsExactly("line 2 uses account 4000, which has errors");
	}

	@Test
	void accountWithNoSubtypeNameOrNumberIsErrored() {
		ObjectNode noSubtype = validLedger();
		account(noSubtype, 1).remove("subtype");
		ObjectNode noName = validLedger();
		account(noName, 1).remove("name");
		ObjectNode noNumber = validLedger();
		account(noNumber, 2).remove("number");

		assertThat(read(noSubtype).erroredAccounts())
				.containsExactly(new ErroredAccount(2, "4000", List.of("subtype is missing")));
		assertThat(read(noName).erroredAccounts())
				.containsExactly(new ErroredAccount(2, "4000", List.of("name is missing")));
		assertThat(read(noNumber).erroredAccounts())
				.containsExactly(new ErroredAccount(3, null, List.of("number is missing")));
	}

	@Test
	void accountWithAnUnknownTypeIsErrored() {
		ObjectNode root = validLedger();
		account(root, 1).put("type", "income");

		assertThat(read(root).erroredAccounts())
				.containsExactly(new ErroredAccount(2, "4000", List.of("type \"income\" is not a valid type")));
	}

	@Test
	void accountWhoseTypeDoesNotFitItsSubtypeIsErrored() {
		// An asset filed under Revenue would be reported with the wrong sign.
		ObjectNode root = validLedger();
		account(root, 1).put("type", "asset");

		assertThat(read(root).erroredAccounts()).containsExactly(
				new ErroredAccount(2, "4000", List.of("type asset does not fit subtype operating_revenue")));
	}

	@Test
	void accountNumberUsedTwiceErrorsTheLaterAccount() {
		ObjectNode root = validLedger();
		account(root, 2).put("number", "1000");

		Ledger ledger = read(root);

		assertThat(ledger.erroredAccounts()).containsExactly(
				new ErroredAccount(3, "1000", List.of("number 1000 is already used by an earlier account")));
		assertThat(ledger.accounts()).extracting(Ledger.Account::name).containsExactly("Cash", "Sales");
	}

	@Test
	void missingIsActiveIsNotAnError() {
		ObjectNode root = validLedger();
		account(root, 1).remove("is_active");

		Ledger ledger = read(root);

		assertThat(ledger.erroredAccounts()).isEmpty();
		assertThat(ledger.erroredEntries()).isEmpty();
	}

	// ---------- The file as a whole ----------

	@Test
	void fileWithNoAccountsOrNoJournalEntriesListIsAFileError() {
		ObjectNode noAccounts = validLedger();
		noAccounts.remove("accounts");
		ObjectNode noEntries = validLedger();
		noEntries.remove("journal_entries");

		assertThat(read(noAccounts).fileError())
				.isEqualTo("The ledger file must have an accounts list and a journal_entries list");
		assertThat(read(noEntries).fileError())
				.isEqualTo("The ledger file must have an accounts list and a journal_entries list");
		assertThat(read(noAccounts).journalEntries()).isEmpty();
	}

	@Test
	void fileThatIsNotJsonIsAFileErrorWithAnEmptyLedger() {
		Ledger ledger = LedgerReader.read(mapper, resource("this is not json"));

		assertThat(ledger.accounts()).isEmpty();
		assertThat(ledger.journalEntries()).isEmpty();
		assertThat(ledger.fileError()).startsWith("The ledger file could not be read: ");
	}

	@Test
	void fileThatDoesNotExistIsAFileErrorWithAnEmptyLedger() {
		Ledger ledger = LedgerReader.read(mapper, new FileSystemResource("/no/such/ledger.json"));

		assertThat(ledger.journalEntries()).isEmpty();
		assertThat(ledger.fileError()).startsWith("The ledger file could not be read: ");
	}

	// ---------- Helpers ----------

	private ObjectNode validLedger() {
		return (ObjectNode) mapper.readTree(VALID_LEDGER);
	}

	private Ledger read(ObjectNode root) {
		return LedgerReader.read(mapper, resource(root.toString()));
	}

	private static ObjectNode account(ObjectNode root, int index) {
		return (ObjectNode) root.get("accounts").get(index);
	}

	private static ObjectNode entry(ObjectNode root, int index) {
		return (ObjectNode) root.get("journal_entries").get(index);
	}

	private static ObjectNode line(ObjectNode root, int entryIndex, int lineIndex) {
		return (ObjectNode) entry(root, entryIndex).get("lines").get(lineIndex);
	}

	/** The reasons of the one entry the file should have had errored. */
	private List<String> reasons(ObjectNode root) {
		List<ErroredEntry> errored = read(root).erroredEntries();
		assertThat(errored).hasSize(1);
		return errored.get(0).reasons();
	}

	private static List<String> entryIds(Ledger ledger) {
		return ledger.journalEntries().stream().map(JournalEntry::id).toList();
	}

	private static ByteArrayResource resource(String text) {
		return new ByteArrayResource(text.getBytes(StandardCharsets.UTF_8));
	}

}

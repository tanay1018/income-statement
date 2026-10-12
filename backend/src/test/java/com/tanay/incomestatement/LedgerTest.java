package com.tanay.incomestatement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tanay.incomestatement.Ledger.Account;
import com.tanay.incomestatement.Ledger.JournalEntry;
import com.tanay.incomestatement.Ledger.JournalLine;
import com.tanay.incomestatement.Ledger.PostedLine;

/**
 * Validation and postedLines, on small ledgers built directly from the records.
 */
class LedgerTest {

	private static final Account CASH = new Account("1000", "Cash", AccountType.ASSET, Subtype.BALANCE_SHEET, true);
	private static final Account SALES = new Account("4000", "Sales", AccountType.REVENUE, Subtype.OPERATING_REVENUE,
			true);
	private static final Account RENT = new Account("6100", "Rent", AccountType.EXPENSE, Subtype.OPERATING_EXPENSE,
			true);

	private static final LocalDate START = LocalDate.of(2026, 1, 1);
	private static final LocalDate END = LocalDate.of(2026, 1, 31);

	// ---------- postedLines ----------

	@Test
	void postedLinesLeavesOutDraftAndVoidEntries() {
		Ledger ledger = ledger(
				sale("POSTED-1", "2026-01-10", EntryStatus.POSTED),
				sale("DRAFT-1", "2026-01-10", EntryStatus.DRAFT),
				sale("VOID-1", "2026-01-10", EntryStatus.VOID));

		assertThat(entryIds(ledger.postedLines(START, END))).containsExactly("POSTED-1", "POSTED-1");
	}

	@Test
	void postedLinesIncludesTheStartAndEndDatesAndNothingOutside() {
		Ledger ledger = ledger(
				sale("DAY-BEFORE", "2025-12-31", EntryStatus.POSTED),
				sale("ON-START", "2026-01-01", EntryStatus.POSTED),
				sale("ON-END", "2026-01-31", EntryStatus.POSTED),
				sale("DAY-AFTER", "2026-02-01", EntryStatus.POSTED));

		assertThat(entryIds(ledger.postedLines(START, END)))
				.containsExactly("ON-START", "ON-START", "ON-END", "ON-END");
	}

	@Test
	void postedLinesFiltersByDateNotByOrderInTheFile() {
		Ledger ledger = ledger(
				sale("LATER", "2026-01-20", EntryStatus.POSTED),
				sale("EARLIER", "2026-01-05", EntryStatus.POSTED));

		assertThat(entryIds(ledger.postedLines(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10))))
				.containsExactly("EARLIER", "EARLIER");
	}

	@Test
	void postedLinesResolvesEachLinesAccount() {
		Ledger ledger = ledger(sale("JE-1", "2026-01-10", EntryStatus.POSTED));

		List<PostedLine> lines = ledger.postedLines(START, END);

		assertThat(lines).hasSize(2);
		assertThat(lines.get(0).account()).isEqualTo(CASH);
		assertThat(lines.get(0).debit()).isEqualByComparingTo("100.00");
		assertThat(lines.get(1).account()).isEqualTo(SALES);
		assertThat(lines.get(1).credit()).isEqualByComparingTo("100.00");
	}

	// ---------- PostedLine.amount ----------

	@Test
	void creditToRevenueIsPositiveAndDebitIsNegative() {
		assertThat(amount(SALES, "0.00", "100.00")).isEqualByComparingTo("100.00");
		assertThat(amount(SALES, "30.00", "0.00")).isEqualByComparingTo("-30.00");
	}

	@Test
	void debitToExpenseIsPositiveAndCreditIsNegative() {
		assertThat(amount(RENT, "100.00", "0.00")).isEqualByComparingTo("100.00");
		assertThat(amount(RENT, "0.00", "30.00")).isEqualByComparingTo("-30.00");
	}

	@Test
	void debitToAnAssetIsPositiveAndCreditIsNegative() {
		assertThat(amount(CASH, "100.00", "0.00")).isEqualByComparingTo("100.00");
		assertThat(amount(CASH, "0.00", "30.00")).isEqualByComparingTo("-30.00");
	}

	// ---------- Validation ----------

	@Test
	void rejectsAnEntryThatDoesNotBalance() {
		JournalEntry unbalanced = entry("JE-BAD", "2026-01-10", EntryStatus.POSTED,
				debit(CASH, "100.00"), credit(SALES, "90.00"));

		assertThatThrownBy(() -> ledger(unbalanced))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("JE-BAD")
				.hasMessageContaining("does not balance");
	}

	@Test
	void rejectsAnUnbalancedEntryEvenWhenItIsADraft() {
		JournalEntry unbalanced = entry("JE-DRAFT", "2026-01-10", EntryStatus.DRAFT,
				debit(CASH, "100.00"), credit(SALES, "90.00"));

		assertThatThrownBy(() -> ledger(unbalanced))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("JE-DRAFT");
	}

	@Test
	void acceptsAmountsThatAreEqualButWrittenWithDifferentScale() {
		JournalEntry entry = entry("JE-1", "2026-01-10", EntryStatus.POSTED,
				debit(CASH, "100.0"), credit(SALES, "100.00"));

		assertThat(ledger(entry).postedLines(START, END)).hasSize(2);
	}

	@Test
	void rejectsALineWithAnUnknownAccount() {
		JournalEntry entry = entry("JE-BAD", "2026-01-10", EntryStatus.POSTED,
				debit(CASH, "100.00"), new JournalLine("9999", new BigDecimal("0.00"), new BigDecimal("100.00")));

		assertThatThrownBy(() -> ledger(entry))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("JE-BAD")
				.hasMessageContaining("unknown account 9999");
	}

	@Test
	void rejectsALineWithBothADebitAndACredit() {
		JournalEntry entry = entry("JE-BAD", "2026-01-10", EntryStatus.POSTED,
				new JournalLine("1000", new BigDecimal("100.00"), new BigDecimal("100.00")),
				new JournalLine("4000", new BigDecimal("0.00"), new BigDecimal("0.00")));

		assertThatThrownBy(() -> ledger(entry))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("JE-BAD")
				.hasMessageContaining("both a debit and a credit");
	}

	@Test
	void rejectsANegativeAmount() {
		JournalEntry entry = entry("JE-BAD", "2026-01-10", EntryStatus.POSTED,
				debit(CASH, "-100.00"), credit(SALES, "-100.00"));

		assertThatThrownBy(() -> ledger(entry))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("JE-BAD")
				.hasMessageContaining("negative amount");
	}

	@Test
	void rejectsAnAmountWithMoreThanTwoDecimals() {
		JournalEntry entry = entry("JE-BAD", "2026-01-10", EntryStatus.POSTED,
				debit(CASH, "100.005"), credit(SALES, "100.005"));

		assertThatThrownBy(() -> ledger(entry))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("JE-BAD")
				.hasMessageContaining("more than two decimals");
	}

	@Test
	void rejectsAnEntryWithFewerThanTwoLines() {
		JournalEntry entry = entry("JE-BAD", "2026-01-10", EntryStatus.POSTED, debit(CASH, "0.00"));

		assertThatThrownBy(() -> ledger(entry))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("JE-BAD")
				.hasMessageContaining("fewer than two lines");
	}

	@Test
	void rejectsAnAccountNumberListedTwice() {
		Account duplicate = new Account("1000", "Petty Cash", AccountType.ASSET, Subtype.BALANCE_SHEET, true);

		assertThatThrownBy(() -> new Ledger("Test Co", "USD", List.of(CASH, duplicate), List.of()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("1000");
	}

	// ---------- Helpers ----------

	private static Ledger ledger(JournalEntry... entries) {
		return new Ledger("Test Co", "USD", List.of(CASH, SALES, RENT), List.of(entries));
	}

	/** A 100.00 sale for cash. */
	private static JournalEntry sale(String id, String date, EntryStatus status) {
		return entry(id, date, status, debit(CASH, "100.00"), credit(SALES, "100.00"));
	}

	private static JournalEntry entry(String id, String date, EntryStatus status, JournalLine... lines) {
		return new JournalEntry(id, LocalDate.parse(date), status, "", List.of(lines));
	}

	private static JournalLine debit(Account account, String amount) {
		return new JournalLine(account.number(), new BigDecimal(amount), new BigDecimal("0.00"));
	}

	private static JournalLine credit(Account account, String amount) {
		return new JournalLine(account.number(), new BigDecimal("0.00"), new BigDecimal(amount));
	}

	private static BigDecimal amount(Account account, String debit, String credit) {
		return new PostedLine(null, account, new BigDecimal(debit), new BigDecimal(credit)).amount();
	}

	private static List<String> entryIds(List<PostedLine> lines) {
		return lines.stream().map(line -> line.entry().id()).toList();
	}

}

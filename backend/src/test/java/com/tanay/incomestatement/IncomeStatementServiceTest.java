package com.tanay.incomestatement;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.tanay.incomestatement.IncomeStatement.Line;

/**
 * Runs the service against the real ledger.json. Expected amounts are worked out by
 * hand from the journal entries, never copied from the service's output.
 */
@SpringBootTest
class IncomeStatementServiceTest {

	@Autowired
	private IncomeStatementService service;

	@Test
	void firstQuarter2026() {
		IncomeStatement statement = statement("2026-01-01", "2026-03-31");

		assertThat(statement.company()).isEqualTo("Northwind Coffee Roasters");
		assertThat(statement.currency()).isEqualTo("USD");
		assertThat(statement.start()).isEqualTo(LocalDate.of(2026, 1, 1));
		assertThat(statement.end()).isEqualTo(LocalDate.of(2026, 3, 31));

		assertLines(statement.revenue().lines(),
				"4000 Product Revenue 35650.75", // 12450.75 + 8200.00 + 15000.00, void JE-009 left out
				"4100 Subscription Revenue 3000.00", // 3 x 1000.00, the 12000.00 billing is deferred
				"4900 Sales Returns & Discounts -800.25"); // -(650.25 + 150.00)
		assertThat(statement.revenue().total()).isEqualByComparingTo("37850.50");

		assertLines(statement.costOfGoodsSold().lines(),
				"5000 Cost of Goods Sold 14272.75"); // 4980.30 + 3280.00 + 6012.45
		assertThat(statement.costOfGoodsSold().total()).isEqualByComparingTo("14272.75");

		assertThat(statement.grossProfit()).isEqualByComparingTo("23577.75");

		assertLines(statement.operatingExpenses().lines(),
				"6000 Salaries 55500.00", // 3 x 18500.00, draft bonus JE-019 left out
				"6100 Rent 9000.00",
				"6200 Software 1099.97", // 1199.97 - 100.00 vendor credit
				"6300 Marketing (legacy) 2500.10"); // inactive account, still reported
		assertThat(statement.operatingExpenses().total()).isEqualByComparingTo("68100.07");

		assertThat(statement.operatingIncome()).isEqualByComparingTo("-44522.32");

		assertLines(statement.otherIncome().lines(),
				"7000 Interest Income 42.18");
		assertThat(statement.otherIncome().total()).isEqualByComparingTo("42.18");

		assertThat(statement.netIncome()).isEqualByComparingTo("-44480.14");
	}

	@Test
	void theThreeMonthsAddUpToTheQuarter() {
		BigDecimal january = statement("2026-01-01", "2026-01-31").netIncome();
		BigDecimal february = statement("2026-02-01", "2026-02-28").netIncome();
		BigDecimal march = statement("2026-03-01", "2026-03-31").netIncome();

		assertThat(january).isEqualByComparingTo("-21529.65");
		assertThat(february).isEqualByComparingTo("-13230.25");
		assertThat(march).isEqualByComparingTo("-9720.24");
		assertThat(january.add(february).add(march)).isEqualByComparingTo("-44480.14");
	}

	// ---------- Entry status ----------

	@Test
	void voidEntryIsExcluded() {
		// JE-009 (void) and JE-010 (posted) are the same 8200.00 sale, entered twice.
		IncomeStatement statement = statement("2026-02-03", "2026-02-03");

		assertLines(statement.revenue().lines(), "4000 Product Revenue 8200.00");
	}

	@Test
	void draftEntryIsExcluded() {
		// JE-019 is a draft 5000.00 bonus, the only entry dated 2026-03-15.
		assertNoActivity(statement("2026-03-15", "2026-03-15"));
	}

	// ---------- Date range ----------

	@Test
	void startDateIsInclusive() {
		// JE-007 rent is dated 2026-01-01. The next entry is on 2026-01-05.
		assertLines(statement("2026-01-01", "2026-01-04").operatingExpenses().lines(), "6100 Rent 9000.00");
		assertNoActivity(statement("2026-01-02", "2026-01-04"));
	}

	@Test
	void endDateIsInclusive() {
		// JE-021, JE-022 and JE-023 are dated 2026-03-31: 1000.00 - 18500.00 + 42.18.
		assertThat(statement("2026-03-21", "2026-03-31").netIncome()).isEqualByComparingTo("-17457.82");
		assertNoActivity(statement("2026-03-21", "2026-03-30"));
	}

	@Test
	void entryIsFoundByDateNotByItsPlaceInTheFile() {
		// JE-007 (2026-01-01) is listed after JE-002 (2026-01-05) in ledger.json.
		IncomeStatement statement = statement("2026-01-01", "2026-01-01");

		assertLines(statement.operatingExpenses().lines(), "6100 Rent 9000.00");
		assertThat(statement.netIncome()).isEqualByComparingTo("-9000.00");
	}

	@Test
	void entriesOutsideTheQuarterCountOnlyInAWiderRange() {
		// JE-001 is dated 2025-12-15 and JE-024 is dated 2026-04-01.
		assertThat(statement("2025-12-01", "2025-12-31").netIncome()).isEqualByComparingTo("5000.00");
		assertThat(statement("2026-04-01", "2026-04-30").netIncome()).isEqualByComparingTo("9100.00");
		// 5000.00 - 44480.14 + 9100.00
		assertThat(statement("2025-12-15", "2026-04-01").netIncome()).isEqualByComparingTo("-30380.14");
	}

	// ---------- Accounts and signs ----------

	@Test
	void contraRevenueIsANegativeLineInsideRevenue() {
		// February: JE-010 sale, JE-014 subscription, JE-012 return of 650.25.
		IncomeStatement statement = statement("2026-02-01", "2026-02-28");

		assertLines(statement.revenue().lines(),
				"4000 Product Revenue 8200.00",
				"4100 Subscription Revenue 1000.00",
				"4900 Sales Returns & Discounts -650.25");
		assertThat(statement.revenue().total()).isEqualByComparingTo("8549.75");
	}

	@Test
	void entryWithThreeLinesPostsRevenueAndDiscountSeparately() {
		// JE-016: 15000.00 credit to revenue and a 150.00 debit to returns and discounts.
		IncomeStatement statement = statement("2026-03-02", "2026-03-02");

		assertLines(statement.revenue().lines(),
				"4000 Product Revenue 15000.00",
				"4900 Sales Returns & Discounts -150.00");
		assertThat(statement.revenue().total()).isEqualByComparingTo("14850.00");
	}

	@Test
	void creditToAnExpenseAccountReducesTheExpense() {
		// JE-018 software 1199.97 on 2026-03-10, then JE-020 vendor credit of 100.00 on 2026-03-20.
		assertLines(statement("2026-03-10", "2026-03-20").operatingExpenses().lines(), "6200 Software 1099.97");

		// The credit on its own is a negative expense, which raises income.
		IncomeStatement creditOnly = statement("2026-03-20", "2026-03-20");
		assertLines(creditOnly.operatingExpenses().lines(), "6200 Software -100.00");
		assertThat(creditOnly.netIncome()).isEqualByComparingTo("100.00");
	}

	@Test
	void otherIncomeIsOutsideRevenueAndOperatingIncome() {
		// March: account 7000 has type revenue but subtype other_income.
		IncomeStatement statement = statement("2026-03-01", "2026-03-31");

		assertThat(statement.revenue().total()).isEqualByComparingTo("15850.00"); // 15000 + 1000 - 150
		assertThat(statement.operatingIncome()).isEqualByComparingTo("-9762.42");
		assertLines(statement.otherIncome().lines(), "7000 Interest Income 42.18");
		assertThat(statement.netIncome()).isEqualByComparingTo("-9720.24"); // -9762.42 + 42.18
	}

	@Test
	void inactiveAccountWithActivityIsStillReported() {
		// JE-008 posts 2500.10 to 6300 Marketing (legacy), which has is_active false.
		IncomeStatement statement = statement("2026-01-20", "2026-01-20");

		assertLines(statement.operatingExpenses().lines(), "6300 Marketing (legacy) 2500.10");
	}

	@Test
	void balanceSheetOnlyEntriesDoNotAppear() {
		// JE-004 (2026-01-10) bills a subscription to deferred revenue. JE-013 (2026-02-15)
		// is a cash receipt. Neither touches a revenue or expense account.
		assertNoActivity(statement("2026-01-10", "2026-01-10"));
		assertNoActivity(statement("2026-02-15", "2026-02-15"));
	}

	@Test
	void rentIsReportedWhenRecordedNotSpreadOverTheQuarter() {
		// JE-007 expenses three months of rent on 2026-01-01.
		assertLines(statement("2026-01-01", "2026-01-31").operatingExpenses().lines(),
				"6000 Salaries 18500.00",
				"6100 Rent 9000.00",
				"6300 Marketing (legacy) 2500.10");
		assertLines(statement("2026-02-01", "2026-03-31").operatingExpenses().lines(),
				"6000 Salaries 37000.00",
				"6200 Software 1099.97");
	}

	@Test
	void accountsWithoutActivityInThePeriodHaveNoLine() {
		// January has no returns (4900) and no interest (7000).
		IncomeStatement statement = statement("2026-01-01", "2026-01-31");

		assertLines(statement.revenue().lines(),
				"4000 Product Revenue 12450.75",
				"4100 Subscription Revenue 1000.00");
		assertThat(statement.otherIncome().lines()).isEmpty();
	}

	// ---------- Empty results ----------

	@Test
	void periodWithNoEntriesHasEmptySectionsAndZeroTotals() {
		assertNoActivity(statement("2027-01-01", "2027-12-31"));
	}

	private static void assertNoActivity(IncomeStatement statement) {
		assertThat(statement.revenue().lines()).isEmpty();
		assertThat(statement.costOfGoodsSold().lines()).isEmpty();
		assertThat(statement.operatingExpenses().lines()).isEmpty();
		assertThat(statement.otherIncome().lines()).isEmpty();
		// Compared as text, so a total must be 0.00 and not 0.
		assertThat(statement.revenue().total().toPlainString()).isEqualTo("0.00");
		assertThat(statement.costOfGoodsSold().total().toPlainString()).isEqualTo("0.00");
		assertThat(statement.grossProfit().toPlainString()).isEqualTo("0.00");
		assertThat(statement.operatingExpenses().total().toPlainString()).isEqualTo("0.00");
		assertThat(statement.operatingIncome().toPlainString()).isEqualTo("0.00");
		assertThat(statement.otherIncome().total().toPlainString()).isEqualTo("0.00");
		assertThat(statement.netIncome().toPlainString()).isEqualTo("0.00");
	}

	private IncomeStatement statement(String start, String end) {
		return service.statement(LocalDate.parse(start), LocalDate.parse(end));
	}

	/** Each expected line is written as "number name amount", in the order it should appear. */
	private static void assertLines(List<Line> lines, String... expected) {
		List<String> actual = lines.stream()
				.map(line -> line.accountNumber() + " " + line.accountName() + " " + line.amount().toPlainString())
				.toList();
		assertThat(actual).containsExactly(expected);
	}

}

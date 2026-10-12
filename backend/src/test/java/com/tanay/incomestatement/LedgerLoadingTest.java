package com.tanay.incomestatement;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.tanay.incomestatement.Ledger.Account;
import com.tanay.incomestatement.Ledger.JournalEntry;
import com.tanay.incomestatement.Ledger.JournalLine;

/**
 * Checks that the supplied ledger.json is read into the records as written: names, enums,
 * dates and exact amounts. No accounting rules are tested here.
 */
@SpringBootTest
class LedgerLoadingTest {

	@Autowired
	private Ledger ledger;

	@Test
	void loadsCompanyCurrencyAndEveryAccountAndEntry() {
		assertThat(ledger.company()).isEqualTo("Northwind Coffee Roasters");
		assertThat(ledger.currency()).isEqualTo("USD");
		assertThat(ledger.accounts()).hasSize(15);
		assertThat(ledger.journalEntries()).hasSize(25);
	}

	@Test
	void nothingInTheSuppliedLedgerIsErrored() {
		assertThat(ledger.erroredAccounts()).isEmpty();
		assertThat(ledger.erroredEntries()).isEmpty();
		assertThat(ledger.fileError()).isNull();
	}

	@Test
	void loadsAccountFieldsIncludingSnakeCaseNames() {
		assertThat(ledger.accounts()).contains(
				new Account("4900", "Sales Returns & Discounts", AccountType.REVENUE, Subtype.CONTRA_REVENUE, true),
				new Account("6300", "Marketing (legacy)", AccountType.EXPENSE, Subtype.OPERATING_EXPENSE, false));
	}

	@Test
	void loadsEntryFieldsWithExactAmounts() {
		JournalEntry entry = ledger.journalEntries().stream()
				.filter(e -> e.id().equals("JE-002"))
				.findFirst()
				.orElseThrow();

		assertThat(entry.date()).isEqualTo(LocalDate.of(2026, 1, 5));
		assertThat(entry.status()).isEqualTo(EntryStatus.POSTED);
		assertThat(entry.memo()).isEqualTo("January product sales");
		assertThat(entry.lines()).containsExactly(
				new JournalLine("1100", new BigDecimal("12450.75"), new BigDecimal("0.00")),
				new JournalLine("4000", new BigDecimal("0.00"), new BigDecimal("12450.75")));
	}

}

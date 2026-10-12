package com.tanay.incomestatement;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Starts the whole app on a ledger file that has something wrong with it. The app must
 * start, answer 200, count only the valid entries, and list the errored ones.
 */
class BadLedgerFileTest {

	/**
	 * ledger-with-problems.json is the supplied ledger with eight things broken:
	 * account 6300 has no type (so JE-008 cannot be used), JE-002 has a debit that is not a
	 * number, JE-006 has no date, JE-012 does not balance, JE-018 has no status, JE-019
	 * (a draft) does not balance, JE-023 uses an account that is not in the chart, and
	 * JE-024 (April) has a line with no credit.
	 */
	@Nested
	@SpringBootTest(properties = "ledger.path=classpath:ledger-with-problems.json")
	@AutoConfigureMockMvc
	class LedgerWithProblems {

		@Autowired
		private MockMvc mvc;

		@Test
		void statementCountsOnlyTheValidEntries() throws Exception {
			// Worked out by hand from the Q1 figures of the supplied ledger:
			// Product Revenue 35650.75 - 12450.75 (JE-002) = 23200.00
			// Returns -800.25 + 650.25 (JE-012) = -150.00, so Revenue is 26050.00
			// Salaries 55500.00 - 18500.00 (JE-006) = 37000.00
			// Software is only the -100.00 credit (JE-018 errored), Marketing is gone (JE-008)
			// Operating expenses 37000.00 + 9000.00 - 100.00 = 45900.00, Other income 0.00 (JE-023)
			mvc.perform(get("/income-statement?start=2026-01-01&end=2026-03-31"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.revenue.total").value("26050.00"))
					.andExpect(jsonPath("$.costOfGoodsSold.total").value("14272.75"))
					.andExpect(jsonPath("$.grossProfit").value("11777.25"))
					.andExpect(jsonPath("$.operatingExpenses.total").value("45900.00"))
					.andExpect(jsonPath("$.operatingIncome").value("-34122.75"))
					.andExpect(jsonPath("$.otherIncome.total").value("0.00"))
					.andExpect(jsonPath("$.netIncome").value("-34122.75"));
		}

		@Test
		void statementListsEveryErroredEntryWithItsReason() throws Exception {
			mvc.perform(get("/income-statement?start=2026-01-01&end=2026-03-31"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.erroredEntries.length()").value(8))
					.andExpect(jsonPath("$.erroredEntries[0].id").value("JE-002"))
					.andExpect(jsonPath("$.erroredEntries[0].position").value(2))
					.andExpect(jsonPath("$.erroredEntries[0].date").value("2026-01-05"))
					.andExpect(jsonPath("$.erroredEntries[0].memo").value("January product sales"))
					.andExpect(jsonPath("$.erroredEntries[0].reasons[0]")
							.value("line 1 debit \"twelve thousand\" is not a valid amount"))
					.andExpect(jsonPath("$.erroredEntries[1].id").value("JE-006"))
					.andExpect(jsonPath("$.erroredEntries[1].date").doesNotExist())
					.andExpect(jsonPath("$.erroredEntries[1].reasons[0]").value("date is missing"))
					.andExpect(jsonPath("$.erroredEntries[2].id").value("JE-008"))
					.andExpect(jsonPath("$.erroredEntries[2].reasons[0]")
							.value("line 1 uses account 6300, which has errors"))
					.andExpect(jsonPath("$.erroredEntries[3].id").value("JE-012"))
					.andExpect(jsonPath("$.erroredEntries[3].reasons[0]")
							.value("debits 600.00 do not equal credits 650.25"))
					.andExpect(jsonPath("$.erroredEntries[4].id").value("JE-018"))
					.andExpect(jsonPath("$.erroredEntries[4].reasons[0]").value("status is missing"))
					.andExpect(jsonPath("$.erroredEntries[5].id").value("JE-019"))
					.andExpect(jsonPath("$.erroredEntries[5].reasons[0]")
							.value("debits 5000.00 do not equal credits 4000.00"))
					.andExpect(jsonPath("$.erroredEntries[6].id").value("JE-023"))
					.andExpect(jsonPath("$.erroredEntries[6].reasons[0]")
							.value("line 2 uses account 7999, which is not in the chart of accounts"))
					.andExpect(jsonPath("$.erroredEntries[7].id").value("JE-024"))
					.andExpect(jsonPath("$.erroredEntries[7].reasons[0]").value("line 2 has no credit"));
		}

		@Test
		void statementListsTheErroredAccount() throws Exception {
			mvc.perform(get("/income-statement?start=2026-01-01&end=2026-03-31"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.erroredAccounts.length()").value(1))
					.andExpect(jsonPath("$.erroredAccounts[0].number").value("6300"))
					.andExpect(jsonPath("$.erroredAccounts[0].position").value(14))
					.andExpect(jsonPath("$.erroredAccounts[0].reasons[0]").value("type is missing"));
		}

	}

	@Nested
	@SpringBootTest(properties = "ledger.path=classpath:not-a-ledger.txt")
	@AutoConfigureMockMvc
	class FileThatIsNotJson {

		@Autowired
		private MockMvc mvc;

		@Test
		void appStartsAndTheStatementSaysTheFileCouldNotBeRead() throws Exception {
			mvc.perform(get("/income-statement?start=2026-01-01&end=2026-03-31"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.netIncome").value("0.00"))
					.andExpect(jsonPath("$.erroredEntries.length()").value(0))
					.andExpect(jsonPath("$.ledgerError").value(startsWith("The ledger file could not be read: ")));
		}

	}

	@Nested
	@SpringBootTest(properties = "ledger.path=file:/no/such/ledger.json")
	@AutoConfigureMockMvc
	class FileThatDoesNotExist {

		@Autowired
		private MockMvc mvc;

		@Test
		void appStartsAndTheStatementSaysTheFileCouldNotBeRead() throws Exception {
			mvc.perform(get("/income-statement?start=2026-01-01&end=2026-03-31"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.netIncome").value("0.00"))
					.andExpect(jsonPath("$.ledgerError").value(startsWith("The ledger file could not be read: ")));
		}

	}

}

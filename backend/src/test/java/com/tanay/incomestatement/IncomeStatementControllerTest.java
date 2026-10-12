package com.tanay.incomestatement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The HTTP endpoint with the real service and ledger behind it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class IncomeStatementControllerTest {

	@Autowired
	private MockMvc mvc;

	// ---------- 200 ----------

	@Test
	void returnsTheStatementForTheRequestedDates() throws Exception {
		mvc.perform(get("/income-statement?start=2026-01-01&end=2026-03-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.company").value("Northwind Coffee Roasters"))
				.andExpect(jsonPath("$.currency").value("USD"))
				.andExpect(jsonPath("$.start").value("2026-01-01"))
				.andExpect(jsonPath("$.end").value("2026-03-31"))
				.andExpect(jsonPath("$.revenue.lines.length()").value(3))
				.andExpect(jsonPath("$.revenue.lines[0].accountNumber").value("4000"))
				.andExpect(jsonPath("$.revenue.lines[0].accountName").value("Product Revenue"))
				.andExpect(jsonPath("$.costOfGoodsSold.lines.length()").value(1))
				.andExpect(jsonPath("$.operatingExpenses.lines.length()").value(4))
				.andExpect(jsonPath("$.otherIncome.lines.length()").value(1));
	}

	@Test
	void writesEveryAmountAsADecimalString() throws Exception {
		// value("...") only matches a JSON string, so a JSON number such as -44480.14 fails here.
		mvc.perform(get("/income-statement?start=2026-01-01&end=2026-03-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.revenue.lines[0].amount").value("35650.75"))
				.andExpect(jsonPath("$.revenue.lines[2].amount").value("-800.25"))
				.andExpect(jsonPath("$.revenue.total").value("37850.50"))
				.andExpect(jsonPath("$.costOfGoodsSold.total").value("14272.75"))
				.andExpect(jsonPath("$.grossProfit").value("23577.75"))
				.andExpect(jsonPath("$.operatingExpenses.total").value("68100.07"))
				.andExpect(jsonPath("$.operatingIncome").value("-44522.32"))
				.andExpect(jsonPath("$.otherIncome.total").value("42.18"))
				.andExpect(jsonPath("$.netIncome").value("-44480.14"));
	}

	@Test
	void periodWithNoEntriesIsOkWithZeroTotals() throws Exception {
		mvc.perform(get("/income-statement?start=2027-01-01&end=2027-12-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.revenue.lines.length()").value(0))
				.andExpect(jsonPath("$.revenue.total").value("0.00"))
				.andExpect(jsonPath("$.netIncome").value("0.00"));
	}

	@Test
	void startEqualToEndIsASingleDay() throws Exception {
		// JE-021, JE-022 and JE-023 are dated 2026-03-31: 1000.00 - 18500.00 + 42.18.
		mvc.perform(get("/income-statement?start=2026-03-31&end=2026-03-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.netIncome").value("-17457.82"));
	}

	// ---------- 400 ----------

	@Test
	void missingStartIsBadRequest() throws Exception {
		mvc.perform(get("/income-statement?end=2026-03-31"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("start is required, as YYYY-MM-DD"));
	}

	@Test
	void missingEndIsBadRequest() throws Exception {
		mvc.perform(get("/income-statement?start=2026-01-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("end is required, as YYYY-MM-DD"));
	}

	@Test
	void emptyDateIsBadRequest() throws Exception {
		mvc.perform(get("/income-statement?start=&end=2026-03-31"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("start is required, as YYYY-MM-DD"));
	}

	@Test
	void malformedDateIsBadRequest() throws Exception {
		mvc.perform(get("/income-statement?start=01/01/2026&end=2026-03-31"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("start must be a valid date, as YYYY-MM-DD"));
	}

	@Test
	void dateThatDoesNotExistIsBadRequest() throws Exception {
		mvc.perform(get("/income-statement?start=2026-01-01&end=2026-02-30"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("end must be a valid date, as YYYY-MM-DD"));
	}

	@Test
	void startAfterEndIsBadRequest() throws Exception {
		mvc.perform(get("/income-statement?start=2026-03-31&end=2026-01-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("start must be on or before end"));
	}

}

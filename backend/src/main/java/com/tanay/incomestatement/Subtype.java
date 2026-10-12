package com.tanay.incomestatement;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Where an account goes on the income statement.
 */
public enum Subtype {

	@JsonProperty("operating_revenue") OPERATING_REVENUE,
	@JsonProperty("contra_revenue") CONTRA_REVENUE,
	@JsonProperty("cogs") COGS,
	@JsonProperty("operating_expense") OPERATING_EXPENSE,
	@JsonProperty("other_income") OTHER_INCOME,
	@JsonProperty("balance_sheet") BALANCE_SHEET

}

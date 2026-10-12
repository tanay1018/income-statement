package com.tanay.incomestatement;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AccountType {

	@JsonProperty("asset") ASSET,
	@JsonProperty("liability") LIABILITY,
	@JsonProperty("equity") EQUITY,
	@JsonProperty("revenue") REVENUE,
	@JsonProperty("expense") EXPENSE;

	/** Assets and expenses grow with debits. Liabilities, equity and revenue grow with credits. */
	public boolean isDebitNormal() {
		return this == ASSET || this == EXPENSE;
	}

}

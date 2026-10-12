package com.tanay.incomestatement;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AccountType {

	@JsonProperty("asset") ASSET,
	@JsonProperty("liability") LIABILITY,
	@JsonProperty("equity") EQUITY,
	@JsonProperty("revenue") REVENUE,
	@JsonProperty("expense") EXPENSE;

	// TODO (by hand): isDebitNormal()

}

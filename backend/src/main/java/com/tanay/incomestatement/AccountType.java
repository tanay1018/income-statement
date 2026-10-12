package com.tanay.incomestatement;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AccountType {

	@JsonProperty("asset") ASSET,
	@JsonProperty("liability") LIABILITY,
	@JsonProperty("equity") EQUITY,
	@JsonProperty("revenue") REVENUE,
	@JsonProperty("expense") EXPENSE;

	public boolean isDebitNormal() {
		if(this.equals(ASSET) || this.equals(EXPENSE)) return true;
		return false;
	}
}

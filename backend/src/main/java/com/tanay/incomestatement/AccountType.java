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

	/** Whether an account of this type can carry the given subtype. */
	public boolean fits(Subtype subtype) {
		return switch (this) {
			case ASSET, LIABILITY, EQUITY -> subtype == Subtype.BALANCE_SHEET;
			case REVENUE -> subtype == Subtype.OPERATING_REVENUE
					|| subtype == Subtype.CONTRA_REVENUE
					|| subtype == Subtype.OTHER_INCOME;
			case EXPENSE -> subtype == Subtype.COGS || subtype == Subtype.OPERATING_EXPENSE;
		};
	}

}

package com.tanay.incomestatement;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum EntryStatus {

	@JsonProperty("posted") POSTED,
	@JsonProperty("draft") DRAFT,
	@JsonProperty("void") VOID

}

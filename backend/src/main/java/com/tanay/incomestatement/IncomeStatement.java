package com.tanay.incomestatement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * The response of GET /income-statement, in the order the statement is read.
 * Amounts are written as strings such as "1234.50", so no client parses them as floats.
 */
public record IncomeStatement(
		String company,
		String currency,
		LocalDate start,
		LocalDate end,
		Section revenue,
		Section costOfGoodsSold,
		@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal grossProfit,
		Section operatingExpenses,
		@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal operatingIncome,
		Section otherIncome,
		@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal netIncome) {

	public record Section(
			List<Line> lines,
			@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal total) {
	}

	public record Line(
			String accountNumber,
			String accountName,
			@JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount) {
	}

}

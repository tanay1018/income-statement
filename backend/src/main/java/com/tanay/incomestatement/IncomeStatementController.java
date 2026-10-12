package com.tanay.incomestatement;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IncomeStatementController {

	private final IncomeStatementService service;

	public IncomeStatementController(IncomeStatementService service) {
		this.service = service;
	}

	// TODO (by hand): 400 with {"message": "..."} when start is after end, and for a
	// missing or malformed date.
	@GetMapping("/income-statement")
	public IncomeStatement incomeStatement(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
		return service.statement(start, end);
	}

}

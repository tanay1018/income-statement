package com.tanay.incomestatement;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
public class IncomeStatementController {

	private final IncomeStatementService service;

	public IncomeStatementController(IncomeStatementService service) {
		this.service = service;
	}

	/**
	 * Both dates are required, in YYYY-MM-DD form, and both are inclusive.
	 */
	@GetMapping("/income-statement")
	public ResponseEntity<?> incomeStatement(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
		if (start.isAfter(end)) {
			return ResponseEntity.badRequest().body(new ErrorResponse("start must be on or before end"));
		}
		return ResponseEntity.ok(service.statement(start, end));
	}

	/** Spring throws this when start or end is not in the request. */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse missingDate(MissingServletRequestParameterException e) {
		return new ErrorResponse(e.getParameterName() + " is required, as YYYY-MM-DD");
	}

	/** Spring throws this when a value is not a real date, such as 2026-02-30 or "yesterday". */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse invalidDate(MethodArgumentTypeMismatchException e) {
		return new ErrorResponse(e.getName() + " must be a valid date, as YYYY-MM-DD");
	}

	public record ErrorResponse(String message) {
	}

}

package com.tanay.incomestatement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.tanay.incomestatement.IncomeStatement.Line;
import com.tanay.incomestatement.IncomeStatement.Section;
import com.tanay.incomestatement.Ledger.Account;
import com.tanay.incomestatement.Ledger.PostedLine;

@Service
public class IncomeStatementService {

	// Two decimals, so an empty total is written as "0.00" and not "0".
	private static final BigDecimal ZERO = new BigDecimal("0.00");

	private final Ledger ledger;

	public IncomeStatementService(Ledger ledger) {
		this.ledger = ledger;
	}

	/**
	 * The income statement for entries dated from start to end, both inclusive.
	 */
	public IncomeStatement statement(LocalDate start, LocalDate end) {
		Map<String, BigDecimal> netAmounts = netAmountsByAccount(start, end);

		Section revenue = buildSection(netAmounts, Subtype.OPERATING_REVENUE, Subtype.CONTRA_REVENUE);
		Section costOfGoodsSold = buildSection(netAmounts, Subtype.COGS);
		Section operatingExpenses = buildSection(netAmounts, Subtype.OPERATING_EXPENSE);
		Section otherIncome = buildSection(netAmounts, Subtype.OTHER_INCOME);

		BigDecimal grossProfit = revenue.total().subtract(costOfGoodsSold.total());
		BigDecimal operatingIncome = grossProfit.subtract(operatingExpenses.total());
		BigDecimal netIncome = operatingIncome.add(otherIncome.total());

		return new IncomeStatement(
				ledger.company(),
				ledger.currency(),
				start,
				end,
				revenue,
				costOfGoodsSold,
				grossProfit,
				operatingExpenses,
				operatingIncome,
				otherIncome,
				netIncome);
	}

	/**
	 * The net amount of each income statement account in the period, by account number.
	 * An account with no posted line in the period is not in the map.
	 */
	private Map<String, BigDecimal> netAmountsByAccount(LocalDate start, LocalDate end) {
		Map<String, BigDecimal> netAmounts = new HashMap<>();
		for (PostedLine line : ledger.postedLines(start, end)) {
			Account account = line.account();
			if (account.subtype() == Subtype.BALANCE_SHEET) {
				continue; // assets, liabilities and equity are not on the income statement
			}
			BigDecimal runningTotal = netAmounts.getOrDefault(account.number(), ZERO);
			netAmounts.put(account.number(), runningTotal.add(line.amount()));
		}
		return netAmounts;
	}

	/**
	 * One line per account of the given subtypes that had activity, in chart of accounts
	 * order, and their total.
	 */
	private Section buildSection(Map<String, BigDecimal> netAmounts, Subtype... subtypes) {
		List<Line> lines = new ArrayList<>();
		BigDecimal total = ZERO;
		Set<Subtype> sectionSubtypes = Set.of(subtypes);
		for (Account account : ledger.accounts()) {
			if (!sectionSubtypes.contains(account.subtype())) {
				continue;
			}
			BigDecimal amount = netAmounts.get(account.number());
			if (amount == null) {
				continue; // no activity in the period
			}
			lines.add(new Line(account.number(), account.name(), amount));
			total = total.add(amount);
		}
		return new Section(lines, total);
	}

}

# Notes

## Net income for Q1 2026

**(44,480.14) USD**, a net loss, for 2026-01-01 to 2026-03-31.

| Revenue | Cost of goods sold | Gross profit | Operating expenses | Operating income | Other income | Net income |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 37,850.50 | 14,272.75 | 23,577.75 | 68,100.07 | (44,522.32) | 42.18 | (44,480.14) |

## Decisions and assumptions

| Decision | Why | Where it shows in the data |
| --- | --- | --- |
| An entry counts only if it is `posted` and dated inside the range, with both dates included | A void entry was cancelled and a draft is not approved yet, so neither is in the books | JE-009 and JE-025 are void, JE-019 is a draft |
| `subtype` picks the section and `type` picks the sign | One rule then covers returns, discounts and vendor credits with no special cases | Returns (4900) are a negative line in Revenue; the vendor credit (JE-020) lowers Software; Interest Income (7000) is under Other income |
| Entries are reported as recorded, on their own date | The prompt says not to re-accrue or reclassify | All 9,000.00 of rent (JE-007) is in January |
| Only revenue and expense accounts reach the statement | Assets, liabilities and equity belong on the balance sheet | The 12,000.00 subscription billing (JE-004) is not revenue; only the three 1,000.00 monthly recognition entries are |
| An inactive account is reported like any other | `is_active` is about new postings, not history | Marketing (6300) has a posted entry in January (JE-008) |
| Amounts never pass through a floating-point number | Cents must add up exactly | `BigDecimal` from the ledger's strings, strings in the response, text formatting on the page |

## How the numbers were checked

1. **By hand.** I did the Q1 accounting on paper from the journal entries.

   ![Q1 worked out by hand](docs/q1-by-hand.jpg)

2. **By an LLM, separately from the app.** I gave it the ledger JSON and the decisions above
   and had it do the accounting on its own, without the app's code.
3. **Against the app.** The numbers from steps 1 and 2 were cross-checked with what the app
   shows, and they are the expected values in the tests.

## Where AI helped, and where it was wrong

**Where it helped:** end to end, from gathering context and designing the solution to
coding and validating. I worked as the solution architect. I first built the plan with it
from my own guidance and preferences, and then it executed that plan while I followed
every step and corrected it where it was wrong.

**Where it was wrong:**

- **The ledger's account lookup.** It modelled `Ledger` as a Java record, which cannot hold
  an extra field, and proposed rebuilding the account-number lookup on every request. I had
  it build the lookup once at load, so `Ledger` is a class.
- **Work repeated inside a loop.** To decide whether an account belongs in a section, it
  rebuilt the list of that section's subtypes for every account. I had it build a set once,
  before the loop.
- **It assumed the ledger file is always complete.** It took every field to be present and
  well formed, so beyond request date validation and a few startup checks (an entry that
  does not balance, an unknown account) it added no handling for missing or malformed data.

On `main`, the scaffold to the last code commit took about 1 hour 40 minutes. Planning came
before that, and the `errored-entries` branch after.

## With more time

- **Proper error handling for a bad ledger file.** Validate every account and entry on its
  own, list the ones that fail with every reason, leave them out of every total, and show
  them above the statement. One bad row could then neither stop the report nor slip into the
  numbers. A working version is on the `errored-entries` branch.
- **An option to include drafts.** A draft is an entry waiting for approval, such as the
  5,000.00 Q1 bonus accrual (JE-019). Including drafts would show what the statement will
  look like once they are approved, which is useful before closing a period.
- Tests for the page's amount formatting.
- Drill down from a line to its journal entries, and two periods side by side.

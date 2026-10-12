// Shape of GET /income-statement. Amounts are decimal strings such as "1234.50".

export type Line = {
  accountNumber: string
  accountName: string
  amount: string
}

export type Section = {
  lines: Line[]
  total: string
}

// An account or a journal entry that failed validation, so it is in no total. position is
// its place in the file, starting at 1. The other fields are as written in the file, and
// null when missing.
export type ErroredAccount = {
  position: number
  number: string | null
  reasons: string[]
}

export type ErroredEntry = {
  position: number
  id: string | null
  date: string | null
  memo: string | null
  reasons: string[]
}

export type IncomeStatement = {
  company: string | null
  currency: string | null
  start: string
  end: string
  revenue: Section
  costOfGoodsSold: Section
  grossProfit: string
  operatingExpenses: Section
  operatingIncome: string
  otherIncome: Section
  netIncome: string
  erroredAccounts: ErroredAccount[]
  erroredEntries: ErroredEntry[]
  // Set when the ledger file could not be read at all.
  ledgerError: string | null
}

// start and end are YYYY-MM-DD strings, as an <input type="date"> gives them.
export async function fetchIncomeStatement(start: string, end: string): Promise<IncomeStatement> {
  const params = new URLSearchParams({ start, end })

  let response: Response
  try {
    response = await fetch(`/income-statement?${params}`)
  } catch {
    throw new Error('Could not reach the server.')
  }

  if (!response.ok) {
    throw new Error(await errorMessage(response))
  }
  return response.json()
}

// The backend answers invalid input with 400 and {"message": "..."}.
async function errorMessage(response: Response): Promise<string> {
  try {
    const body = await response.json()
    if (typeof body.message === 'string') {
      return body.message
    }
  } catch {
    // The body was not JSON.
  }
  return `The server returned an error (${response.status}).`
}

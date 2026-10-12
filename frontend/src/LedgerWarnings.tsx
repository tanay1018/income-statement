import type { ErroredAccount, ErroredEntry } from './api'

type Props = {
  erroredAccounts: ErroredAccount[]
  erroredEntries: ErroredEntry[]
  ledgerError: string | null
}

// Lists what failed validation and is therefore in none of the totals below. Renders
// nothing when the ledger had no errors, which is the normal case.
function LedgerWarnings({ erroredAccounts, erroredEntries, ledgerError }: Props) {
  if (erroredAccounts.length === 0 && erroredEntries.length === 0 && !ledgerError) {
    return null
  }

  return (
    <section className="warnings" role="alert">
      <h2>This statement is incomplete</h2>
      {ledgerError ? <p>{ledgerError}</p> : <p>The totals below do not include anything listed here.</p>}

      {erroredAccounts.length > 0 && (
        <>
          <h3>Accounts with errors ({erroredAccounts.length})</h3>
          <ul className="errored">
            {erroredAccounts.map((account) => (
              <li key={account.position}>
                <p className="errored-item">
                  <strong>{account.number ?? `Account #${account.position} in the file`}</strong>
                </p>
                <Reasons reasons={account.reasons} />
              </li>
            ))}
          </ul>
        </>
      )}

      {erroredEntries.length > 0 && (
        <>
          <h3>Journal entries with errors ({erroredEntries.length})</h3>
          <ul className="errored">
            {erroredEntries.map((entry) => (
              <li key={entry.position}>
                <p className="errored-item">
                  <strong>{entry.id ?? `Entry #${entry.position} in the file`}</strong>
                  <span className="errored-detail">
                    {' · '}
                    {entry.date ?? 'no date'}
                    {entry.memo && ` · ${entry.memo}`}
                  </span>
                </p>
                <Reasons reasons={entry.reasons} />
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  )
}

function Reasons({ reasons }: { reasons: string[] }) {
  return (
    <ul>
      {reasons.map((reason) => (
        <li key={reason}>{reason}</li>
      ))}
    </ul>
  )
}

export default LedgerWarnings

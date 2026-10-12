import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { fetchIncomeStatement } from './api'
import type { IncomeStatement } from './api'
import StatementTable from './StatementTable'

// The page opens on Q1 2026. Dates stay as YYYY-MM-DD strings from the inputs to the
// request, so no time zone can shift them.
const DEFAULT_START = '2026-01-01'
const DEFAULT_END = '2026-03-31'

function App() {
  const [start, setStart] = useState(DEFAULT_START)
  const [end, setEnd] = useState(DEFAULT_END)
  const [statement, setStatement] = useState<IncomeStatement | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  function loadStatement(startDate: string, endDate: string) {
    fetchIncomeStatement(startDate, endDate)
      .then((loaded) => {
        setStatement(loaded)
        setError(null)
      })
      .catch((cause: unknown) => {
        setStatement(null)
        setError(cause instanceof Error ? cause.message : 'Something went wrong.')
      })
      .finally(() => setLoading(false))
  }

  // Show the default period when the page opens. loading starts as true.
  useEffect(() => {
    loadStatement(DEFAULT_START, DEFAULT_END)
  }, [])

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)
    loadStatement(start, end)
  }

  return (
    <main>
      <form className="filter" onSubmit={handleSubmit}>
        <label>
          Start date
          <input type="date" value={start} onChange={(event) => setStart(event.target.value)} required />
        </label>
        <label>
          End date
          <input type="date" value={end} onChange={(event) => setEnd(event.target.value)} required />
        </label>
        <button type="submit" disabled={loading}>
          {loading ? 'Loading…' : 'Run report'}
        </button>
      </form>

      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {statement && <StatementTable statement={statement} />}
    </main>
  )
}

export default App

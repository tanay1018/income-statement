import type { IncomeStatement } from './api'

type Props = {
  statement: IncomeStatement
}

function StatementTable({ statement }: Props) {
  // TODO (by hand): sections, a row per account, totals and subtotals
  return (
    <section className="statement">
      <h2>Income Statement</h2>
      <p className="period">
        {statement.company}, {statement.start} to {statement.end}
      </p>
    </section>
  )
}

export default StatementTable

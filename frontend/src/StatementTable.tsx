import type { IncomeStatement, Section } from './api'
import { formatAmount } from './format'

type Props = {
  statement: IncomeStatement
}

// The statement, top to bottom, in the order the backend returns it. Every amount is
// shown as the backend sent it; nothing is added up here.
function StatementTable({ statement }: Props) {
  return (
    <section className="statement">
      <header>
        <h1>{statement.company}</h1>
        <p>Income Statement</p>
        <p>
          For the period {statement.start} to {statement.end}
        </p>
        <p className="currency">Amounts in {statement.currency}</p>
      </header>

      <table>
        <SectionRows title="Revenue" totalLabel="Total revenue" section={statement.revenue} />
        <SectionRows
          title="Cost of goods sold"
          totalLabel="Total cost of goods sold"
          section={statement.costOfGoodsSold}
        />
        <SubtotalRow label="Gross profit" amount={statement.grossProfit} />
        <SectionRows
          title="Operating expenses"
          totalLabel="Total operating expenses"
          section={statement.operatingExpenses}
        />
        <SubtotalRow label="Operating income" amount={statement.operatingIncome} />
        <SectionRows title="Other income" totalLabel="Total other income" section={statement.otherIncome} />
        <SubtotalRow label="Net income" amount={statement.netIncome} className="net-income" />
      </table>
    </section>
  )
}

type SectionRowsProps = {
  title: string
  totalLabel: string
  section: Section
}

// A section heading, one row per account, and the section total.
function SectionRows({ title, totalLabel, section }: SectionRowsProps) {
  return (
    <tbody>
      <tr className="section-title">
        <th colSpan={2} scope="rowgroup">
          {title}
        </th>
      </tr>
      {section.lines.map((line) => (
        <tr key={line.accountNumber} className="line">
          <td>
            <span className="account-number">{line.accountNumber}</span>
            {line.accountName}
          </td>
          <AmountCell amount={line.amount} />
        </tr>
      ))}
      {section.lines.length === 0 && (
        <tr className="line">
          <td className="no-activity" colSpan={2}>
            No activity
          </td>
        </tr>
      )}
      <tr className="section-total">
        <th scope="row">{totalLabel}</th>
        <AmountCell amount={section.total} />
      </tr>
    </tbody>
  )
}

type SubtotalRowProps = {
  label: string
  amount: string
  className?: string
}

// Gross profit, operating income or net income.
function SubtotalRow({ label, amount, className = '' }: SubtotalRowProps) {
  return (
    <tbody>
      <tr className={`subtotal ${className}`}>
        <th scope="row">{label}</th>
        <AmountCell amount={amount} />
      </tr>
    </tbody>
  )
}

// A right-aligned amount. A negative amount ends with ")", so a positive one gets an
// invisible ")" after it. That keeps the digits of every row in the same columns.
function AmountCell({ amount }: { amount: string }) {
  const negative = amount.startsWith('-')
  return (
    <td className="amount">
      {formatAmount(amount)}
      {!negative && <span className="paren-space">)</span>}
    </td>
  )
}

export default StatementTable

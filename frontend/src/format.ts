// Formats a decimal string from the backend for display: "-1234.50" becomes "(1,234.50)".
// It works on the text only, so the amount is never converted to a floating-point number.
export function formatAmount(amount: string): string {
  const negative = amount.startsWith('-')
  const unsigned = negative ? amount.slice(1) : amount
  const [whole, cents] = unsigned.split('.')
  const text = `${groupThousands(whole)}.${cents}`
  return negative ? `(${text})` : text
}

// "1234567" becomes "1,234,567". A comma goes before every digit that has a multiple of
// three digits left after it, except the first.
function groupThousands(digits: string): string {
  let grouped = ''
  for (let i = 0; i < digits.length; i++) {
    const digitsLeft = digits.length - i
    if (i > 0 && digitsLeft % 3 === 0) {
      grouped += ','
    }
    grouped += digits[i]
  }
  return grouped
}

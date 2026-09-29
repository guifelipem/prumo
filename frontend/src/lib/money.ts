export function money(value: string, currency = 'BRL'): string {
  const [whole, fraction = ''] = value.replace(/^\+/, '').split('.')
  const negative = whole.startsWith('-')
  const integer = new Intl.NumberFormat('pt-BR').format(BigInt(whole.replace('-', '') || '0'))
  const cents = fraction.padEnd(2, '0').slice(0, 2)
  try {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency }).formatToParts(negative ? -0.01 : 0)
      .map(part => part.type === 'integer' ? integer : part.type === 'fraction' ? cents : part.value).join('')
  } catch {
    return `${negative ? '-' : ''}${integer},${cents} ${currency}`
  }
}

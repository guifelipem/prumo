export type Account = { id: string; ownerId: string; name: string; currency: string }
export type Category = { id: string; ownerId: string; name: string; type: 'INCOME' | 'EXPENSE' | 'BOTH' }
export type Transaction = { id: string; accountId: string; type: 'INCOME' | 'EXPENSE'; amount: string; description: string; occurredAt: string; categoryId: string | null; createdAt: string; transferId: string | null }
export type TransactionInput = Pick<Transaction, 'type' | 'amount' | 'description' | 'categoryId' | 'occurredAt'>
export type Transfer = { id: string; ownerId: string; sourceAccountId: string; destinationAccountId: string; amount: string; description: string; occurredAt: string; createdAt: string }
export type TransferInput = Pick<Transfer, 'sourceAccountId' | 'destinationAccountId' | 'amount' | 'description' | 'occurredAt'>
export type Session = { accessToken: string; expiresAt: string }
export const sessionExpiredEvent = 'prumo:session-expired'
const base = import.meta.env.VITE_API_URL || '/api'
export class ApiError extends Error { constructor(public status: number) { super(status === 401 ? 'Sessão expirada ou credenciais inválidas.' : status === 409 ? 'Não foi possível concluir: há um conflito com os dados atuais.' : status === 404 ? 'Registro não encontrado.' : 'Não foi possível concluir a operação. Tente novamente.') } }
async function request<T>(path: string, options: RequestInit = {}, token?: string): Promise<T> { const response = await fetch(`${base}${path}`, { ...options, headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers } }); if (!response.ok) { if (response.status === 401 && token) window.dispatchEvent(new CustomEvent(sessionExpiredEvent, { detail: token })); throw new ApiError(response.status) } if (response.status === 204) return undefined as T; return response.json() as Promise<T> }
const json = (value: unknown) => JSON.stringify(value)
export const api = {
  login: (email: string, password: string) => request<Session>('/sessions', { method: 'POST', body: json({ email, password }) }),
  register: (email: string, password: string) => request<{ id: string; email: string }>('/users', { method: 'POST', body: json({ email, password }) }),
  accounts: (token: string, page: number) => request<Account[]>(`/accounts?page=${page}&size=20`, {}, token),
  account: (token: string, id: string) => request<Account>(`/accounts/${encodeURIComponent(id)}`, {}, token),
  createAccount: (token: string, name: string, currency: string) => request<Account>('/accounts', { method: 'POST', body: json({ name, currency }) }, token),
  categories: (token: string) => request<Category[]>('/categories', {}, token),
  createCategory: (token: string, name: string, type: Category['type']) => request<Category>('/categories', { method: 'POST', body: json({ name, type }) }, token),
  updateCategory: (token: string, id: string, name: string, type: Category['type']) => request<Category>(`/categories/${id}`, { method: 'PUT', body: json({ name, type }) }, token),
  deleteCategory: (token: string, id: string) => request<void>(`/categories/${id}`, { method: 'DELETE' }, token),
  transactions: (token: string, accountId: string, page: number) => request<Transaction[]>(`/transactions?accountId=${encodeURIComponent(accountId)}&page=${page}&size=20`, {}, token),
  balance: (token: string, accountId: string) => request<{ accountId: string; balance: string }>(`/transactions/balance?accountId=${encodeURIComponent(accountId)}`, {}, token),
  createTransaction: (token: string, data: TransactionInput & { accountId: string }) => request<Transaction>('/transactions', { method: 'POST', body: json(data) }, token),
  updateTransaction: (token: string, id: string, data: TransactionInput) => request<Transaction>(`/transactions/${id}`, { method: 'PUT', body: json(data) }, token),
  deleteTransaction: (token: string, id: string) => request<void>(`/transactions/${id}`, { method: 'DELETE' }, token),
  transfers: (token: string, page: number) => request<Transfer[]>(`/transfers?page=${page}&size=20`, {}, token),
  createTransfer: (token: string, data: TransferInput) => request<Transfer>('/transfers', { method: 'POST', body: json(data) }, token),
  updateTransfer: (token: string, id: string, data: TransferInput) => request<Transfer>(`/transfers/${id}`, { method: 'PUT', body: json(data) }, token),
  deleteTransfer: (token: string, id: string) => request<void>(`/transfers/${id}`, { method: 'DELETE' }, token),
}


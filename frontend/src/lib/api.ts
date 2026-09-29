export type Account = { id: string; ownerId: string; name: string; currency: string }
export type Category = { id: string; ownerId: string; name: string; type: 'INCOME' | 'EXPENSE' | 'BOTH' }
export type Transaction = { id: string; accountId: string; type: 'INCOME' | 'EXPENSE'; amount: number; description: string; occurredAt: string; categoryId: string | null; createdAt: string }
export type TransactionInput = Pick<Transaction, 'type' | 'amount' | 'description' | 'categoryId' | 'occurredAt'>
export type Session = { accessToken: string; expiresAt: string }
const base = import.meta.env.VITE_API_URL || '/api'
export class ApiError extends Error { constructor(public status: number) { super(status === 401 ? 'Sessão expirada ou credenciais inválidas.' : status === 409 ? 'Este email já está cadastrado.' : status === 404 ? 'Registro não encontrado.' : 'Não foi possível concluir a operação. Tente novamente.') } }
async function request<T>(path: string, options: RequestInit = {}, token?: string): Promise<T> { const response = await fetch(`${base}${path}`, { ...options, headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers } }); if (!response.ok) throw new ApiError(response.status); if (response.status === 204) return undefined as T; return response.json() as Promise<T> }
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
  balance: (token: string, accountId: string) => request<{ accountId: string; balance: number }>(`/transactions/balance?accountId=${encodeURIComponent(accountId)}`, {}, token),
  createTransaction: (token: string, data: TransactionInput & { accountId: string }) => request<Transaction>('/transactions', { method: 'POST', body: json(data) }, token),
  updateTransaction: (token: string, id: string, data: TransactionInput) => request<Transaction>(`/transactions/${id}`, { method: 'PUT', body: json(data) }, token),
  deleteTransaction: (token: string, id: string) => request<void>(`/transactions/${id}`, { method: 'DELETE' }, token),
}


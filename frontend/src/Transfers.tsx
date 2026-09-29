import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeftRight, Pencil, Plus, Trash2 } from 'lucide-react'
import { api, type Account, type Transfer, type TransferInput } from './lib/api'
import { Button } from './components/ui/button'
import { Card } from './components/ui/card'
import { Input } from './components/ui/input'
import { localDateTime } from './lib/datetime'
import { money } from './lib/money'

const date = (value: string) => new Intl.DateTimeFormat('pt-BR', { dateStyle: 'medium' }).format(new Date(value))
const errorText = (error: unknown) => error instanceof Error ? error.message : 'Ocorreu um erro.'
async function allAccounts(token: string) {
  const result: Account[] = []
  for (let page = 0; ; page++) {
    const batch = await api.accounts(token, page)
    result.push(...batch)
    if (batch.length < 20) return result
  }
}

function TransferForm({ token, accounts, transfer, done }: { token: string; accounts: Account[]; transfer?: Transfer; done: () => void }) {
  const qc = useQueryClient()
  const [sourceAccountId, setSource] = useState(transfer?.sourceAccountId ?? '')
  const [destinationAccountId, setDestination] = useState(transfer?.destinationAccountId ?? '')
  const [amount, setAmount] = useState(transfer?.amount.toString() ?? '')
  const [description, setDescription] = useState(transfer?.description ?? '')
  const [occurredAt, setOccurredAt] = useState(localDateTime(transfer?.occurredAt ?? new Date().toISOString()))
  const source = accounts.find(a => a.id === sourceAccountId)
  const destinations = accounts.filter(a => a.id !== sourceAccountId && (!source || a.currency === source.currency))
  const mutation = useMutation({
    mutationFn: () => {
      const data: TransferInput = { sourceAccountId, destinationAccountId, amount, description: description.trim(), occurredAt: new Date(occurredAt).toISOString() }
      return transfer ? api.updateTransfer(token, transfer.id, data) : api.createTransfer(token, data)
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['transfers'] })
      qc.invalidateQueries({ queryKey: ['transactions'] })
      qc.invalidateQueries({ queryKey: ['balance'] })
      done()
    },
  })
  return <form className="grid gap-4 sm:grid-cols-2" onSubmit={e => { e.preventDefault(); mutation.mutate() }}>
    <label className="block space-y-1.5 text-sm font-medium text-stone-700"><span>Conta de origem</span><select className="select" required value={sourceAccountId} onChange={e => { setSource(e.target.value); setDestination('') }}><option value="">Selecione</option>{accounts.map(a => <option key={a.id} value={a.id}>{a.name} ({a.currency})</option>)}</select></label>
    <label className="block space-y-1.5 text-sm font-medium text-stone-700"><span>Conta de destino</span><select className="select" required value={destinationAccountId} onChange={e => setDestination(e.target.value)}><option value="">Selecione</option>{destinations.map(a => <option key={a.id} value={a.id}>{a.name} ({a.currency})</option>)}</select></label>
    <label className="block space-y-1.5 text-sm font-medium text-stone-700"><span>Valor</span><Input type="number" min="0.01" max="99999999999999999.99" step="0.01" required value={amount} onChange={e => setAmount(e.target.value)}/></label>
    <label className="block space-y-1.5 text-sm font-medium text-stone-700"><span>Descrição</span><Input required maxLength={255} value={description} onChange={e => setDescription(e.target.value)}/></label>
    <label className="block space-y-1.5 text-sm font-medium text-stone-700"><span>Data e hora</span><Input type="datetime-local" required value={occurredAt} onChange={e => setOccurredAt(e.target.value)}/></label>
    <div className="flex items-end"><Button disabled={mutation.isPending || destinations.length === 0}>{mutation.isPending ? 'Salvando...' : transfer ? 'Salvar alterações' : 'Criar transferência'}</Button></div>
    {mutation.error && <p role="alert" className="sm:col-span-2 rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{errorText(mutation.error)}</p>}
    {source && destinations.length === 0 && <p className="sm:col-span-2 text-sm text-stone-500">Crie outra conta na moeda {source.currency} para transferir.</p>}
  </form>
}

export function Transfers({ token }: { token: string }) {
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<Transfer | 'new' | null>(null)
  const [deleteError, setDeleteError] = useState<unknown>()
  const transfers = useQuery({ queryKey: ['transfers', page], queryFn: () => api.transfers(token, page) })
  const accounts = useQuery({ queryKey: ['accounts', 'all'], queryFn: () => allAccounts(token) })
  const deletion = useMutation({
    mutationFn: (id: string) => api.deleteTransfer(token, id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['transfers'] })
      qc.invalidateQueries({ queryKey: ['transactions'] })
      qc.invalidateQueries({ queryKey: ['balance'] })
      setDeleteError(undefined)
    },
    onError: setDeleteError,
  })
  const account = (id: string) => accounts.data?.find(a => a.id === id)
  return <div className="space-y-5">
    <div className="flex flex-wrap items-center justify-between gap-3"><div><h2 className="text-2xl font-semibold tracking-tight text-stone-900">Transferências</h2><p className="mt-1 text-sm text-stone-500">Movimente dinheiro entre contas na mesma moeda.</p></div><Button onClick={() => setEditing(editing === 'new' ? null : 'new')}><Plus size={16}/> Nova transferência</Button></div>
    {editing && <Card className="p-5"><div className="mb-4 flex items-center justify-between"><h3 className="font-semibold">{editing === 'new' ? 'Nova transferência' : 'Editar transferência'}</h3><Button variant="ghost" size="sm" onClick={() => setEditing(null)}>Cancelar</Button></div>{accounts.isPending ? <p className="text-sm text-stone-500">Carregando contas...</p> : accounts.error ? <p role="alert" className="text-sm text-red-700">{errorText(accounts.error)}</p> : <TransferForm key={editing === 'new' ? 'new' : editing.id} token={token} accounts={accounts.data} transfer={editing === 'new' ? undefined : editing} done={() => setEditing(null)}/>}</Card>}
    {deleteError != null && <p role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{errorText(deleteError)}</p>}
    <Card className="overflow-hidden">{transfers.isPending ? <p className="p-6 text-sm text-stone-500">Carregando transferências...</p> : transfers.error ? <p role="alert" className="p-6 text-sm text-red-700">{errorText(transfers.error)}</p> : transfers.data.length === 0 ? <p className="p-6 text-sm text-stone-500">Nenhuma transferência nesta página.</p> : <div className="divide-y divide-stone-100">{transfers.data.map(t => <div key={t.id} className="flex flex-wrap items-center gap-4 px-5 py-4"><span className="rounded-xl bg-emerald-50 p-2 text-emerald-700"><ArrowLeftRight size={20}/></span><div className="min-w-0 flex-1"><p className="truncate font-medium">{t.description}</p><p className="text-xs text-stone-500">{date(t.occurredAt)} · {account(t.sourceAccountId)?.name ?? 'Conta de origem'} → {account(t.destinationAccountId)?.name ?? 'Conta de destino'}</p></div><p className="font-semibold">{money(t.amount, account(t.sourceAccountId)?.currency ?? 'BRL')}</p><div className="flex gap-1"><Button aria-label={`Editar ${t.description}`} variant="ghost" size="icon" onClick={() => setEditing(t)}><Pencil size={16}/></Button><Button aria-label={`Excluir ${t.description}`} variant="ghost" size="icon" disabled={deletion.isPending} onClick={() => { if (window.confirm('Excluir esta transferência e suas duas movimentações?')) deletion.mutate(t.id) }}><Trash2 size={16}/></Button></div></div>)}</div>}</Card>
    {transfers.data && (page > 0 || transfers.data.length === 20) && <div className="flex items-center justify-end gap-3 text-sm text-stone-500"><Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage(page - 1)}>Anterior</Button><span>Página {page + 1}</span><Button variant="outline" size="sm" disabled={transfers.data.length < 20} onClick={() => setPage(page + 1)}>Próxima</Button></div>}
  </div>
}

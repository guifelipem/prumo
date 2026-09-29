# Prumo frontend

Interface React, Vite, TypeScript, Tailwind e componentes shadcn/ui para a API do Prumo.

## Executar

Com o `api-gateway` disponível em `http://localhost:8080`:

```bash
cd frontend
npm ci
npm run dev
```

Abra `http://localhost:3000`. O Vite encaminha `/api/*` ao Gateway. Para outro endereço do Gateway, defina `VITE_API_URL` no ambiente antes do build ou da execução.

O frontend usa apenas as rotas públicas do Gateway: `/users`, `/sessions`, `/accounts`, `/transactions`, `/transactions/balance`, `/categories` e `/transfers`. O token da sessão é guardado no armazenamento local até a expiração retornada pela API. Ao expirar ou receber `401`, a interface volta à tela de login.

Valores monetários trafegam como strings decimais na API para preservar os centavos também em valores grandes.

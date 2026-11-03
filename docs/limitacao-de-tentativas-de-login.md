# Limitação de tentativas de login (LUC-73)

Decisão de 2026-11-03, proposta e implementada sem desenho prévio (o roadmap deixava em aberto). A VPS será pública e o login não tinha proteção contra força bruta.

## Regra

- Conta **só tentativas falhas** (e-mail inexistente ou senha errada). Login bem-sucedido não consome limite, e uma conta inativa com senha correta não conta como falha.
- Janela deslizante de 15 minutos, por duas chaves independentes:
  - **por e-mail** (minúsculas, sem espaços): 5 falhas. Vale para e-mails inexistentes também, para a resposta não revelar se a conta existe;
  - **por IP**: 30 falhas, para barrar quem varre muitos e-mails.
- Ao atingir o limite, `POST /api/v1/auth/login` responde `429` com `code = LOGIN_RATE_LIMITED`, header `Retry-After` (segundos até a falha mais antiga sair da janela) e **não verifica a senha**, mesmo que esteja certa. Um login correto zera o contador do e-mail (o do IP continua).
- Tudo é configurável: `LOGIN_RATE_LIMIT_ENABLED` (`true`), `LOGIN_RATE_LIMIT_MAX_PER_EMAIL` (`5`), `LOGIN_RATE_LIMIT_MAX_PER_IP` (`30`), `LOGIN_RATE_LIMIT_WINDOW` (`PT15M`). Propriedades `security.login-rate-limit.*`.

## Escolhas e limites

- **Contadores em memória**, por instância, esquecidos no reinício. É suficiente para o piloto: uma instância ativa por vez, a virada blue/green dura minutos e o atacante só ganha uma janela nova por troca. Se a API passar a ter réplicas, trocar a implementação de `LoginAttemptLimiter` (porta já existente) por uma tabela no Postgres ou Redis, sem mexer no caso de uso.
- **Bloqueio por e-mail permite negar login a uma vítima** (atacante errando a senha dela de propósito). Aceito: o bloqueio dura no máximo 15 minutos e não vale para o refresh. O limite por IP reduz o risco, e o e-mail da vítima não é desbloqueado por outro IP.
- **IP do cliente**: `server.forward-headers-strategy=native` faz o Tomcat usar o `X-Forwarded-For` enviado pelo Caddy (proxies internos confiáveis). Isso só é seguro porque o Caddy é o único serviço com porta publicada; ver o trabalho manual para conferir na VPS.
- Não limita `refresh` nem as rotas autenticadas; o rate limit geral da API ficou para a hardening da VPS (LUC-72), no Caddy.
- O código `LOGIN_RATE_LIMITED` (429) entra no catálogo de erros do LUC-50.

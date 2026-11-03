# Trabalho manual: acesso e admin (Onda 3)

Passos que o código não resolve: credenciais, painéis externos, decisões de produto e jurídicas, servidor e conferência em dispositivo. Cada item é checável.

## LUC-54: sessão em cookies HttpOnly

- [ ] Em produção, definir `ALLOWED_ORIGINS` no `.env` da VPS com a origem pública do portal (por exemplo `https://app.seudominio.com.br`), separada por vírgulas se houver mais de uma. Conferir que a origem do Swagger (a própria API) está na lista se for usado em produção. Sem isso, escritas do portal recebem `403 ORIGIN_NOT_ALLOWED`.
- [ ] Conferir que `SESSION_COOKIE_SECURE` **não** está definido (padrão `true`) em produção e que o site abre por HTTPS.
- [ ] No navegador (DevTools > Application > Cookies), depois de logar, conferir `access_token` (Path `/`) e `refresh_token` (Path `/api/v1/auth`) com `HttpOnly`, `Secure` e `SameSite=Strict`.
- [ ] Em desenvolvimento, colocar `SESSION_COOKIE_SECURE=false` no `local.env` de quem usa o app Flutter ou o `curl` por HTTP.
- [ ] Portal web e app Flutter precisam adotar o contrato de `docs/contrato-sessao-cookies.md` (fora deste repositório): remover o `localStorage`, renovar em single-flight, `cookie_jar` no Dio.
- [ ] Decisão de produto (LGPD): confirmar com quem responde pelo enquadramento que cookie estritamente necessário dispensa banner de consentimento.

## LUC-70: versão mínima do cliente

- [ ] Web e mobile precisam enviar `X-Client-Version: <plataforma>/<versão>` (contrato em `docs/contrato-versao-cliente.md`) e tratar `426 CLIENT_OUTDATED` com a tela "atualize o aplicativo".
- [ ] Para forçar atualização: definir `CLIENT_MIN_VERSION_WEB`, `CLIENT_MIN_VERSION_ANDROID` ou `CLIENT_MIN_VERSION_IOS` no `.env` da VPS e reiniciar a API (blue/green). Padrão `0.0.0` aceita tudo.
- [ ] Em dispositivo real: com o mínimo acima da versão instalada, abrir o app e conferir a tela de atualização.

## LUC-65: aceite de termos

- [ ] **Decisão jurídica/produto:** redigir os textos de Termos de Uso e Política de Privacidade, definir a base legal (LGPD) e verificar se o comitê de ética do TCC exige termo próprio. Hoje não há texto nenhum no servidor: ele só guarda a versão aceita.
- [ ] Definir o identificador de versão de cada documento e configurar `CONSENT_TERMS_VERSION` e `CONSENT_PRIVACY_VERSION` no `.env` da VPS (padrão `placeholder-1`, que **não deve ir para o piloto**). Para pedir novo aceite a todos, trocar o valor e reiniciar.
- [ ] Publicar os textos onde o portal e o app os exibem (o servidor não os serve) e implementar a tela de aceite nos dois clientes (`docs/contrato-aceite-de-termos.md`).
- [ ] Enquanto os textos não existirem, `CONSENT_ENFORCED=false` desliga o bloqueio `CONSENT_REQUIRED`; religar (`true`, padrão) antes de abrir o piloto.
- [ ] Contas do seed local não têm aceite: usar `CONSENT_ENFORCED=false` em `local.env` ou aceitar via `POST /api/v1/me/consents`.

## LUC-66 / LUC-43: outbox de e-mails

- [ ] No Resend, verificar o domínio do `MAIL_FROM` e a `RESEND_API_KEY` de produção no `.env` da VPS (sem domínio verificado, só se envia ao dono da conta).
- [ ] Depois do deploy, criar um nutricionista de teste e conferir que o e-mail chega em até 1 minuto, que a senha temporária funciona e que a troca é exigida.
- [ ] Alerta de `DEAD`: a API registra `ERROR` "Outbox email is DEAD" nos logs. A regra de alerta e o webhook do Discord entram na Onda 5 (observabilidade, LUC-69); até lá, consultar `SELECT id, type, recipient, last_error FROM email_outbox WHERE status = 'DEAD';` depois de falhas do Resend.
- [ ] Limitação conhecida: a geração automática do plano (planejamento) enfileira o e-mail logo depois de salvar o plano, em transação própria, não na mesma. Se a API cair entre os dois passos, o plano existe sem e-mail. Decisão: aceito para o piloto.
- [ ] O e-mail de acesso do paciente (criado pelo nutricionista) continua no fluxo antigo, fora da outbox (`CreateRelationshipUseCase` não muda, decisão do roadmap).

## LUC-32 / LUC-33: admin e auditoria

- [ ] **Criar o primeiro ADMIN em produção** (não há rota). No Postgres da VPS, gerar o hash bcrypt localmente (`htpasswd -bnBC 8 "" 'SENHA-FORTE' | tr -d ':\n' | sed 's/$2y/$2a/'`) e inserir:
  `INSERT INTO user_account (id, email, password_hash, role, status, password_change_required) VALUES (gen_random_uuid(), 'voce@dominio', '<hash>', 'ADMIN', 'ACTIVE', false);`
  Em seguida logar em `/api/v1/auth/login` e conferir `GET /api/v1/admin/users`.
- [ ] Remover/rotacionar a senha do admin do histórico do shell depois do passo acima.
- [ ] Clientes que ainda chamavam `POST /api/v1/identity/useraccounts` (removida) precisam parar; conferir portal e app.

## LUC-73: limitação de login

- [ ] Na VPS, conferir que o Caddy repassa `X-Forwarded-For` (comportamento padrão do `reverse_proxy`) e que a porta da API não está publicada. Teste: errar a senha 5 vezes de um aparelho e conferir `429` com `Retry-After`; de outra rede, o mesmo e-mail também fica bloqueado por 15 minutos.
- [ ] Se vários usuários saírem pelo mesmo IP (NAT de clínica/universidade), subir `LOGIN_RATE_LIMIT_MAX_PER_IP`.

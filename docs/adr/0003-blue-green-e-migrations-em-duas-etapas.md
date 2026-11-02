# Deploy blue/green com cor antiga ligada e migrations em duas etapas

Status: aceita (2026-11-02)

O deploy em produção usa duas cores da aplicação (`app-blue` e `app-green`) atrás do Caddy. A cor nova sobe, espera o `readiness` ficar verde e só então o Caddy passa a apontar para ela, com `caddy reload`. A cor antiga **continua ligada**, para que o rollback seja só voltar o upstream. Desativamos a cor antiga quando houver confiança.

## Decisões

- **Migrations em duas etapas a partir do go-live.** A cor nova roda o Flyway enquanto a antiga ainda atende o mesmo banco, então toda migration precisa ser compatível com a versão anterior. Mudança destrutiva acontece em duas releases: expandir (adicionar) e depois contrair (remover). Até o go-live as migrations destrutivas são livres, e por isso as mudanças de schema planejadas (LUC-39, LUC-40, LUC-35 e o ADR 0002) entram antes.
- **Um processo agenda cada job.** Com duas cores ligadas, os jobs agendados rodariam em duplicidade. Usamos ShedLock (JDBC): todas as cores mantêm o agendamento ligado e cada job pega um lock nomeado numa tabela `shedlock`. O job do outbox também usa `FOR UPDATE SKIP LOCKED` por linha. A propriedade `app.scheduling.enabled` fica só para testes e para o perfil `local`.
- **Compatibilidade de API em duas releases** e deploy em bloco com o portal, descritos em `docs/infraestrutura.md`.

## Alternativas descartadas

- **Desligar a cor antiga após a virada:** rollback em cerca de um minuto, mas escolhemos rollback instantâneo.
- **Ligar e desligar o agendamento por flag na virada:** um passo que pode falhar e deixar as duas cores com os jobs desligados, o que pararia a geração das 08:00 sem alerta.
- **Manter a cor antiga com os jobs ligados confiando em idempotência:** qualquer job novo sem proteção duplicaria trabalho.

## Consequências

- Duas JVMs ligadas de forma permanente. Cabem nos 8 GB da VPS KVM 2.
- Toda migration nova passa por esta pergunta: a versão anterior da aplicação continua funcionando com ela?

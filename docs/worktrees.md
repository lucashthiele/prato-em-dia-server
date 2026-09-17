# Git worktrees

O ambiente de desenvolvimento está preparado para funcionar em worktrees sem
duplicar configuração local nem disputar as portas do Docker.

## Configuração inicial

Ative os hooks versionados uma vez por clone:

```shell
git config core.hooksPath .githooks
```

Essa configuração é compartilhada por todos os worktrees do repositório. Ao
executar `git worktree add`, o hook `post-checkout` copia o `local.env` do
worktree principal quando o novo worktree ainda não possui esse arquivo. Um
arquivo existente nunca é sobrescrito.

A aplicação importa automaticamente o `local.env` da raiz do worktree como um
arquivo de propriedades do Spring Boot. Portanto, executar `ServerApplication`
diretamente pelo IntelliJ não exige o plugin EnvFile nem a configuração manual
de `JWT_SECRET`.

## Docker Compose

O `compose.yaml` possui o nome de projeto fixo `server`. Assim, todos os
worktrees reutilizam os mesmos containers de PostgreSQL e Mailpit, em vez de
criar stacks diferentes tentando publicar as mesmas portas.

Somente uma instância da aplicação deve usar a porta HTTP `8080` por vez. Para
executar duas instâncias simultaneamente, configure uma delas com outra porta,
por exemplo `SERVER_PORT=8081`.

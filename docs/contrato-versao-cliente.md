# Versão do cliente (`X-Client-Version`)

Parte da política de compatibilidade de `docs/infraestrutura.md`.

- Web e mobile enviam `X-Client-Version: <plataforma>/<versão>` em toda requisição à API (ex.: `web/1.4.2`, `android/2.0.1+45`, `ios/2.0.1`). Plataformas conhecidas: `web`, `android`, `ios`. Sufixos `-beta.1` e `+build` são ignorados na comparação.
- O servidor guarda a versão mínima por plataforma na configuração `client.min-versions.<plataforma>` (variáveis `CLIENT_MIN_VERSION_WEB`, `CLIENT_MIN_VERSION_ANDROID`, `CLIENT_MIN_VERSION_IOS`; padrão `0.0.0`, que aceita tudo).
- Abaixo do mínimo, a resposta é `426` com o envelope de erro padrão e `code = CLIENT_OUTDATED`. O cliente deve exibir a tela de "atualize o aplicativo" (web: recarregar a página).
- Cabeçalho ausente, malformado ou de plataforma sem mínimo configurado **passa**: curl, Swagger e clientes antigos continuam funcionando. O filtro vale para as rotas protegidas pela cadeia da API (não para `/v3/api-docs` nem `/swagger-ui`).
- Para forçar a atualização: subir o mínimo na `.env` da VPS e reiniciar a aplicação (ver `docs/trabalho-manual/w3-acesso-admin.md`).

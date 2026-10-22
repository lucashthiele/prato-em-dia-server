# Credenciais do seed de desenvolvimento

> Gerado por `./mvnw spring-boot:run -Dspring-boot.run.profiles=local,dev-seed`.
> Nao edite a mao: rode o seed novamente. Estas credenciais existem apenas no banco local.

Todas as contas usam a mesma senha: `Seed@12345`

| Papel | Nome | E-mail | Observacoes |
| --- | --- | --- | --- |
| ADMIN | Admin Seed | `admin@seed.pratoemdia.local` | Administrador |
| NUTRITIONIST | Ana Nutricionista | `nutri.ana@seed.pratoemdia.local` | Acompanha Carla, Diego e Fabio |
| NUTRITIONIST | Bruno Nutricionista | `nutri.bruno@seed.pratoemdia.local` | Acompanha Elisa; encerrou o vínculo com Gabi |
| PATIENT | Carla Paciente | `patient.carla@seed.pratoemdia.local` | Plano RESTRICTED; v1 arquivada, v2 publicada e v3 em rascunho; planos semanais confirmados e pendente; histórico de refeições |
| PATIENT | Diego Paciente | `patient.diego@seed.pratoemdia.local` | Plano SUBSTITUTIONS_ONLY com substituições permitidas; planos semanais confirmados; histórico de refeições |
| PATIENT | Elisa Paciente | `patient.elisa@seed.pratoemdia.local` | Plano FREE; refeições registradas sem plano semanal |
| PATIENT | Fabio Paciente | `patient.fabio@seed.pratoemdia.local` | Precisa trocar a senha no primeiro login; apenas prescrição em rascunho; e-mail do vínculo pendente |
| PATIENT | Gabi Paciente | `patient.gabi@seed.pratoemdia.local` | Conta INATIVA; vínculo encerrado; prescrição arquivada |

## Como entrar

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"patient.carla@seed.pratoemdia.local","password":"Seed@12345"}'
```

Tambem e possivel autenticar pelo Swagger UI em `http://localhost:8080/swagger-ui.html`.

## Dados criados

- Contas e perfis de administrador, nutricionistas e pacientes, com vinculos ativos, pendente e encerrado.
- Prescricoes nos modos `RESTRICTED`, `SUBSTITUTIONS_ONLY` e `FREE`, com versoes publicada, arquivada e
  rascunho, metas nutricionais, refeicoes, itens e substituicoes permitidas.
- Planos semanais (semana anterior, atual e proxima) e historico de refeicoes registradas, incluindo
  registros livres e um registro removido.
- O catalogo de alimentos nao e alterado: o seed usa alimentos que ja estao importados.

## Executar de novo

- Rodar o seed novamente nao duplica nada: se as contas ja existem, apenas este arquivo e regerado.
- Para recriar os dados do seed (e somente eles), acrescente `--devseed.reset=true`:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,dev-seed \
  -Dspring-boot.run.arguments="--devseed.reset=true"
```

package com.ulvratech.pratoemdia.server.devseed;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes the credentials doc. It has no timestamps, so rerunning the seed leaves the file unchanged. */
class CredentialsDocWriter {
    void write(Path file, String password) {
        var doc = new StringBuilder("""
                # Credenciais do seed de desenvolvimento

                > Gerado por `./mvnw spring-boot:run -Dspring-boot.run.profiles=local,dev-seed`.
                > Nao edite a mao: rode o seed novamente. Estas credenciais existem apenas no banco local.

                Todas as contas usam a mesma senha: `%s`

                | Papel | Nome | E-mail | Observacoes |
                | --- | --- | --- | --- |
                """.formatted(password));
        for (var account : SeedAccount.ALL) {
            doc.append(
                    "| %s | %s | `%s` | %s |%n".formatted(
                            account.role(), account.fullName(), account.email(), account.notes()
                    )
            );
        }
        doc.append(
                """

                        ## Como entrar

                        ```bash
                        curl -X POST http://localhost:8080/api/v1/auth/login \\
                          -H 'Content-Type: application/json' \\
                          -d '{"email":"patient.carla@seed.pratoemdia.local","password":"%s"}'
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
                        ./mvnw spring-boot:run -Dspring-boot.run.profiles=local,dev-seed \\
                          -Dspring-boot.run.arguments="--devseed.reset=true"
                        ```
                        """
                        .formatted(password)
        );
        try {
            var parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, doc.toString(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not write " + file, exception);
        }
    }
}

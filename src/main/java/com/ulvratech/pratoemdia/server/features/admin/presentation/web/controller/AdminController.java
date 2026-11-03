package com.ulvratech.pratoemdia.server.features.admin.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.admin.application.dto.AdminUserFilter;
import com.ulvratech.pratoemdia.server.features.admin.application.dto.ChangeUserStatusInput;
import com.ulvratech.pratoemdia.server.features.admin.application.dto.CreateNutritionistInput;
import com.ulvratech.pratoemdia.server.features.admin.application.dto.ListAdminUsersInput;
import com.ulvratech.pratoemdia.server.features.admin.application.port.in.ChangeUserStatus;
import com.ulvratech.pratoemdia.server.features.admin.application.port.in.CreateNutritionist;
import com.ulvratech.pratoemdia.server.features.admin.application.port.in.GetAdminUser;
import com.ulvratech.pratoemdia.server.features.admin.application.port.in.ListAdminUsers;
import com.ulvratech.pratoemdia.server.features.admin.application.port.in.ResendNutritionistAccess;
import com.ulvratech.pratoemdia.server.features.admin.presentation.web.request.ChangeUserStatusRequest;
import com.ulvratech.pratoemdia.server.features.admin.presentation.web.request.CreateNutritionistRequest;
import com.ulvratech.pratoemdia.server.features.admin.presentation.web.response.AdminUserResponse;
import com.ulvratech.pratoemdia.server.features.admin.presentation.web.response.CreateNutritionistResponse;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import com.ulvratech.pratoemdia.server.shared.openapi.OpenApiConfiguration;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/admin")
@SecurityRequirement(name = OpenApiConfiguration.COOKIE_AUTH)
@Tag(name = "Administração", description = "Gestão de contas pela administração. Somente ADMIN.")
public class AdminController {
    private final CreateNutritionist createNutritionist;
    private final ResendNutritionistAccess resendAccess;
    private final ListAdminUsers listUsers;
    private final GetAdminUser getUser;
    private final ChangeUserStatus changeStatus;

    public AdminController(
            CreateNutritionist createNutritionist, ResendNutritionistAccess resendAccess, ListAdminUsers listUsers,
            GetAdminUser getUser, ChangeUserStatus changeStatus
    ) {
        this.createNutritionist = createNutritionist;
        this.resendAccess = resendAccess;
        this.listUsers = listUsers;
        this.getUser = getUser;
        this.changeStatus = changeStatus;
    }

    @PostMapping("/nutritionists")
    @Operation(summary = "Cadastrar nutricionista", description = "Cria a conta do nutricionista. A senha temporária é gerada pelo servidor e enviada por e-mail (pela outbox, em até um minuto); ela nunca é devolvida na resposta e a troca é obrigatória no primeiro acesso.")
    ResponseEntity<CreateNutritionistResponse> createNutritionist(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateNutritionistRequest request
    ) {
        var output = createNutritionist.execute(
                new CreateNutritionistInput(id(jwt), request.email(), request.fullName(), request.registrationNumber())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(CreateNutritionistResponse.from(output));
    }

    @PostMapping("/nutritionists/{id}/access-email")
    @Operation(summary = "Reenviar acesso ao nutricionista", description = "Agenda um novo e-mail de acesso com uma nova senha temporária, que substitui as anteriores. Só vale para nutricionista ativo que ainda não trocou a senha inicial.")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void resendAccess(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        resendAccess.execute(id(jwt), id);
    }

    @GetMapping("/users")
    @Operation(summary = "Listar usuários", description = "Página de contas, da mais recente para a mais antiga. Todos os filtros são opcionais e combinam entre si; `totalElements` reflete o filtro aplicado. O estado do e-mail de acesso vem da outbox (nutricionista) ou do vínculo (paciente).")
    PageResult<AdminUserResponse> listUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Filtra pelo papel.") @RequestParam(required = false) UserAccountRole role,
            @Parameter(description = "Filtra pelo status da conta.") @RequestParam(required = false) UserAccountStatus status,
            @Parameter(description = "Busca por nome ou e-mail (sem distinção de maiúsculas).") @RequestParam(required = false) @Size(max = 180) String q
    ) {
        var result = listUsers.execute(new ListAdminUsersInput(new AdminUserFilter(role, status, q), page, size));
        return new PageResult<>(
                result.content().stream().map(AdminUserResponse::from).toList(), result.page(), result.size(),
                result.totalElements(), result.totalPages()
        );
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Consultar usuário", description = "Detalhes de uma conta, com o estado do e-mail de acesso.")
    AdminUserResponse getUser(@PathVariable UUID id) {
        return AdminUserResponse.from(getUser.execute(id));
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Alterar status da conta", description = "Ativa ou desativa uma conta. Desativar revoga os refresh tokens (o access token em curso vale até expirar). O administrador não pode alterar a própria conta.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changeStatus(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody ChangeUserStatusRequest request
    ) {
        changeStatus.execute(new ChangeUserStatusInput(id(jwt), id, UserAccountStatus.valueOf(request.status())));
    }

    private static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}

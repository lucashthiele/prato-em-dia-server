package com.ulvratech.pratoemdia.server.features.audit.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.audit.application.dto.ListAuditEventsInput;
import com.ulvratech.pratoemdia.server.features.audit.application.port.in.ListAuditEvents;
import com.ulvratech.pratoemdia.server.features.audit.domain.enums.AuditAction;
import com.ulvratech.pratoemdia.server.features.audit.presentation.web.response.AuditEventResponse;
import com.ulvratech.pratoemdia.server.shared.openapi.OpenApiConfiguration;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/v1/admin/audit-events")
@SecurityRequirement(name = OpenApiConfiguration.COOKIE_AUTH)
@Tag(name = "Auditoria (admin)", description = "Trilha das ações administrativas. Somente ADMIN.")
public class AuditEventController {
    private final ListAuditEvents listAuditEvents;

    public AuditEventController(ListAuditEvents listAuditEvents) {
        this.listAuditEvents = listAuditEvents;
    }

    @GetMapping
    @Operation(summary = "Listar eventos de auditoria", description = "Página dos eventos administrativos (criação de nutricionista, reenvio de acesso e mudança de status), do mais recente para o mais antigo. Todos os filtros são opcionais e combinam entre si.")
    PageResult<AuditEventResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @Parameter(description = "Tipo da ação.") @RequestParam(required = false) AuditAction action,
            @Parameter(description = "Conta do administrador que executou a ação.") @RequestParam(required = false) UUID actorId,
            @Parameter(description = "Conta afetada pela ação.") @RequestParam(required = false) UUID targetId,
            @Parameter(description = "Início do período (ISO-8601, inclusivo).") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @Parameter(description = "Fim do período (ISO-8601, inclusivo).") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        var result = listAuditEvents.execute(new ListAuditEventsInput(action, actorId, targetId, from, to, page, size));
        return new PageResult<>(
                result.content().stream().map(AuditEventResponse::from).toList(), result.page(), result.size(),
                result.totalElements(), result.totalPages()
        );
    }
}

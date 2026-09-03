package com.ulvratech.pratoemdia.server.features.identity.presentation.web.controller;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.CreateUserAccountInput;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.CreateUserAccount;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.presentation.web.request.CreateUserAccountRequest;
import com.ulvratech.pratoemdia.server.features.identity.presentation.web.response.CreateUserAccountResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/identity/useraccounts")
@Slf4j
public class UserAccountController {
    private final CreateUserAccount createUserAccountCommand;

    public UserAccountController(CreateUserAccount createUserAccountCommand) {
        this.createUserAccountCommand = createUserAccountCommand;
    }

    @PostMapping
    ResponseEntity<CreateUserAccountResponse> create(
        @Valid @RequestBody CreateUserAccountRequest request
    ) {
        var commandInput = new CreateUserAccountInput(
            request.email(),
            request.password(),
            UserAccountRole.valueOf(request.role()),
            request.fullName()
        );

        var output = createUserAccountCommand.execute(commandInput);

        var response = new CreateUserAccountResponse(
            output.id(),
            output.email(),
            output.role().name(),
            output.status().name()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

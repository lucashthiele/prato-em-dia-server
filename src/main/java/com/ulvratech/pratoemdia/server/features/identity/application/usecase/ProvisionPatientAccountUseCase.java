package com.ulvratech.pratoemdia.server.features.identity.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.ProvisionPatientAccountInput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.ProvisionPatientAccountOutput;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.PatientAccountConflictException;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.ProvisionPatientAccount;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.InitialPasswordGenerator;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.PasswordHasher;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.UserProfileInitializer;
import com.ulvratech.pratoemdia.server.features.identity.domain.entity.UserAccount;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.UserAccountRepository;
import jakarta.transaction.Transactional;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ProvisionPatientAccountUseCase implements ProvisionPatientAccount {
  private final UserAccountRepository accounts;
  private final PasswordHasher hasher;
  private final InitialPasswordGenerator passwords;
  private final UserProfileInitializer profiles;

  public ProvisionPatientAccountUseCase(
      UserAccountRepository accounts,
      PasswordHasher hasher,
      InitialPasswordGenerator passwords,
      UserProfileInitializer profiles) {
    this.accounts = accounts;
    this.hasher = hasher;
    this.passwords = passwords;
    this.profiles = profiles;
  }

  public ProvisionPatientAccountOutput execute(ProvisionPatientAccountInput input) {
    var email = input.email().strip().toLowerCase(Locale.ROOT);
    var existing = accounts.findByEmailIgnoreCase(email);
    if (existing.isPresent()) {
      var account = existing.get();
      if (account.getRole() != UserAccountRole.PATIENT
          || account.getStatus() != UserAccountStatus.ACTIVE)
        throw new PatientAccountConflictException();
      return new ProvisionPatientAccountOutput(
          account.getId(), account.getEmail(), false, account.isPasswordChangeRequired(), null);
    }
    var raw = passwords.generate();
    var saved =
        accounts.save(
            new UserAccount(
                email, hasher.hash(raw), UserAccountRole.PATIENT, UserAccountStatus.ACTIVE, true));
    profiles.initialize(saved.getId(), UserAccountRole.PATIENT, input.fullName().strip());
    return new ProvisionPatientAccountOutput(saved.getId(), saved.getEmail(), true, true, raw);
  }
}

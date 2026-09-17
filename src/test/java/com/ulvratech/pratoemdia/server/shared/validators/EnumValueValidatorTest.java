package com.ulvratech.pratoemdia.server.shared.validators;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.shared.annotations.EnumValue;
import org.junit.jupiter.api.Test;

class EnumValueValidatorTest {
  @Test
  void acceptsKnownEnumsAndLetsRequiredValidatorsHandleBlankValues() {
    var annotation = mock(EnumValue.class);
    when(annotation.enumClass()).thenReturn((Class) UserAccountRole.class);
    var validator = new EnumValueValidator();
    validator.initialize(annotation);

    assertTrue(validator.isValid("ADMIN", null));
    assertFalse(validator.isValid("admin", null));
    assertFalse(validator.isValid("OTHER", null));
    assertTrue(validator.isValid(null, null));
    assertTrue(validator.isValid("  ", null));
  }
}

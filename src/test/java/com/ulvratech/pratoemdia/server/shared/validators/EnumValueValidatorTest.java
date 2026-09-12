package com.ulvratech.pratoemdia.server.shared.validators;

import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.shared.annotations.EnumValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

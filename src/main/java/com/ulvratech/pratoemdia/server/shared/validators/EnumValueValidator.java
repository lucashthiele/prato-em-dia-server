package com.ulvratech.pratoemdia.server.shared.validators;

import com.ulvratech.pratoemdia.server.shared.annotations.EnumValue;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumValueValidator
    implements ConstraintValidator<EnumValue, String> {

    private Set<String> acceptedValues;

    @Override
    public void initialize(EnumValue annotation) {
        acceptedValues = Arrays.stream(
                annotation.enumClass().getEnumConstants()
            )
            .map(Enum::name)
            .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(
        String value,
        ConstraintValidatorContext context
    ) {
        if (value == null || value.isBlank()) {
            return true;
        }

        return acceptedValues.contains(value);
    }
}

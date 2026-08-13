package com.ulvratech.pratoemdia.server.shared.application.usecase;

public interface UseCase<I, O> {
    O execute(I input);
}

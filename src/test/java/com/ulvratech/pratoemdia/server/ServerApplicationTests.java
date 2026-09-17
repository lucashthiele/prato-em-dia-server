package com.ulvratech.pratoemdia.server;

import org.junit.jupiter.api.Test;

class ServerApplicationTests {

  @Test
  void applicationEntryPointIsAvailable() {
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(
        () -> ServerApplication.class.getDeclaredMethod("main", String[].class));
  }
}

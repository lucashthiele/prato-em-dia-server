package com.ulvratech.pratoemdia.server.devseed;

import java.util.List;

record SeedAccount(
        String key,
        String email,
        String role,
        String fullName,
        boolean active,
        boolean passwordChangeRequired,
        String notes) {
    static final String EMAIL_DOMAIN = "@seed.pratoemdia.local";

    static final SeedAccount ADMIN = new SeedAccount(
            "admin", "admin" + EMAIL_DOMAIN, "ADMIN", "Admin Seed", true, false, "Administrador"
    );
    static final SeedAccount ANA = new SeedAccount(
            "ana", "nutri.ana" + EMAIL_DOMAIN, "NUTRITIONIST", "Ana Nutricionista", true, false,
            "Acompanha Carla, Diego e Fabio"
    );
    static final SeedAccount BRUNO = new SeedAccount(
            "bruno", "nutri.bruno" + EMAIL_DOMAIN, "NUTRITIONIST", "Bruno Nutricionista", true, false,
            "Acompanha Elisa; encerrou o vínculo com Gabi"
    );
    static final SeedAccount CARLA = new SeedAccount(
            "carla", "patient.carla" + EMAIL_DOMAIN, "PATIENT", "Carla Paciente", true, false,
            "Plano RESTRICTED; v1 arquivada, v2 publicada e v3 em rascunho; planos semanais confirmados e pendente; "
                    + "histórico de refeições"
    );
    static final SeedAccount DIEGO = new SeedAccount(
            "diego", "patient.diego" + EMAIL_DOMAIN, "PATIENT", "Diego Paciente", true, false,
            "Plano SUBSTITUTIONS_ONLY com substituições permitidas; planos semanais confirmados; histórico de refeições"
    );
    static final SeedAccount ELISA = new SeedAccount(
            "elisa", "patient.elisa" + EMAIL_DOMAIN, "PATIENT", "Elisa Paciente", true, false,
            "Plano FREE; refeições registradas sem plano semanal"
    );
    static final SeedAccount FABIO = new SeedAccount(
            "fabio", "patient.fabio" + EMAIL_DOMAIN, "PATIENT", "Fabio Paciente", true, true,
            "Precisa trocar a senha no primeiro login; apenas prescrição em rascunho; e-mail do vínculo pendente"
    );
    static final SeedAccount GABI = new SeedAccount(
            "gabi", "patient.gabi" + EMAIL_DOMAIN, "PATIENT", "Gabi Paciente", false, false,
            "Conta INATIVA; vínculo encerrado; prescrição arquivada"
    );

    static final List<SeedAccount> ALL = List.of(ADMIN, ANA, BRUNO, CARLA, DIEGO, ELISA, FABIO, GABI);

    boolean isPatient() {
        return role.equals("PATIENT");
    }

    boolean isNutritionist() {
        return role.equals("NUTRITIONIST");
    }

    java.util.UUID accountId() {
        return SeedIds.of("account:" + key);
    }

    java.util.UUID profileId() {
        return SeedIds.of("profile:" + key);
    }
}

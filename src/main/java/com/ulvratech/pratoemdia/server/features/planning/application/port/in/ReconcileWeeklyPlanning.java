package com.ulvratech.pratoemdia.server.features.planning.application.port.in;

public interface ReconcileWeeklyPlanning {
    void generateDuePlans();

    void confirmDuePlans();
}

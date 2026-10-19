package com.ulvratech.pratoemdia.server.features.planning.application.port.out;

import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanGeneratedNotice;

public interface PlanGeneratedNotifier {
    void planGenerated(PlanGeneratedNotice notice);
}

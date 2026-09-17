package com.ulvratech.pratoemdia.server.features.planning.application.port.in;

import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.Actor;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.PlanQuery;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.PlanUpdate;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.PreferenceUpdate;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.WeeklyPlanOutput;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.WeeklyPlanningPreferenceOutput;

public interface PlanningCommands {
    WeeklyPlanningPreferenceOutput updatePreference(PreferenceUpdate input);

    WeeklyPlanOutput generate(Actor actor);

    WeeklyPlanOutput update(PlanUpdate input);

    WeeklyPlanOutput confirm(PlanQuery input);
}

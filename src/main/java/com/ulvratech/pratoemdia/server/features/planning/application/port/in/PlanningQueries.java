package com.ulvratech.pratoemdia.server.features.planning.application.port.in;

import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.Actor;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.PlanQuery;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.WeeklyPlanOutput;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.WeeklyPlanningPreferenceOutput;
import java.util.List;

public interface PlanningQueries {
  WeeklyPlanningPreferenceOutput preference(Actor actor);

  List<WeeklyPlanOutput> list(Actor actor);

  WeeklyPlanOutput get(PlanQuery input);
}

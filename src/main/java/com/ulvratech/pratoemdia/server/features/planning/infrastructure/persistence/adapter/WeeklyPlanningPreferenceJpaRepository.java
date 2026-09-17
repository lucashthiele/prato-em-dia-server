package com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.model.WeeklyPlanningPreferenceModel;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

interface WeeklyPlanningPreferenceJpaRepository
    extends JpaRepository<WeeklyPlanningPreferenceModel, UUID> {
  List<WeeklyPlanningPreferenceModel> findAllByEnabledTrueAndDayOfWeek(int dayOfWeek);
}

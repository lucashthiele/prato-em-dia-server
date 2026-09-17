package com.ulvratech.pratoemdia.server;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(
    packages = ArchitectureTest.BASE_PACKAGE,
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
  static final String BASE_PACKAGE = "com.ulvratech.pratoemdia.server";

  @ArchTest
  static final ArchRule ports_are_interfaces =
      classes()
          .that()
          .resideInAnyPackage(
              "..features.*.application.port.in..",
              "..features.*.application.port.out..",
              "..features.*.domain.port.out..")
          .should()
          .beInterfaces();

  @ArchTest
  static final ArchRule use_cases_implement_inbound_ports =
      classes()
          .that()
          .resideInAPackage("..features.*.application.usecase..")
          .should()
          .implement(resideInAPackage("..features.*.application.port.in.."));

  @ArchTest
  static final ArchRule persistence_models_stay_in_infrastructure =
      classes()
          .that()
          .areAnnotatedWith(Entity.class)
          .should()
          .resideInAPackage("..infrastructure.persistence.model..");

  @ArchTest
  static final ArchRule spring_data_repositories_stay_in_infrastructure =
      classes()
          .that()
          .areAssignableTo(JpaRepository.class)
          .should()
          .resideInAPackage("..infrastructure.persistence.adapter..");

  @ArchTest
  static final ArchRule repository_adapters_stay_in_infrastructure =
      classes()
          .that()
          .haveSimpleNameEndingWith("RepositoryAdapter")
          .should()
          .resideInAPackage("..infrastructure.persistence.adapter..");

  @ArchTest
  static final ArchRule repository_adapters_implement_repository_ports =
      classes()
          .that()
          .haveSimpleNameEndingWith("RepositoryAdapter")
          .should()
          .implement(resideInAPackage("..features.*.domain.port.out.."));

  @ArchTest
  static final ArchRule features_are_free_of_cycles =
      slices().matching(BASE_PACKAGE + ".features.(*)..").should().beFreeOfCycles();

  private static final String FEATURES = BASE_PACKAGE + ".features..";

  @ArchTest
  static final ArchRule features_do_not_access_another_features_repositories =
      classes().that().resideInAPackage(FEATURES).should(notAccessRepositoriesFromAnotherFeature());

  private static final String DOMAIN = "..features.*.domain..";
  private static final String APPLICATION = "..features.*.application..";
  private static final String INFRASTRUCTURE = "..features.*.infrastructure..";
  private static final String PRESENTATION = "..features.*.presentation..";

  @ArchTest
  static final ArchRule domain_is_independent_from_outer_layers =
      noClasses()
          .that()
          .resideInAPackage(DOMAIN)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(APPLICATION, INFRASTRUCTURE, PRESENTATION);

  @ArchTest
  static final ArchRule application_does_not_depend_on_adapters_or_presentation =
      noClasses()
          .that()
          .resideInAPackage(APPLICATION)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(INFRASTRUCTURE, PRESENTATION);

  @ArchTest
  static final ArchRule infrastructure_does_not_depend_on_presentation =
      noClasses()
          .that()
          .resideInAPackage(INFRASTRUCTURE)
          .should()
          .dependOnClassesThat()
          .resideInAPackage(PRESENTATION);

  private static final String CONTROLLERS = "..features.*.presentation..controller..";

  @ArchTest
  static final ArchRule controllers_do_not_bypass_inbound_ports =
      noClasses()
          .that()
          .areAnnotatedWith(RestController.class)
          .or()
          .resideInAPackage(CONTROLLERS)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..features.*.application.usecase..",
              "..features.*.application.port.out..",
              "..features.*.domain.port.out..",
              INFRASTRUCTURE);

  @ArchTest
  static final ArchRule controllers_do_not_access_repositories =
      noClasses()
          .that()
          .areAnnotatedWith(RestController.class)
          .or()
          .resideInAPackage(CONTROLLERS)
          .should()
          .dependOnClassesThat()
          .haveSimpleNameEndingWith("Repository");

  private static ArchCondition<JavaClass> notAccessRepositoriesFromAnotherFeature() {
    return new ArchCondition<>("not access repositories from another feature") {
      @Override
      public void check(JavaClass source, ConditionEvents events) {
        String sourceFeature = featureName(source).orElseThrow();

        source.getDirectDependenciesFromSelf().stream()
            .filter(ArchitectureTest::targetsRepository)
            .filter(
                dependency ->
                    featureName(dependency.getTargetClass())
                        .filter(targetFeature -> !targetFeature.equals(sourceFeature))
                        .isPresent())
            .forEach(
                dependency ->
                    events.add(SimpleConditionEvent.violated(source, dependency.getDescription())));
      }
    };
  }

  private static boolean targetsRepository(Dependency dependency) {
    JavaClass target = dependency.getTargetClass();
    String packageName = target.getPackageName();

    return target.getSimpleName().contains("Repository")
        && (packageName.contains(".domain.port.out")
            || packageName.contains(".application.port.out")
            || packageName.contains(".infrastructure.persistence.adapter"));
  }

  private static Optional<String> featureName(JavaClass javaClass) {
    String[] packageParts = javaClass.getPackageName().split("\\.");

    for (int index = 0; index < packageParts.length - 1; index++) {
      if (packageParts[index].equals("features")) {
        return Optional.of(packageParts[index + 1]);
      }
    }

    return Optional.empty();
  }
}

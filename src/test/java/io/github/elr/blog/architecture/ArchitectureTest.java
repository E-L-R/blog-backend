package io.github.elr.blog.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

/**
 * Architecture rules, enforced on every build (issue #10). A failing rule prints exactly which class
 * and which dependency broke it.
 */
@AnalyzeClasses(packages = "io.github.elr.blog", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String[] FEATURES = {
        "..blog.auth..", "..blog.user..", "..blog.post..", "..blog.tag..", "..blog.comment..", "..blog.tldr.."
    };

    @ArchTest
    static final ArchRule features_have_no_cycles =
            slices().matching("io.github.elr.blog.(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule common_does_not_depend_on_features = noClasses()
            .that()
            .resideInAPackage("..blog.common..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(FEATURES);

    @ArchTest
    static final ArchRule controllers_do_not_use_repositories = noClasses()
            .that()
            .areAnnotatedWith(RestController.class)
            .should()
            .dependOnClassesThat()
            .areAssignableTo(Repository.class)
            .allowEmptyShould(true); // no controllers yet in M1; the rule waits for them

    @ArchTest
    static final ArchRule controllers_do_not_return_entities = methods()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(RestController.class)
            .should(notExposeEntities())
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule no_transactions_in_controllers = noMethods()
            .that()
            .areDeclaredInClassesThat()
            .areAnnotatedWith(RestController.class)
            .should()
            .beAnnotatedWith(Transactional.class)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule use_springs_transactional_annotation = noClasses()
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("jakarta.transaction.Transactional")
            .because("Spring's @Transactional supports readOnly, propagation and rollback rules consistently");

    @ArchTest
    static final ArchRule no_field_injection = NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

    @ArchTest
    static final ArchRule no_standard_streams = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    @ArchTest
    static final ArchRule no_java_util_logging = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    /** Fails for return types like Post, ResponseEntity<Post>, List<Post>, Page<Post>, ... */
    private static ArchCondition<JavaMethod> notExposeEntities() {
        return new ArchCondition<>("not return JPA entities (also inside generics)") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                for (JavaClass type : method.getReturnType().getAllInvolvedRawTypes()) {
                    if (type.isAnnotatedWith(Entity.class)) {
                        events.add(SimpleConditionEvent.violated(
                                method, method.getFullName() + " exposes entity " + type.getSimpleName()));
                    }
                }
            }
        };
    }
}

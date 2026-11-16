package io.github.gabrielivo.oficina.billing.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class CamadasArchUnitTest {

    private final JavaClasses classes = new ClassFileImporter().importPackages("io.github.gabrielivo.oficina.billing");

    @Test
    void dominioNaoDependeDasCamadasExternas() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..application..", "..infrastructure..", "..presentation..");
        rule.check(classes);
    }

    @Test
    void applicationNaoDependeDeInfrastructureNemPresentation() {
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("..application..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..infrastructure..", "..presentation..");
        rule.check(classes);
    }
}

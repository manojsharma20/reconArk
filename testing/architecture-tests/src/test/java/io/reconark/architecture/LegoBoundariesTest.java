package io.reconark.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import io.reconark.kernel.api.ReconArkPlugin;

/** The rules that make bricks replaceable (HLD §3, ADR-0026). */
@AnalyzeClasses(packages = "io.reconark", importOptions = ImportOption.DoNotIncludeTests.class)
class LegoBoundariesTest {

    @ArchTest
    static final ArchRule kernelIsFrameworkFreeAndDomainAgnostic = noClasses()
            .that().resideInAPackage("io.reconark.kernel..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "io.reconark.domain..", "io.reconark.spi..", "io.reconark.plugins..")
            .because("the kernel is the baseplate: it knows extension points, never implementations or frameworks");

    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses()
            .that().resideInAnyPackage("io.reconark.domain..", "io.reconark.spi..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta..", "io.reconark.plugins..")
            .because("engines depend on SPI contracts only (ADR-0001, ADR-0026)");

    @ArchTest
    static final ArchRule pluginsNeverDependOnEachOther = slices()
            .matching("io.reconark.plugins.(*)..")
            .should().notDependOnEachOther()
            .because("a brick must be removable without breaking another brick");

    @ArchTest
    static final ArchRule pluginsDoNotUseFrameworks = noClasses()
            .that().resideInAPackage("io.reconark.plugins..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
            .because("plugins run in any host: services, tests, the remote plugin host");

    @ArchTest
    static final ArchRule pluginEntryPointsAreFinal = classes()
            .that().implement(ReconArkPlugin.class)
            .should().haveModifier(com.tngtech.archunit.core.domain.JavaModifier.FINAL)
            .andShould().bePublic()
            .because("plugins are discovered by ServiceLoader and must not be subclassed");

    @ArchTest
    static final ArchRule onlyTheKafkaPluginTouchesKafka = noClasses()
            .that().resideOutsideOfPackage("io.reconark.plugins.bus.kafka..")
            .should().dependOnClassesThat().resideInAPackage("org.apache.kafka..")
            .because("only the message-bus extension touches broker clients (ADR-0007)");

    @ArchTest
    static final ArchRule noLegacyNames = noClasses()
            .should().haveSimpleNameContaining("Loyalty")
            .because("clean-room naming (ADR-0025); extend the deny-list in scripts/naming-denylist.txt");
}

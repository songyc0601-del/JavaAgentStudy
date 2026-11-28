package dev.javaagentstudy.stage01;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

/**
 * 架构守卫测试：自动防止 stage-01 内部子包出现循环依赖，并确保公共 web 包保持单向被依赖。
 */
@AnalyzeClasses(packages = "dev.javaagentstudy.stage01", importOptions = ImportOption.DoNotIncludeTests.class)
class Stage01ArchitectureTest {

    @ArchTest
    static final ArchRule subpackagesShouldBeFreeOfCycles =
            SlicesRuleDefinition.slices()
                    .matching("dev.javaagentstudy.stage01.(*)..")
                    .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule webPackageShouldNotDependOnFeaturePackages =
            noClasses()
                    .that().resideInAPackage("..stage01.web..")
                    .should().dependOnClassesThat().resideInAnyPackage("..stage01.explain..", "..stage01.exercise..");
}

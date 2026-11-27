package dev.javaagentstudy.common.testing;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * 守卫测试：确认默认构建真的排除了带 live-llm 标签的测试。
 *
 * <p>这个测试故意写成“一旦运行就失败”。
 * 如果 `mvn test` 通过，就说明它被排除了，排除配置有效；
 * 如果有人改坏了父 pom 里的 surefire.excludedGroups，`mvn test` 会立刻变红。
 *
 * <p>需要真实调用大模型时，用 -Dsurefire.excludedGroups= 覆盖排除配置，
 * 但不要在那种情况下运行这个守卫测试。
 */
class LiveLlmExclusionGuardTest {

    @Test
    @Tag("live-llm")
    void mustNotRunInDefaultBuild() {
        fail("带 live-llm 标签的测试不应该在默认 mvn test 中运行，请检查 surefire.excludedGroups 配置");
    }
}

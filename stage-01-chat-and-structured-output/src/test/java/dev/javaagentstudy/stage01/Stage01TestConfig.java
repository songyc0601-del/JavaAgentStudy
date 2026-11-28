package dev.javaagentstudy.stage01;

import dev.javaagentstudy.common.testing.SwitchableChatModel;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * stage-01 离线 MockMvc 集成测试共享的测试配置。
 *
 * <p>用单一 {@link SwitchableChatModel} Bean 替代最外层 {@code ChatModel}，
 * 让所有离线测试复用同一个 Spring 上下文，避免重复拉起容器与重复编写 {@code @TestConfiguration} 样板。
 */
@TestConfiguration
public class Stage01TestConfig {

    @Bean
    @Primary
    public SwitchableChatModel switchableChatModel() {
        return new SwitchableChatModel();
    }
}

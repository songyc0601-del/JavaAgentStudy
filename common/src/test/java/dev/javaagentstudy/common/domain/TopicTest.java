package dev.javaagentstudy.common.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Topic 领域类型的行为测试。
 */
class TopicTest {

    @Test
    void keepsTheGivenName() {
        assertThat(new Topic("集合框架").name()).isEqualTo("集合框架");
    }

    @Test
    void rejectsBlankName() {
        // 空白名称不是一个有效的 Topic，应当在创建时就被拒绝。
        assertThatThrownBy(() -> new Topic("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

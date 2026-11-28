package dev.javaagentstudy.common.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * FakeChatModel 模拟下游异常的行为测试（接缝 2：测试替身本身是否可靠）。
 *
 * <p>stage-01 的“模型异常时返回清晰错误”测试需要一个会抛异常的假模型，
 * 所以要先确认替身能按要求抛出异常。
 */
class FakeChatModelFailureTest {

    @Test
    void throwsPresetExceptionWhenConfiguredToFail() {
        // 准备：创建一个调用时会抛出指定异常的假模型
        IllegalStateException failure = new IllegalStateException("model is down");
        FakeChatModel fakeModel = FakeChatModel.failingWith(failure);

        // 执行并验证：调用时抛出的正是我们预设的那个异常
        assertThatThrownBy(() -> fakeModel.call(new Prompt("any question")))
                .isSameAs(failure);

        // 补充验证：即使抛出异常，这次调用也已被记录，说明记录行为不依赖调用是否成功
        assertThat(fakeModel.recordedPrompts()).hasSize(1);
    }

    @Test
    void failsStreamWithPresetExceptionWhenConfiguredToFail() {
        IllegalStateException failure = new IllegalStateException("model is down");
        FakeChatModel fakeModel = FakeChatModel.failingWith(failure);

        // failingWith 对流式调用同样生效，不能静默返回空流。
        assertThatThrownBy(() -> fakeModel.stream(new Prompt("any question")).blockLast())
                .isSameAs(failure);
        assertThat(fakeModel.recordedPrompts()).hasSize(1);
    }
}

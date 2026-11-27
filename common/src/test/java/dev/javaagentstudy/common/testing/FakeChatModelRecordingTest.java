package dev.javaagentstudy.common.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * FakeChatModel 记录请求的行为测试（接缝 2：测试替身本身是否可靠）。
 *
 * <p>stage-01 需要断言“讲解提示词是否面向初级 Learner”，
 * 这要求假模型能把收到的请求保存下来，供测试读取。
 */
class FakeChatModelRecordingTest {

    @Test
    void recordsPromptsSentToTheModel() {
        // 准备：创建一个普通的假模型
        FakeChatModel fakeModel = FakeChatModel.replyingWith("hello");

        // 执行：连续发送两次请求
        fakeModel.call(new Prompt("first question"));
        fakeModel.call(new Prompt("second question"));

        // 验证：两次请求都按发送顺序被记录下来，且内容与发送时一致
        assertThat(fakeModel.recordedPrompts())
                .extracting(Prompt::getContents)
                .containsExactly("first question", "second question");
    }

    @Test
    void recordsPromptEvenWhenTheModelFails() {
        // 准备：创建一个调用时会抛异常的假模型
        FakeChatModel fakeModel = FakeChatModel.failingWith(new IllegalStateException("model is down"));

        // 执行并验证：调用必须抛出异常；如果没有抛出，测试会失败，而不是悄悄通过
        assertThatThrownBy(() -> fakeModel.call(new Prompt("question")))
                .isInstanceOf(IllegalStateException.class);

        // 验证：即使模型失败，这次请求也已被记录（便于确认“确实调用过模型”）
        assertThat(fakeModel.recordedPrompts()).hasSize(1);
    }

    @Test
    void clearRemovesAllRecordedPrompts() {
        // 准备：先发送一次请求，让假模型里有一条记录
        FakeChatModel fakeModel = FakeChatModel.replyingWith("hello");
        fakeModel.call(new Prompt("question"));

        // 执行：清空记录
        fakeModel.clearRecordedPrompts();

        // 验证：记录被清空，之后的测试不会看到上一个测试留下的请求
        assertThat(fakeModel.recordedPrompts()).isEmpty();
    }
}

package dev.javaagentstudy.common.testing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * FakeChatModel 的行为测试（接缝 2：测试替身本身是否可靠）。
 *
 * <p>为什么要测“测试替身”？因为后面所有离线集成测试都依赖它，
 * 如果替身本身有问题，上层测试的结果就不可信。
 */
class FakeChatModelTest {

    @Test
    void returnsPresetTextReply() {
        // 准备：创建一个预设回复为 "hello" 的假模型
        FakeChatModel fakeModel = FakeChatModel.replyingWith("hello");

        // 执行：像调用真实模型一样调用它
        String reply = fakeModel.call(new Prompt("any question"))
                .getResult()
                .getOutput()
                .getText();

        // 验证：拿到的就是预设的文本，没有访问任何网络
        assertThat(reply).isEqualTo("hello");
    }
}

package dev.javaagentstudy.common.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * SwitchableChatModel 的委托与切换行为测试。
 */
class SwitchableChatModelTest {

    @Test
    void delegatesCallAndStreamToCurrentFakeModel() {
        SwitchableChatModel switchable = new SwitchableChatModel();

        switchable.use(FakeChatModel.replyingWith("同步回复"));
        assertThat(switchable.call(new Prompt("q1")).getResult().getOutput().getText())
                .isEqualTo("同步回复");
        assertThat(switchable.recordedPrompts()).hasSize(1);

        switchable.use(FakeChatModel.streamingChunks("片段一", "片段二"));
        List<String> chunks = switchable.stream(new Prompt("q2"))
                .map(ChatResponse::getResult)
                .map(gen -> gen.getOutput().getText())
                .collectList()
                .block();
        assertThat(chunks).containsExactly("片段一", "片段二");
        assertThat(switchable.recordedPrompts()).hasSize(1);
    }

    @Test
    void rejectsNullDelegate() {
        SwitchableChatModel switchable = new SwitchableChatModel();
        assertThatThrownBy(() -> switchable.use(null))
                .isInstanceOf(NullPointerException.class);
    }
}

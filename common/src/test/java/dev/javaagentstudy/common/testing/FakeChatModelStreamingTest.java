package dev.javaagentstudy.common.testing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * FakeChatModel 流式回放的行为测试（接缝 2：测试替身本身是否可靠）。
 *
 * <p>stage-01 的 SSE 流式讲解测试依赖替身按顺序回放预设分片，
 * 所以先确认替身的 stream(...) 能按预设顺序逐段输出。
 */
class FakeChatModelStreamingTest {

    @Test
    void streamsPresetChunksInOrder() {
        // 准备：创建一个预设三段分片的假模型
        FakeChatModel fakeModel = FakeChatModel.streamingChunks("集合", "框架", "入门");

        // 执行：像调用真实模型一样消费流
        List<String> chunks = fakeModel.stream(new Prompt("any question"))
                .map(ChatResponse::getResult)
                .map(generation -> generation.getOutput().getText())
                .collectList()
                .block();

        // 验证：分片顺序与预设一致，并且这次调用已被记录
        assertThat(chunks).containsExactly("集合", "框架", "入门");
        assertThat(fakeModel.recordedPrompts()).hasSize(1);
    }
}

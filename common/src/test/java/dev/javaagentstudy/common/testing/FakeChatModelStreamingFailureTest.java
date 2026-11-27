package dev.javaagentstudy.common.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * FakeChatModel 流式异常场景的行为测试（接缝 2：测试替身本身是否可靠）。
 *
 * <p>stage-01 的 SSE 流式讲解需要验证“流中途出错”的表现，
 * 所以替身要能先回放若干分片，再以预设异常终止流。
 */
class FakeChatModelStreamingFailureTest {

    @Test
    void emitsChunksThenFailsWithPresetException() {
        // 准备：先输出两段分片，然后在流中途失败
        IllegalStateException failure = new IllegalStateException("stream broke");
        FakeChatModel fakeModel = FakeChatModel.streamingChunksThenFailing(failure, "集合", "框架");

        // 执行：消费流，收集出错之前收到的分片
        List<String> received = new ArrayList<>();
        // 这里直接用 blockLast() 消费而不引入 reactor-test 依赖；
        // IllegalStateException 是运行时异常，Reactor 不会包装，调用方拿到的就是预设的同一个对象。
        assertThatThrownBy(() -> fakeModel.stream(new Prompt("any question"))
                .map(ChatResponse::getResult)
                .map(generation -> generation.getOutput().getText())
                .doOnNext(received::add)
                .blockLast())
                // 验证：流以预设的同一个异常终止
                .isSameAs(failure);

        // 补充验证：出错前的分片已按顺序送达，且这次调用已被记录
        assertThat(received).containsExactly("集合", "框架");
        assertThat(fakeModel.recordedPrompts()).hasSize(1);
    }
}

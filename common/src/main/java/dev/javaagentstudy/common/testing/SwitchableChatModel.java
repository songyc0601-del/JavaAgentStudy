package dev.javaagentstudy.common.testing;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import reactor.core.publisher.Flux;

/**
 * 可按测试场景动态切换底层 {@link FakeChatModel} 的共享测试替身。
 *
 * <p>各阶段模块只需声明一份公共 {@code @TestConfiguration} 注册本 Bean，
 * 多个离线集成测试即可复用同一个 Spring Boot 上下文，避免为不同假回复反复重启容器。
 */
public class SwitchableChatModel implements ChatModel {

    private final AtomicReference<FakeChatModel> delegate;

    public SwitchableChatModel() {
        this(FakeChatModel.replyingWith("ok"));
    }

    public SwitchableChatModel(FakeChatModel initial) {
        this.delegate = new AtomicReference<>(Objects.requireNonNull(initial, "initial FakeChatModel must not be null"));
    }

    /** 切换当前测试使用的底层假模型（通常在 {@code @BeforeEach} 或测试方法开头调用）。 */
    public void use(FakeChatModel model) {
        delegate.set(Objects.requireNonNull(model, "FakeChatModel must not be null"));
    }

    /** 返回当前生效的底层假模型实例。 */
    public FakeChatModel current() {
        return delegate.get();
    }

    /** 便捷读取当前假模型记录的 Prompt 列表。 */
    public List<Prompt> recordedPrompts() {
        return delegate.get().recordedPrompts();
    }

    /** 清空当前假模型记录的 Prompt。 */
    public void clearRecordedPrompts() {
        delegate.get().clearRecordedPrompts();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        return delegate.get().call(prompt);
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return delegate.get().stream(prompt);
    }
}

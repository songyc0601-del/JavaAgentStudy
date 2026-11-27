package dev.javaagentstudy.common.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * 离线测试用的假 {@link ChatModel}。
 *
 * <p>真实的 ChatModel 会通过网络调用大模型，既要 API Key 又要花钱。
 * 这个类实现了同一个接口，但只返回我们预先设置好的文本，
 * 所以测试可以在没有网络、没有 Key、零费用的情况下运行。
 */
public class FakeChatModel implements ChatModel {

    /** 每次调用都会返回的预设文本。 */
    private final String presetReply;

    /** 如果不为 null，每次调用都会抛出这个异常，用来模拟下游模型故障。 */
    private final RuntimeException presetFailure;

    /**
     * 按调用顺序保存收到的每一个 Prompt，供测试断言“发给模型的内容是什么”。
     * 使用线程安全的包装，因为 Web 请求可能在不同线程里调用模型。
     */
    private final List<Prompt> recordedPrompts = Collections.synchronizedList(new ArrayList<>());

    // 构造方法设为 private，外部统一通过 replyingWith(...) 或 failingWith(...) 创建，读起来更像一句话。
    private FakeChatModel(String presetReply, RuntimeException presetFailure) {
        this.presetReply = presetReply;
        this.presetFailure = presetFailure;
    }

    /**
     * 创建一个总是返回指定文本的假模型。
     *
     * @param reply 假模型每次要返回的文本
     */
    public static FakeChatModel replyingWith(String reply) {
        // 第二个参数传 null，表示这个假模型不会抛异常。
        return new FakeChatModel(reply, null);
    }

    /**
     * 创建一个每次调用都抛出指定异常的假模型，用来模拟“下游大模型出错”。
     *
     * @param failure 调用时要抛出的异常（调用方拿到的就是同一个对象）
     */
    public static FakeChatModel failingWith(RuntimeException failure) {
        // 第一个参数传 null，因为这个假模型永远不会返回文本。
        return new FakeChatModel(null, failure);
    }

    /**
     * 返回到目前为止收到的所有请求（按发送顺序）。
     * 返回的是一份拷贝，测试代码修改它不会影响假模型内部的记录。
     */
    public List<Prompt> recordedPrompts() {
        synchronized (recordedPrompts) {
            return List.copyOf(recordedPrompts);
        }
    }

    /**
     * 清空已记录的请求。
     * Spring 测试会在多个测试方法之间复用同一个假模型 Bean，
     * 所以每个测试开始前需要清空，避免上一个测试的记录影响断言。
     */
    public void clearRecordedPrompts() {
        recordedPrompts.clear();
    }

    /**
     * 模拟“调用大模型”：不访问网络，直接把预设文本包装成 ChatResponse 返回。
     */
    @Override
    public ChatResponse call(Prompt prompt) {
        // 先记录请求，再决定返回还是抛异常；这样即使模型“失败”，测试也能确认它确实被调用过。
        recordedPrompts.add(prompt);

        // 如果预设了异常，就直接抛出，模拟真实模型调用失败（例如网络中断、额度不足）。
        if (presetFailure != null) {
            throw presetFailure;
        }
        AssistantMessage message = new AssistantMessage(presetReply);
        return new ChatResponse(List.of(new Generation(message)));
    }
}

package dev.javaagentstudy.stage01.explain;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

/**
 * 概念讲解的业务逻辑：组装提示词，通过 ChatClient 调用大模型。
 */
@Service
public class ConceptExplanationService {

    /** 系统提示词：告诉模型它的角色和讲解风格，面向初级 Java 开发者。 */
    private static final String SYSTEM_PROMPT = """
            你是一名耐心的 Java 导师，讲解对象是初级 Java 开发者。
            请用通俗易懂的简体中文讲解，并包含三部分：
            1. 核心原理；
            2. 典型使用场景；
            3. 常见误区。
            """;

    private final ChatClient chatClient;

    // Spring 会自动注入 ChatClient.Builder，它背后使用容器中的 ChatModel。
    public ConceptExplanationService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
    }

    /**
     * 讲解一个 Topic。
     *
     * @param request 包含 topic 和可选 focus 的请求
     * @return 包含 topic 与讲解文本的响应
     */
    public ExplanationResponse  explain(ExplanationRequest request) {
        String userPrompt = buildUserPrompt(request);

        // 只把“调用模型”这一步包起来：这里抛出的异常才算模型故障，
        // 统一转换成 ModelCallException，交给 ApiExceptionHandler 处理。
        String explanation;
        try {
            explanation = chatClient.prompt().user(userPrompt).call().content();
        } catch (RuntimeException ex) {
            throw new ModelCallException(ex);
        }
        return new ExplanationResponse(request.topic(), explanation);
    }

    /**
     * 流式讲解一个 Topic：与 explain 使用同一套提示词，但按分片实时返回文本。
     *
     * @param request 包含 topic 和可选 focus 的请求
     * @return 按生成顺序逐段发出的文本分片
     */
    public Flux<String> explainStream(ExplanationRequest request) {
        String userPrompt = buildUserPrompt(request);
        // 同步与流式共用 buildUserPrompt，保证两个接口对同一请求给出一致的讲解要求。
        // 流中途的异常同样转换成 ModelCallException，避免上游异常细节泄露给调用方。
        return chatClient.prompt().user(userPrompt).stream().content()
                .onErrorMap(RuntimeException.class, ModelCallException::new);
    }

    // 用户提示词：必须包含 topic；如果有 focus，再追加关注重点。
    private String buildUserPrompt(ExplanationRequest request) {
        String userPrompt = "请讲解 Java 主题：" + request.topic();
        if (request.focus() != null && !request.focus().isBlank()) {
            userPrompt += "\n请重点讲解：" + request.focus();
        }
        return userPrompt;
    }
}

package dev.javaagentstudy.stage01.explain;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import dev.javaagentstudy.stage01.web.ModelCallException;
import dev.javaagentstudy.stage01.web.RequestLogSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

/**
 * 概念讲解的业务逻辑：使用 Spring AI PromptTemplate 渲染提示词，通过 ChatClient 调用大模型。
 */
@Service
public class ConceptExplanationService {

    private static final Logger log = LoggerFactory.getLogger(ConceptExplanationService.class);

    /** 系统提示词：告诉模型它的角色和讲解风格，面向初级 Java 开发者。 */
    private static final String SYSTEM_PROMPT = """
            你是一名耐心的 Java 导师，讲解对象是初级 Java 开发者。
            请用通俗易懂的简体中文讲解，并包含三部分：
            1. 核心原理；
            2. 典型使用场景；
            3. 常见误区。
            """;

    /** 参数化 User Prompt 模板：仅指定 Topic 时使用。 */
    private static final PromptTemplate USER_PROMPT_TEMPLATE =
            new PromptTemplate("请讲解 Java 主题：{topic}");

    /** 参数化 User Prompt 模板：附带 Learner 关注重点时使用。 */
    private static final PromptTemplate USER_PROMPT_WITH_FOCUS_TEMPLATE =
            new PromptTemplate("请讲解 Java 主题：{topic}\n请重点讲解：{focus}");

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
    public ExplanationResponse explain(ExplanationRequest request) {
        int topicLength = RequestLogSupport.lengthOf(request.topic());
        boolean hasFocus = RequestLogSupport.hasText(request.focus());
        log.info("服务入口[同步讲解] ConceptExplanationService.explain：mode=sync, topicLength={}, hasFocus={}",
                topicLength, hasFocus);

        String userPrompt = buildUserPrompt(request);
        long startNanos = System.nanoTime();
        // 只把“调用模型”这一步包起来：这里抛出的异常才算模型故障，
        // 统一转换成 ModelCallException，交给 ApiExceptionHandler 处理。
        String explanation;
        try {
            explanation = chatClient.prompt().user(userPrompt).call().content();
        } catch (RuntimeException ex) {
            log.error("服务分支[同步模型调用失败]：条件=ChatClient调用抛出{}，promptChars={}, elapsedMs={}",
                    ex.getClass().getSimpleName(), userPrompt.length(), RequestLogSupport.elapsedMs(startNanos), ex);
            throw new ModelCallException(ex);
        }
        if (explanation == null) {
            log.info("服务分支[同步模型返回空内容]：条件=模型返回content为null，promptChars={}, responseChars=0, elapsedMs={}",
                    userPrompt.length(), RequestLogSupport.elapsedMs(startNanos));
        } else {
            log.info("服务分支[同步模型调用成功]：条件=模型返回非空内容，promptChars={}, responseChars={}, elapsedMs={}",
                    userPrompt.length(), explanation.length(), RequestLogSupport.elapsedMs(startNanos));
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
        int topicLength = RequestLogSupport.lengthOf(request.topic());
        boolean hasFocus = RequestLogSupport.hasText(request.focus());
        log.info("服务入口[流式讲解] ConceptExplanationService.explainStream：mode=stream, topicLength={}, hasFocus={}",
                topicLength, hasFocus);

        String userPrompt = buildUserPrompt(request);
        // 同步与流式共用 buildUserPrompt，保证两个接口对同一请求给出一致的讲解要求。
        // 使用 Flux.defer 确保每次订阅独立计时与计数，避免并发请求或重试共享状态。
        return Flux.defer(() -> {
            long startNanos = System.nanoTime();
            AtomicInteger chunkCount = new AtomicInteger();
            AtomicInteger totalChars = new AtomicInteger();
            // 流正常完成或出错后再触发取消回调时，不再重复记为客户端取消。
            AtomicBoolean terminated = new AtomicBoolean(false);

            // 流中途的异常同样转换成 ModelCallException，避免上游异常细节泄露给调用方。
            return chatClient.prompt().user(userPrompt).stream().content()
                    .onErrorMap(RuntimeException.class, ModelCallException::new)
                    .doOnSubscribe(subscription ->
                            log.info("流式分支[订阅开始]：条件=收到下游订阅信号，promptChars={}, hasFocus={}",
                                    userPrompt.length(), hasFocus))
                    .doOnNext(chunk -> {
                        int index = chunkCount.incrementAndGet();
                        int length = RequestLogSupport.lengthOf(chunk);
                        int accumulated = totalChars.addAndGet(length);
                        log.debug("流式分支[分片产出]：chunkIndex={}, chunkChars={}, totalChars={}",
                                index, length, accumulated);
                    })
                    .doOnComplete(() -> {
                        terminated.set(true);
                        log.info("流式分支[流正常完成]：条件=收到complete信号，chunks={}, totalChars={}, elapsedMs={}",
                                chunkCount.get(), totalChars.get(), RequestLogSupport.elapsedMs(startNanos));
                    })
                    .doOnError(error -> {
                        terminated.set(true);
                        log.error("流式分支[流异常终止]：条件=收到error信号，chunks={}, totalChars={}, elapsedMs={}",
                                chunkCount.get(), totalChars.get(), RequestLogSupport.elapsedMs(startNanos), error);
                    })
                    .doOnCancel(() -> {
                        if (!terminated.get()) {
                            log.warn("流式分支[流被取消]：条件=终态前收到cancel信号，chunks={}, totalChars={}, elapsedMs={}",
                                    chunkCount.get(), totalChars.get(), RequestLogSupport.elapsedMs(startNanos));
                        }
                    });
        });
    }

    // 使用 Spring AI PromptTemplate 渲染参数化用户提示词，替代手工字符串拼接。
    private String buildUserPrompt(ExplanationRequest request) {
        if (RequestLogSupport.hasText(request.focus())) {
            String userPrompt = USER_PROMPT_WITH_FOCUS_TEMPLATE.render(Map.of(
                    "topic", request.topic(),
                    "focus", request.focus()));
            log.info("提示词分支[附带focus]：条件=focus非空白，topicLength={}, focusLength={}, promptChars={}",
                    RequestLogSupport.lengthOf(request.topic()), RequestLogSupport.lengthOf(request.focus()),
                    userPrompt.length());
            return userPrompt;
        }
        String userPrompt = USER_PROMPT_TEMPLATE.render(Map.of("topic", request.topic()));
        log.info("提示词分支[仅基础topic]：条件=focus为null或空白，topicLength={}, promptChars={}",
                RequestLogSupport.lengthOf(request.topic()), userPrompt.length());
        return userPrompt;
    }
}

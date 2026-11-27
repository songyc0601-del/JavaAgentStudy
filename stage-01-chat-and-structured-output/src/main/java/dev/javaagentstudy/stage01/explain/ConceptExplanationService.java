package dev.javaagentstudy.stage01.explain;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

/**
 * 概念讲解的业务逻辑：组装提示词，通过 ChatClient 调用大模型。
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
        int topicLength = ExplainLogSupport.topicLength(request);
        boolean hasFocus = ExplainLogSupport.hasFocus(request);
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
                    ex.getClass().getSimpleName(), userPrompt.length(), ExplainLogSupport.elapsedMs(startNanos), ex);
            throw new ModelCallException(ex);
        }
        if (explanation == null) {
            log.info("服务分支[同步模型返回空内容]：条件=模型返回content为null，promptChars={}, responseChars=0, elapsedMs={}",
                    userPrompt.length(), ExplainLogSupport.elapsedMs(startNanos));
        } else {
            log.info("服务分支[同步模型调用成功]：条件=模型返回非空内容，promptChars={}, responseChars={}, elapsedMs={}",
                    userPrompt.length(), explanation.length(), ExplainLogSupport.elapsedMs(startNanos));
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
        int topicLength = ExplainLogSupport.topicLength(request);
        boolean hasFocus = ExplainLogSupport.hasFocus(request);
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
                        int length = chunk != null ? chunk.length() : 0;
                        int accumulated = totalChars.addAndGet(length);
                        log.debug("流式分支[分片产出]：chunkIndex={}, chunkChars={}, totalChars={}",
                                index, length, accumulated);
                    })
                    .doOnComplete(() -> {
                        terminated.set(true);
                        log.info("流式分支[流正常完成]：条件=收到complete信号，chunks={}, totalChars={}, elapsedMs={}",
                                chunkCount.get(), totalChars.get(), ExplainLogSupport.elapsedMs(startNanos));
                    })
                    .doOnError(error -> {
                        terminated.set(true);
                        log.error("流式分支[流异常终止]：条件=收到error信号，chunks={}, totalChars={}, elapsedMs={}",
                                chunkCount.get(), totalChars.get(), ExplainLogSupport.elapsedMs(startNanos), error);
                    })
                    .doOnCancel(() -> {
                        if (!terminated.get()) {
                            log.warn("流式分支[流被取消]：条件=终态前收到cancel信号，chunks={}, totalChars={}, elapsedMs={}",
                                    chunkCount.get(), totalChars.get(), ExplainLogSupport.elapsedMs(startNanos));
                        }
                    });
        });
    }

    // 用户提示词：必须包含 topic；如果有 focus，再追加关注重点。
    private String buildUserPrompt(ExplanationRequest request) {
        String userPrompt = "请讲解 Java 主题：" + request.topic();
        if (ExplainLogSupport.hasFocus(request)) {
            userPrompt += "\n请重点讲解：" + request.focus();
            log.info("提示词分支[附带focus]：条件=focus非空白，topicLength={}, focusLength={}, promptChars={}",
                    ExplainLogSupport.topicLength(request), ExplainLogSupport.focusLength(request),
                    userPrompt.length());
        } else {
            log.info("提示词分支[仅基础topic]：条件=focus为null或空白，topicLength={}, promptChars={}",
                    ExplainLogSupport.topicLength(request), userPrompt.length());
        }
        return userPrompt;
    }
}

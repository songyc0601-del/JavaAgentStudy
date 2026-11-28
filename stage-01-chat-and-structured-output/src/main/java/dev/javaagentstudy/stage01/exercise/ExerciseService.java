package dev.javaagentstudy.stage01.exercise;

import java.util.Map;

import dev.javaagentstudy.common.domain.Difficulty;
import dev.javaagentstudy.common.domain.Exercise;
import dev.javaagentstudy.stage01.web.ExerciseFormatException;
import dev.javaagentstudy.stage01.web.ModelCallException;
import dev.javaagentstudy.stage01.web.RequestLogSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.StructuredOutputValidationAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

/**
 * Exercise 生成的业务逻辑：使用 PromptTemplate 组装提示词，经 ChatClient 调用模型，并把输出绑定为强类型 Exercise。
 */
@Service
public class ExerciseService {

    private static final Logger log = LoggerFactory.getLogger(ExerciseService.class);

    private static final String SYSTEM_PROMPT = """
            你是一名 Java 导师，请为初级 Java 开发者设计一道练习题。
            题目内容使用简体中文，并给出标题、题干、解题提示与参考答案要点。
            """;

    /** 参数化 User Prompt 模板：未指定 focus 时使用（format 作为参数注入，避免 JSON Schema 花括号被二次解析）。 */
    private static final PromptTemplate USER_PROMPT_TEMPLATE =
            new PromptTemplate("请围绕 Java 主题出题：{topic}\n难度：{difficulty}\n{format}");

    /** 参数化 User Prompt 模板：附带 focus 时使用。 */
    private static final PromptTemplate USER_PROMPT_WITH_FOCUS_TEMPLATE =
            new PromptTemplate("请围绕 Java 主题出题：{topic}\n难度：{difficulty}\n请重点围绕：{focus}\n{format}");

    /** 校验失败后最多再让模型重试的次数。 */
    private static final int MAX_REPEAT_ATTEMPTS = 2;

    private final ChatClient chatClient;
    private final BeanOutputConverter<Exercise> converter = new BeanOutputConverter<>(Exercise.class);

    public ExerciseService(ChatClient.Builder chatClientBuilder) {
        // 校验 Advisor 按 Exercise 的 JSON Schema 校验输出，不合法时把错误信息追加给模型并重试。
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(StructuredOutputValidationAdvisor.builder()
                        .outputType(Exercise.class)
                        .maxRepeatAttempts(MAX_REPEAT_ATTEMPTS)
                        .build())
                .build();
    }

    public Exercise generate(ExerciseRequest request) {
        boolean hasFocus = RequestLogSupport.hasText(request.focus());
        log.info("服务入口[生成Exercise] ExerciseService.generate：topicLength={}, hasDifficulty={}, hasFocus={}",
                RequestLogSupport.lengthOf(request.topic()), request.difficulty() != null, hasFocus);

        Difficulty difficulty;
        if (request.difficulty() == null) {
            // 未传难度时回退为初级，因为目标读者是初级 Java 开发者。
            difficulty = Difficulty.BEGINNER;
            log.info("服务分支[默认难度]：条件=请求未指定difficulty，采用BEGINNER");
        } else {
            difficulty = request.difficulty();
            log.info("服务分支[指定难度]：条件=请求显式指定difficulty，value={}", difficulty);
        }

        String userPrompt = buildUserPrompt(request, difficulty, hasFocus);

        // 分两步：先取文本、再解析。这样“调用失败”和“格式不合法”能映射成不同错误码。
        long startNanos = System.nanoTime();
        String content;
        try {
            content = chatClient.prompt().user(userPrompt).call().content();
        } catch (RuntimeException ex) {
            log.error("服务分支[Exercise模型调用失败]：条件=ChatClient调用抛出{}，promptChars={}, elapsedMs={}",
                    ex.getClass().getSimpleName(), userPrompt.length(), RequestLogSupport.elapsedMs(startNanos), ex);
            throw new ModelCallException(ex);
        }

        Exercise exercise;
        try {
            // 校验重试用尽后 Advisor 会原样返回最后一次输出，所以这里仍可能解析失败。
            exercise = converter.convert(content);
        } catch (RuntimeException ex) {
            // 脱敏例外：Jackson 解析异常的 message 会带上出错位置的模型原文片段，
            // 所以日志只记异常类名与长度，不附带异常对象，避免泄露模型输出。
            log.error("服务分支[Exercise格式不合法]：条件=重试后输出仍无法解析，cause={}，contentChars={}",
                    ex.getClass().getSimpleName(), RequestLogSupport.lengthOf(content));
            throw new ExerciseFormatException(ex);
        }

        if (exercise == null) {
            // 独立记录 null 返回分支，便于区分“解析抛异常”与“转换器返回空”。
            log.error("服务分支[Exercise转换返回空]：条件=BeanOutputConverter返回null，contentChars={}",
                    RequestLogSupport.lengthOf(content));
            throw new ExerciseFormatException(new IllegalStateException("BeanOutputConverter returned null"));
        }

        log.info("服务分支[Exercise生成成功]：条件=模型输出成功绑定为Exercise，promptChars={}, elapsedMs={}",
                userPrompt.length(), RequestLogSupport.elapsedMs(startNanos));
        return exercise;
    }

    private String buildUserPrompt(ExerciseRequest request, Difficulty difficulty, boolean hasFocus) {
        if (hasFocus) {
            // 有关注重点时使用附带 focus 的模板；日志只记长度，不记正文。
            String userPrompt = USER_PROMPT_WITH_FOCUS_TEMPLATE.render(Map.of(
                    "topic", request.topic(),
                    "difficulty", difficulty.name(),
                    "focus", request.focus(),
                    "format", converter.getFormat()));
            log.info("服务分支[附带focus]：条件=focus非空白，focusLength={}, promptChars={}",
                    RequestLogSupport.lengthOf(request.focus()), userPrompt.length());
            return userPrompt;
        }
        String userPrompt = USER_PROMPT_TEMPLATE.render(Map.of(
                "topic", request.topic(),
                "difficulty", difficulty.name(),
                "format", converter.getFormat()));
        log.info("服务分支[无focus]：条件=focus为null或空白，promptChars={}", userPrompt.length());
        return userPrompt;
    }
}

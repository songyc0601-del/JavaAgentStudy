package dev.javaagentstudy.stage01.explain;

import java.util.concurrent.TimeUnit;

/**
 * 概念讲解接口与服务共用的脱敏日志统计辅助方法。
 *
 * <p>只提取长度、布尔标记与耗时等非敏感元数据，确保提示词正文与模型输出不会写入日志。
 */
final class ExplainLogSupport {

    private ExplainLogSupport() {
    }

    static int topicLength(ExplanationRequest request) {
        return request != null && request.topic() != null ? request.topic().length() : 0;
    }

    static boolean hasFocus(ExplanationRequest request) {
        return request != null && request.focus() != null && !request.focus().isBlank();
    }

    static int focusLength(ExplanationRequest request) {
        return request != null && request.focus() != null ? request.focus().length() : 0;
    }

    static long elapsedMs(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }
}

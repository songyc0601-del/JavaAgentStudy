package dev.javaagentstudy.stage01.web;

import java.util.concurrent.TimeUnit;

/**
 * 各接口与服务共用的脱敏日志统计辅助方法。
 *
 * <p>只提取长度、非空标记与耗时等非敏感元数据，确保提示词正文与模型输出不会写入日志。
 */
public final class RequestLogSupport {

    private RequestLogSupport() {
    }

    public static int lengthOf(String value) {
        return value != null ? value.length() : 0;
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public static long elapsedMs(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }
}

package dev.javaagentstudy.stage01.explain;

import java.util.List;

/**
 * 统一的错误响应结构。
 *
 * @param error   机器可读的错误码，例如 VALIDATION_FAILED、MODEL_CALL_FAILED
 * @param message 给人看的错误说明，便于诊断
 * @param details 具体的校验问题列表；没有时为空列表
 */
public record ApiError(String error, String message, List<String> details) {
}

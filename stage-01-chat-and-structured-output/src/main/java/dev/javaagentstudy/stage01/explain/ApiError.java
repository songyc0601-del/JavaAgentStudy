package dev.javaagentstudy.stage01.explain;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 统一的错误响应结构。
 *
 * @param error   机器可读的错误码，例如 VALIDATION_FAILED、MODEL_CALL_FAILED
 * @param message 给人看的错误说明，便于诊断
 * @param details 具体的校验问题列表；没有时为空列表
 */
@Schema(description = "统一 JSON 错误响应")
public record ApiError(
        @Schema(description = "机器可读的错误码", example = "VALIDATION_FAILED")
        String error,
        @Schema(description = "面向开发者的错误说明", example = "Request validation failed")
        String message,
        @Schema(description = "具体的校验失败明细；无明细时为空列表")
        List<String> details) {
}

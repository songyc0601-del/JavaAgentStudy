package dev.javaagentstudy.stage01.explain;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 集中处理接口抛出的异常，把它们转换成结构化的 JSON 错误响应。
 *
 * <p>@RestControllerAdvice 对所有 Controller 生效。
 * 这里只处理“已知类型”的异常，不捕获宽泛的 RuntimeException，
 * 这样我们自己代码的 bug 不会被伪装成模型故障。
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** 入参校验失败：返回 400，并列出每个字段的问题。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        ApiError body = new ApiError("VALIDATION_FAILED", "Request validation failed", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /** 请求体为空或不是合法 JSON：这是调用方的错误，返回 400，不会调用模型。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMalformedRequest(HttpMessageNotReadableException ex) {
        ApiError body = new ApiError("MALFORMED_REQUEST",
                "Request body is missing or is not valid JSON", List.of());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * 下游模型调用失败（网络、额度、服务故障等）：返回 502，表示问题出在上游依赖。
     *
     * <p>注意：响应里只放固定文案，不拼接上游异常的原文，
     * 因为上游错误可能包含地址、模型名、额度等内部细节。
     * 详细原因只写入服务端日志，供开发者排查。
     */
    @ExceptionHandler(ModelCallException.class)
    public ResponseEntity<ApiError> handleModelFailure(ModelCallException ex) {
        log.error("Language model call failed", ex);
        ApiError body = new ApiError("MODEL_CALL_FAILED",
                "The language model is currently unavailable, please try again later", List.of());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }
}

package dev.javaagentstudy.stage01.exercise;

/**
 * 模型已正常返回，但内容经校验重试后仍无法解析为合法 Exercise。
 *
 * <p>与 ModelCallException（下游调用失败）分开定义，是为了让调用方通过错误码区分“模型不可用”和“模型输出格式不对”。
 */
public class ExerciseFormatException extends RuntimeException {

    public ExerciseFormatException(Throwable cause) {
        super("Model output could not be parsed as a valid Exercise", cause);
    }
}

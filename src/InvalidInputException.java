/**
 * 业务校验异常：用户输入的数据违反业务规则时抛出。
 *
 * <p>与 {@link java.lang.IllegalArgumentException} 的区别在于：
 * 本异常语义上表示“用户填错了”，属于程序可以预期并提示用户重试的情况；
 * 而 IllegalArgumentException 表示“调用方写错了代码”，属于程序缺陷。
 * 界面层捕获本异常后只打印提示，不让程序退出。</p>
 *
 * @author 乐豆芽
 * @version 1.0
 */
public class InvalidInputException extends Exception {

    /** 序列化版本号，满足 Exception 可序列化的约定。 */
    private static final long serialVersionUID = 1L;

    /**
     * 构造异常的 Java 标准写法。
     *
     * @param message 面向用户的错误提示
     */
    public InvalidInputException(String message) {
        super(message);
    }

    /**
     * 带底层原因的构造器，用于包装解析异常。
     *
     * @param message 面向用户的错误提示
     * @param cause   底层原因
     */
    public InvalidInputException(String message, Throwable cause) {
        super(message, cause);
    }
}

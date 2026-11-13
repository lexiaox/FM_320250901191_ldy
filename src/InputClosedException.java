/**
 * 输入流结束异常：重定向的文件已读完，或用户在终端按下了 Ctrl+D。
 *
 * <p>为什么不写成内部类？作业明确要求“不要使用内部类”，
 * 因此把它提升为独立的顶层类，语义上也更清晰：
 * 它描述的是“输入通道结束”这件事，属于输入工具之外的独立概念。</p>
 *
 * <p>继承自 RuntimeException 而不是受检异常，是为了不必在每一层方法上声明 throws，
 * 只在主循环处统一捕获一次并干净退出。</p>
 *
 * @author 乐豆芽
 * @version 1.0
 */
public class InputClosedException extends RuntimeException {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /**
     * 构造异常。
     */
    public InputClosedException() {
        super("输入流已结束");
    }
}

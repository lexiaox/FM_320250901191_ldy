/**
 * 自检程序中的“待测动作”：把一段可能抛异常的代码包装成对象，交给 {@link RuleChecker} 执行。
 *
 * <p>不使用 lambda 也不使用匿名内部类，是因为作业要求“不要使用内部类”；
 * 改为独立的顶层接口 + 独立实现类，既满足要求，也让测试代码更易读。</p>
 *
 * @author 乐豆芽
 * @version 1.0
 */
public interface TestAction {

    /**
     * 执行动作。
     *
     * @throws Exception 任何异常都由测试框架捕获并判定
     */
    void run() throws Exception;
}

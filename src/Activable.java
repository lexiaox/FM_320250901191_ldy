/**
 * 可激活接口：定义“能在激活与封锁之间切换”这一能力契约。
 *
 * <p>为什么用接口而不是直接写进父类？</p>
 * <ol>
 *   <li>激活是一种<strong>能力</strong>，而不是“是什么”。父类 {@link Satellite}
 *       表达的是“卫星”这一类别，接口表达的是“可以做哪些事”；</li>
 *   <li>将来如果新增地面站、探测器等对象，它们同样需要激活与封锁能力，
 *       但不一定适合继承 Satellite，实现本接口即可复用校验逻辑；</li>
 *   <li>{@link SatelliteManager} 只面向接口编写激活/封锁流程，
 *       不需要关心具体的卫星子类，降低耦合。</li>
 * </ol>
 *
 * <p>契约要求：只有“封锁非激活”的卫星才能被激活，只有“激活可用”的卫星才能被封锁；
 * 非法状态切换必须抛出异常，并且状态变更必须自动记录时间与事项。</p>
 *
 * @author 乐豆芽
 * @version 1.0
 */
public interface Activable {

    /**
     * 激活卫星。
     *
     * @throws IllegalStateException 当前状态不允许被激活时抛出
     */
    void activate();

    /**
     * 封锁（失活）卫星。
     *
     * @throws IllegalStateException 当前状态不允许被封锁时抛出
     */
    void deactivate();

    /**
     * 判断卫星当前是否处于激活可用状态。
     *
     * @return 激活可用返回 {@code true}，否则返回 {@code false}
     */
    boolean isActive();
}

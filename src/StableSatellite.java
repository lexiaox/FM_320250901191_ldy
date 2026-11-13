/**
 * 静止轨道卫星：长期停留在固定轨道位置，用于通信中继、气象观测等。
 *
 * <p>与父类的差异：</p>
 * <ul>
 *   <li>轨道高度必须不低于 {@link #MIN_STABLE_ORBIT}（本设计定为 3.5），
 *       低于该高度的“静止轨道卫星”在物理上不合理，注册时拒绝；</li>
 *   <li>列表标记为“静止”，种类名称为“静止轨道卫星”。</li>
 * </ul>
 *
 * <p>子类只重写三个抽象方法，其余注册、改名、状态变更等行为全部复用父类实现，
 * 这正是继承“把共有行为上移”的价值。</p>
 *
 * @author 乐豆芽
 * @version 1.0
 */
public class StableSatellite extends Satellite {

    /** 静止轨道卫星的最低轨道高度（本设计自行约定的业务细化规则）。 */
    public static final double MIN_STABLE_ORBIT = 3.5;

    /**
     * 创建静止轨道卫星。
     *
     * @param cosparId     已校验的标识
     * @param name         名称
     * @param orbit        轨道高度
     * @param functionDesc 功能与注意事项
     * @throws InvalidInputException 参数不合法或轨道低于子类下限时抛出
     */
    public StableSatellite(CosparId cosparId, String name, double orbit, String functionDesc)
            throws InvalidInputException {
        super(cosparId, name, orbit, functionDesc);
        checkSpecialRules(orbit);
        commitRegistration();
    }

    /**
     * 返回种类名称。
     *
     * @return “静止轨道卫星”
     */
    @Override
    public String getTypeName() {
        return "静止轨道卫星";
    }

    /**
     * 校验静止轨道卫星的附加规则。
     *
     * @param orbit 待校验的轨道高度
     * @throws InvalidInputException 低于 {@link #MIN_STABLE_ORBIT} 时抛出
     */
    @Override
    public void checkSpecialRules(double orbit) throws InvalidInputException {
        if (orbit < MIN_STABLE_ORBIT) {
            throw new InvalidInputException(
                "静止轨道卫星的轨道高度不得低于 " + String.format("%.1f", MIN_STABLE_ORBIT)
                + "，当前 " + String.format("%.2f", orbit));
        }
    }

    /**
     * 返回列表标记。
     *
     * @return “静止”
     */
    @Override
    public String getTypeTag() {
        return "静止";
    }
}

/**
 * 太阳同步轨道卫星：每天在同一时刻经过同一地点，适合对地遥感。
 *
 * <p>与父类的差异：</p>
 * <ul>
 *   <li>轨道高度必须不高于 {@link #MAX_SUN_SYNC_ORBIT}（本设计定为 3.4），
 *       高于该值就不符合太阳同步轨道的物理特点，注册时拒绝；</li>
 *   <li>列表标记为“太阳”，种类名称为“太阳同步轨道卫星”。</li>
 * </ul>
 *
 * <p>静止轨道与太阳同步轨道的上下限恰好互补（一个 ≥3.5，一个 ≤3.4），
 * 这两种子类用同一套注册流程，却得出不同的合法性判断，体现了多态的作用。</p>
 *
 * @author 乐豆芽
 * @version 1.0
 */
public class SunSyncSatellite extends Satellite {

    /** 太阳同步轨道卫星的最高轨道高度（本设计自行约定的业务细化规则）。 */
    public static final double MAX_SUN_SYNC_ORBIT = 3.4;

    /**
     * 创建太阳同步轨道卫星。
     *
     * @param cosparId     已校验的标识
     * @param name         名称
     * @param orbit        轨道高度
     * @param functionDesc 功能与注意事项
     * @throws InvalidInputException 参数不合法或轨道高于子类上限时抛出
     */
    public SunSyncSatellite(CosparId cosparId, String name, double orbit, String functionDesc)
            throws InvalidInputException {
        super(cosparId, name, orbit, functionDesc);
        checkSpecialRules(orbit);
        commitRegistration();
    }

    /**
     * 返回种类名称。
     *
     * @return “太阳同步轨道卫星”
     */
    @Override
    public String getTypeName() {
        return "太阳同步轨道卫星";
    }

    /**
     * 校验太阳同步轨道卫星的附加规则。
     *
     * @param orbit 待校验的轨道高度
     * @throws InvalidInputException 高于 {@link #MAX_SUN_SYNC_ORBIT} 时抛出
     */
    @Override
    public void checkSpecialRules(double orbit) throws InvalidInputException {
        if (orbit > MAX_SUN_SYNC_ORBIT) {
            throw new InvalidInputException(
                "太阳同步轨道卫星的轨道高度不得高于 " + String.format("%.1f", MAX_SUN_SYNC_ORBIT)
                + "，当前 " + String.format("%.2f", orbit));
        }
    }

    /**
     * 返回列表标记。
     *
     * @return “太阳”
     */
    @Override
    public String getTypeTag() {
        return "太阳";
    }
}

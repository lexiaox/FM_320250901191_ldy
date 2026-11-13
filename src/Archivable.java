/**
 * 可归档接口：定义“报废后能够被归档保存”这一能力契约。
 *
 * <p>作业规则要求：卫星报废后应及时删除、腾出轨道空间，
 * 但删除的信息必须归档保存，状态标记为 {@link SatelliteStatus#DELETE}，
 * 并且该 COSPARID 永远不能再次使用。本接口把“归档”这一能力抽象出来：</p>
 * <ul>
 *   <li>归档时状态被强制改写为 DELETE，并记录归档时间；</li>
 *   <li>归档后的卫星不再占用轨道，轨道空间在下次注册时可以复用；</li>
 *   <li>归档记录应当只读，不能被再次删除或修改。</li>
 * </ul>
 *
 * @author 乐豆芽
 * @version 1.0
 */
public interface Archivable {

    /**
     * 归档卫星：标记为报废状态，并封存为只读记录。
     *
     * @throws IllegalStateException 当前状态不允许归档时抛出
     */
    void archive();

    /**
     * 判断卫星是否已经归档。
     *
     * @return 已归档返回 {@code true}，否则返回 {@code false}
     */
    boolean isArchived();
}

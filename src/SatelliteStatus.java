/**
 * 卫星状态枚举。
 *
 * <p>作业只规定了三种状态，因此使用枚举而不是 int 常量，
 * 既能保证取值合法，又能在 switch 中穷举所有分支。</p>
 *
 * <ul>
 *   <li>{@link #ACTIVATED}：激活可用，能够被查找、修改、删除；</li>
 *   <li>{@link #DEACTIVATED}：封锁非激活，只读，不能修改和删除；</li>
 *   <li>{@link #DELETE}：报废归档，已经移出在用列表，只能被查询。</li>
 * </ul>
 *
 * @author 乐豆芽
 * @version 1.0
 */
public enum SatelliteStatus {

    /** 激活可用。 */
    ACTIVATED("激活可用"),

    /** 封锁非激活。 */
    DEACTIVATED("封锁非激活"),

    /** 报废（已归档）。 */
    DELETE("报废归档");

    /** 中文显示名称，终端中与英文代号一起显示，方便核对。 */
    private final String label;

    /**
     * 构造枚举常量。
     *
     * @param label 中文显示名称
     */
    SatelliteStatus(String label) {
        this.label = label;
    }

    /**
     * 获取中文显示名称。
     *
     * @return 中文显示名称
     */
    public String getLabel() {
        return label;
    }

    /**
     * 生成形如 {@code ACTIVATED(激活可用)} 的展示文本。
     *
     * @return 展示文本
     */
    public String display() {
        return name() + "(" + label + ")";
    }
}

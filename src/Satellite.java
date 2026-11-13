import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 卫星抽象父类：抽取所有卫星共有的属性与行为。
 *
 * <p><strong>共有属性</strong>：COSPARID、名称、轨道高度、状态、功能与注意事项、
 * 注册时间、修改历史。</p>
 *
 * <p><strong>共有行为</strong>：状态变更（激活/封锁/归档）、名称修改、功能说明修改、
 * 历史记录追加、摘要信息输出。</p>
 *
 * <p><strong>交由子类实现的差异</strong>：{@link #getTypeName()}、
 * {@link #checkSpecialRules(double)}、{@link #getTypeTag()}，
 * 分别对应“这一类卫星叫什么、有哪些额外规则、列表里怎么标记”。</p>
 *
 * <p>封装约定：</p>
 * <ul>
 *   <li>{@code cosparId}、{@code registerTime}、{@code history} 三个字段为 {@code final}；</li>
 *   <li>{@code status} 只能通过 {@link #activate()}、{@link #deactivate()}、
 *       {@link #archive()} 三个方法改变，每次改变都会自动追加一条带时间的修改记录；</li>
 *   <li>{@link #getHistory()} 返回只读视图，外部无法直接增删历史。</li>
 * </ul>
 *
 * <p>本类只实现“激活与归档能力”的通用部分，具体流程由子类和
 * {@link SatelliteManager} 协同完成。</p>
 *
 * @author 乐豆芽
 * @version 1.0
 */
public abstract class Satellite implements Activable, Archivable {

    /** 系统规定的最低轨道高度。 */
    public static final double MIN_ORBIT = 1.2;

    /** 系统规定的两颗卫星之间的最小轨道间距。 */
    public static final double MIN_ORBIT_GAP = 0.2;

    /** 名称的最大长度，防止界面排版错乱。 */
    public static final int MAX_NAME_LENGTH = 20;

    /** 功能与注意事项的最大长度。 */
    public static final int MAX_DESC_LENGTH = 200;

    /** 卫星唯一标识，构造时初始化，永不可变。 */
    private final CosparId cosparId;

    /** 用户可识别的名称，可通过 {@link #rename(String)} 修改。 */
    private String name;

    /** 轨道高度（单位由报告约定，程序内视为无量纲的“轨道高度”）。 */
    private double orbit;

    /** 当前状态，只能通过状态变更方法修改。 */
    private SatelliteStatus status;

    /** 功能与注意事项。 */
    private String functionDesc;

    /** 注册时间，构造时由系统产生，不可修改。 */
    private final LocalDateTime registerTime;

    /** 修改历史，只追加不删除。 */
    private final List<ModifyRecord> history;

    /**
     * 创建一颗新卫星，注册时间取当前系统时间。
     *
     * @param cosparId    已经校验过的标识对象
     * @param name        卫星名称
     * @param orbit       轨道高度
     * @param functionDesc 功能与注意事项
     * @throws InvalidInputException 名称或功能说明不合法时抛出
     */
    protected Satellite(CosparId cosparId, String name, double orbit, String functionDesc)
            throws InvalidInputException {
        this.cosparId = cosparId;
        this.registerTime = LocalDateTime.now();
        this.history = new ArrayList<ModifyRecord>();
        this.status = SatelliteStatus.ACTIVATED;
        this.orbit = orbit;
        this.name = validate(name, "卫星名称", MAX_NAME_LENGTH);
        this.functionDesc = validate(functionDesc, "功能与注意事项", MAX_DESC_LENGTH);
        // 注意：注册记录由子类校验完附加规则后再调用 commitRegistration() 写入，
        // 否则非法轨道会在历史中留下半成品记录。
    }

    /**
     * 注册成功时写入第一条历史记录。
     *
     * <p>由子类构造器在 {@link #checkSpecialRules(double)} 通过之后调用，
     * 保证“注册”记录一定出现在一个完全合法的对象上。</p>
     */
    protected void commitRegistration() {
        appendHistory("注册", "注册卫星「" + name + "」，初始状态 " + status.display()
                + "，轨道 " + String.format("%.2f", orbit));
    }

    /** ====================== 抽象方法：交给子类实现 ====================== */

    /**
     * 返回卫星种类的中文名称，例如“静止轨道卫星”。
     *
     * @return 种类名称
     */
    public abstract String getTypeName();

    /**
     * 校验该种类卫星的附加轨道规则。
     *
     * @param orbit 待校验的轨道高度
     * @throws InvalidInputException 不满足该种类规则时抛出
     */
    public abstract void checkSpecialRules(double orbit) throws InvalidInputException;

    /**
     * 返回列表中的种类标记（2 个汉字以内），用于对齐显示。
     *
     * @return 种类标记
     */
    public abstract String getTypeTag();

    /**
     * 校验文本类输入：非空、长度受限。
     *
     * @param text   原始输入
     * @param field  字段名称，用于提示
     * @param maxLen 最大长度
     * @return 去除首尾空白后的文本
     * @throws InvalidInputException 不合法时抛出
     */
    private String validate(String text, String field, int maxLen) throws InvalidInputException {
        if (text == null || text.trim().isEmpty()) {
            throw new InvalidInputException(field + "不能为空");
        }
        String trimmed = text.trim();
        if (trimmed.length() > maxLen) {
            throw new InvalidInputException(field + "不能超过 " + maxLen + " 个字符，当前 " + trimmed.length() + " 个");
        }
        return trimmed;
    }

    /**
     * 获取名称的最大长度，供界面层提示。
     *
     * @return 最大长度
     */
    public int getMaxNameLength() {
        return MAX_NAME_LENGTH;
    }

    /** ====================== 查询方法 ====================== */

    /**
     * 获取卫星标识。
     *
     * @return COSPARID 对象
     */
    public CosparId getCosparId() {
        return cosparId;
    }

    /**
     * 获取卫星名称。
     *
     * @return 名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取轨道高度。
     *
     * @return 轨道高度
     */
    public double getOrbit() {
        return orbit;
    }

    /**
     * 获取当前状态。
     *
     * @return 状态枚举
     */
    public SatelliteStatus getStatus() {
        return status;
    }

    /**
     * 获取功能与注意事项。
     *
     * @return 功能与注意事项
     */
    public String getFunctionDesc() {
        return functionDesc;
    }

    /**
     * 获取注册时间。
     *
     * @return 注册时间
     */
    public LocalDateTime getRegisterTime() {
        return registerTime;
    }

    /**
     * 获取只读的修改历史。
     *
     * <p>返回 {@link Collections#unmodifiableList(List)} 包装后的视图：
     * 调用方能遍历，但调用 {@code add}/{@code remove} 会抛出
     * {@link UnsupportedOperationException}，从而保护历史数据。</p>
     *
     * @return 只读历史列表
     */
    public List<ModifyRecord> getHistory() {
        return Collections.unmodifiableList(history);
    }

    /**
     * 判断当前是否激活可用。
     *
     * @return 语义同 {@link SatelliteStatus#ACTIVATED}
     */
    @Override
    public boolean isActive() {
        return status == SatelliteStatus.ACTIVATED;
    }

    /**
     * 判断是否已归档。
     *
     * @return 语义同 {@link SatelliteStatus#DELETE}
     */
    @Override
    public boolean isArchived() {
        return status == SatelliteStatus.DELETE;
    }

    /**
     * 判断当前是否处于“封锁非激活”状态。
     *
     * @return 封锁状态返回 {@code true}
     */
    public boolean isLocked() {
        return status == SatelliteStatus.DEACTIVATED;
    }

    /** ====================== 状态变更（唯一的状态修改入口） ====================== */

    /**
     * 激活卫星，只有封锁状态可被激活。
     *
     * @throws IllegalStateException 当前状态不是封锁状态时抛出
     */
    @Override
    public void activate() {
        if (status == SatelliteStatus.ACTIVATED) {
            throw new IllegalStateException("卫星 " + cosparId + " 已经是激活状态，无需重复激活");
        }
        if (status == SatelliteStatus.DELETE) {
            throw new IllegalStateException("卫星 " + cosparId + " 已报废归档，不能重新激活");
        }
        changeStatus(SatelliteStatus.ACTIVATED, "激活", "解除封锁，恢复为激活可用");
    }

    /**
     * 封锁卫星，只有激活状态可被封锁。
     *
     * @throws IllegalStateException 当前状态不是激活状态时抛出
     */
    @Override
    public void deactivate() {
        if (status == SatelliteStatus.DEACTIVATED) {
            throw new IllegalStateException("卫星 " + cosparId + " 已经是封锁状态，无需重复封锁");
        }
        if (status == SatelliteStatus.DELETE) {
            throw new IllegalStateException("卫星 " + cosparId + " 已报废归档，不能封锁");
        }
        changeStatus(SatelliteStatus.DEACTIVATED, "封锁", "封锁为非激活状态");
    }

    /**
     * 归档卫星，只有封锁状态可被归档（必须先封锁再报废，防止误删）。
     *
     * @throws IllegalStateException 当前状态不是封锁状态时抛出
     */
    @Override
    public void archive() {
        if (status == SatelliteStatus.DELETE) {
            throw new IllegalStateException("卫星 " + cosparId + " 已归档，不能重复归档");
        }
        if (status != SatelliteStatus.DEACTIVATED) {
            throw new IllegalStateException("卫星 " + cosparId + " 当前为激活状态，请先封锁再报废");
        }
        changeStatus(SatelliteStatus.DELETE, "报废", "状态标记为 DELETE，信息归档保存");
    }

    /**
     * 所有状态变更的唯一底层实现，保证每次变更都留下时间戳。
     *
     * @param target   目标状态
     * @param category 历史记录类别
     * @param detail   历史记录说明
     */
    private void changeStatus(SatelliteStatus target, String category, String detail) {
        SatelliteStatus from = this.status;
        this.status = target;
        appendHistory(category, detail + "（" + from.display() + " → " + target.display() + "）");
    }

    /** ====================== 信息修改 ====================== */

    /**
     * 修改卫星名称。
     *
     * @param newName 新名称
     * @throws InvalidInputException 名称为空、过长或与原名相同时抛出
     * @throws IllegalStateException 被封锁或已归档时抛出
     */
    public void rename(String newName) throws InvalidInputException {
        assertEditable("修改名称");
        String checked = validate(newName, "卫星名称", MAX_NAME_LENGTH);
        if (checked.equals(this.name)) {
            throw new InvalidInputException("新名称与原名相同，无需修改");
        }
        String old = this.name;
        this.name = checked;
        appendHistory("名称", old + " → " + checked);
    }

    /**
     * 修改功能与注意事项。
     *
     * @param newDesc 新的描述内容
     * @throws InvalidInputException 描述为空或超长时抛出
     * @throws IllegalStateException 被封锁或已归档时抛出
     */
    public void updateFunctionDesc(String newDesc) throws InvalidInputException {
        assertEditable("修改功能说明");
        String checked = validate(newDesc, "功能与注意事项", MAX_DESC_LENGTH);
        if (checked.equals(this.functionDesc)) {
            throw new InvalidInputException("新内容与原内容相同，无需修改");
        }
        String old = this.functionDesc;
        this.functionDesc = checked;
        appendHistory("功能", old + " → " + checked);
    }

    /**
     * 检查卫星当前是否允许修改信息。
     *
     * @param action 操作名称，用于错误提示
     * @throws IllegalStateException 不允许修改时抛出
     */
    protected void assertEditable(String action) {
        if (status == SatelliteStatus.DEACTIVATED) {
            throw new IllegalStateException("卫星 " + cosparId + " 已封锁，不能" + action + "，请先激活");
        }
        if (status == SatelliteStatus.DELETE) {
            throw new IllegalStateException("卫星 " + cosparId + " 已报废归档，不能" + action);
        }
    }

    /**
     * 追加一条修改记录，时间由系统自动生成。
     *
     * @param category 修改类别
     * @param detail   修改说明
     */
    protected void appendHistory(String category, String detail) {
        history.add(new ModifyRecord(LocalDateTime.now(), category, detail));
    }

    /** ====================== 输出 ====================== */

    /**
     * 生成单行摘要，用于列表显示。
     *
     * @return 形如 {@code [CN0001] 天眼一号 · 静止轨道 · 轨道 3.60 · ACTIVATED ...}
     */
    public String toSummaryLine() {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(cosparId.getValue()).append("] ");
        sb.append(new TextLayout().pad(name, 20));
        sb.append(' ').append(getTypeTag());
        sb.append(" 轨道=").append(String.format("%5.2f", orbit));
        sb.append("  ").append(new TextLayout().pad(status.display(), 26));
        sb.append(" 注册于 ").append(ModifyRecord.FORMATTER.format(registerTime));
        return sb.toString();
    }

    /**
     * 生成多行详细信息，供“模糊查找”与“删除确认”使用。
     *
     * @return 多行文本
     */
    public String toDetailText() {
        StringBuilder sb = new StringBuilder();
        sb.append("  COSPARID  : ").append(cosparId.getValue())
          .append("  （国家：").append(cosparId.getCountryName()).append("）\n");
        sb.append("  名称      : ").append(name).append('\n');
        sb.append("  种类      : ").append(getTypeName()).append('\n');
        sb.append("  轨道高度  : ").append(String.format("%.2f", orbit)).append('\n');
        sb.append("  状态      : ").append(status.display()).append('\n');
        sb.append("  功能与注意事项 : ").append(new TextLayout().wrap(functionDesc, 18, 72)).append('\n');
        sb.append("  注册时间  : ").append(ModifyRecord.FORMATTER.format(registerTime)).append('\n');
        sb.append("  修改次数  : ").append(history.size()).append(" 条");
        return sb.toString();
    }

    /**
     * 摘要输出，重写了 Object 的方法，便于调试。
     *
     * @return 标识与名称
     */
    @Override
    public String toString() {
        return "[" + cosparId + "] " + name;
    }

    /**
     * 按标识与名称判断是否为同一颗卫星。
     *
     * @param obj 对方对象
     * @return 标识相同返回 {@code true}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Satellite)) {
            return false;
        }
        return cosparId.equals(((Satellite) obj).cosparId);
    }

    /**
     * 与 equals 保持一致，保证以卫星为键的集合行为正确。
     *
     * @return 哈希码
     */
    @Override
    public int hashCode() {
        return cosparId.hashCode();
    }
}

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 卫星管理器：所有业务规则的统一入口，负责组织数据与校验。
 *
 * <p>职责划分：界面层 {@link Main} 只负责输入输出，
 * 本类负责“能不能做、做了之后数据怎么变”，两者互不越界。</p>
 *
 * <p>数据结构（对应业务规则 R10）：</p>
 * <ul>
 *   <li>{@code satellites}：当前在用卫星，<strong>初始容量为 10</strong>；</li>
 *   <li>{@code archived}：报废归档卫星，单独保存，不与在用卫星混在一起；</li>
 *   <li>{@code usedCosparIds}：<strong>历史上出现过</strong>的所有 COSPARID，
 *       包含已归档的，保证报废的标识永不复用（规则 R6）。</li>
 * </ul>
 *
 * @author 乐豆芽
 * @version 1.0
 */
public class SatelliteManager {

    /** ArrayList 的初始容量，作业要求为 10。 */
    public static final int INITIAL_CAPACITY = 10;

    /** 轨道比较时允许的浮点误差，避免 1.2 - 1.0 之类的二进制误差误判。 */
    private static final double EPSILON = 1e-9;

    /** 当前在用卫星。 */
    private final ArrayList<Satellite> satellites;

    /** 已归档卫星（报废）。 */
    private final ArrayList<Satellite> archived;

    /** 历史占用过的所有 COSPARID。 */
    private final Set<String> usedCosparIds;

    /**
     * 创建管理器并预加载示例卫星。
     *
     * <p>预加载 6 颗（要求不少于 5 颗），覆盖两种子类与两种状态，
     * 便于开箱即用演示所有功能。</p>
     */
    public SatelliteManager() {
        this.satellites = new ArrayList<Satellite>(INITIAL_CAPACITY);
        this.archived = new ArrayList<Satellite>();
        this.usedCosparIds = new HashSet<String>();
        preload();
    }

    /**
     * 预加载示例数据。
     *
     * <p>预加载数据直接放入集合，不经过注册流程的“轨道距离”校验之外的额外提示，
     * 但它们本身完全合法；时间由构造器自动产生。</p>
     */
    private void preload() {
        try {
            add(new StableSatellite(new CosparId("CN0001"), "天眼一号", 3.60,
                    "气象遥感与通信中继；注意保持 3.60 轨道位置，避免与天眼二号发生干扰"));
            add(new StableSatellite(new CosparId("US0002"), "星链七号", 4.20,
                    "低时延宽带通信；注意对日定向，每月校准一次姿态"));
            add(new SunSyncSatellite(new CosparId("EU0003"), "哨兵三号", 2.80,
                    "对地遥感与海洋观测；注意避开太阳耀斑期，设备在强辐射时自动降频"));
            add(new SunSyncSatellite(new CosparId("JP0004"), "辉夜二号", 1.50,
                    "月面中继通信；注意近地点轨道衰减，每季度抬升一次"));
            add(new StableSatellite(new CosparId("RU0005"), "北极星一号", 5.10,
                    "高纬度导航定位；注意高轨段的温度控制"));
            add(new SunSyncSatellite(new CosparId("IN0006"), "太阳神一号", 1.20,
                    "太阳活动监测；注意低轨大气阻力，备用推进剂仅够两次抬升"));
        } catch (InvalidInputException e) {
            // 预加载数据是硬编码的正确值，走到这里说明代码本身有缺陷，属于程序错误
            throw new IllegalStateException("预加载数据不合法，请检查内置示例：" + e.getMessage(), e);
        }
        // 预先把其中一颗封锁，演示失活列表功能
        satellites.get(3).deactivate();
    }

    /** ====================== 查询 ====================== */

    /**
     * 获取当前在用卫星的只读列表（按注册顺序）。
     *
     * @return 只读列表
     */
    public List<Satellite> getSatellites() {
        return Collections.unmodifiableList(satellites);
    }

    /**
     * 获取已归档卫星的只读列表。
     *
     * @return 只读列表
     */
    public List<Satellite> getArchived() {
        return Collections.unmodifiableList(archived);
    }

    /**
     * 按状态筛选在用卫星。
     *
     * @param status 目标状态
     * @return 匹配的卫星列表（新列表，不影响原集合）
     */
    public List<Satellite> filterByStatus(SatelliteStatus status) {
        List<Satellite> result = new ArrayList<Satellite>();
        for (Satellite s : satellites) {
            if (s.getStatus() == status) {
                result.add(s);
            }
        }
        return result;
    }

    /**
     * 获取全部失活（封锁）卫星。
     *
     * @return 失活卫星列表
     */
    public List<Satellite> getDeactivatedSatellites() {
        return filterByStatus(SatelliteStatus.DEACTIVATED);
    }

    /**
     * 按 COSPARID 精确查找在用卫星。
     *
     * @param cosparId 6 位标识（大小写不敏感）
     * @return 找到的卫星，未找到返回 {@code null}
     */
    public Satellite findByCosparId(String cosparId) {
        if (cosparId == null) {
            return null;
        }
        String key = cosparId.trim().toUpperCase();
        for (Satellite s : satellites) {
            if (s.getCosparId().getValue().equals(key)) {
                return s;
            }
        }
        return null;
    }

    /**
     * 按 COSPARID 或名称模糊查找，范围包括在用卫星与归档卫星。
     *
     * <p>匹配规则：关键字去空格后转大写，对 COSPARID 与名称都做不区分大小写的包含判断，
     * 因此输入 {@code cn} 能命中所有中国卫星，输入 {@code 天眼} 能命中天眼一号。</p>
     *
     * @param keyword 关键字
     * @return 匹配结果，可能为空列表
     * @throws InvalidInputException 关键字为空时抛出
     */
    public List<Satellite> search(String keyword) throws InvalidInputException {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new InvalidInputException("查找关键字不能为空");
        }
        String key = keyword.trim().toUpperCase();
        List<Satellite> result = new ArrayList<Satellite>();
        for (Satellite s : satellites) {
            if (matches(s, key)) {
                result.add(s);
            }
        }
        for (Satellite s : archived) {
            if (matches(s, key)) {
                result.add(s);
            }
        }
        return result;
    }

    /**
     * 判断卫星是否匹配关键字。
     *
     * @param s   卫星
     * @param key 已转大写的关键字
     * @return 命中返回 {@code true}
     */
    private boolean matches(Satellite s, String key) {
        return s.getCosparId().getValue().contains(key)
                || s.getName().toUpperCase().contains(key);
    }

    /**
     * 判断 COSPARID 是否曾经被使用过（含已归档）。
     *
     * @param cosparId 标识
     * @return 使用过返回 {@code true}
     */
    public boolean isCosparIdUsed(String cosparId) {
        return usedCosparIds.contains(cosparId.trim().toUpperCase());
    }

    /** ====================== 注册 ====================== */

    /**
     * 注册一颗新卫星。
     *
     * <p>校验顺序（任何一步失败都会抛出异常并给出具体原因）：</p>
     * <ol>
     *   <li>COSPARID 格式与白名单国家编码（由 {@link CosparId} 构造器完成）；</li>
     *   <li>COSPARID 唯一性，含已归档的标识；</li>
     *   <li>轨道不低于 1.2；</li>
     *   <li>与所有在用卫星的轨道距离不小于 0.2；</li>
     *   <li>子类附加规则。</li>
     * </ol>
     *
     * @param cosparInput  COSPARID 原始输入
     * @param type         卫星种类，取值 {@code STABLE} 或 {@code SUN_SYNC}（大小写不敏感）
     * @param name         名称
     * @param orbit        轨道高度
     * @param functionDesc 功能与注意事项
     * @return 注册成功的卫星对象
     * @throws InvalidInputException 违反任一业务规则时抛出
     */
    public Satellite register(String cosparInput, String type, String name, double orbit,
                              String functionDesc) throws InvalidInputException {
        CosparId id = new CosparId(cosparInput);
        // 先做与其它卫星无关的校验（种类附加规则），用户能更快看到关键错误
        checkOrbitFloor(orbit);
        checkSpecialOrbitRule(type, orbit);
        if (isCosparIdUsed(id.getValue())) {
            throw new InvalidInputException("COSPARID " + id.getValue()
                    + " 已被使用过（含已报废归档的卫星），不能重复分配");
        }
        checkOrbitGap(orbit);
        Satellite satellite = createSatellite(id, type, name, orbit, functionDesc);
        add(satellite);
        return satellite;
    }

    /**
     * 根据种类创建具体子类对象（工厂方法的雏形）。
     *
     * @param id           标识
     * @param type         种类
     * @param name         名称
     * @param orbit        轨道
     * @param functionDesc 功能说明
     * @return 新对象
     * @throws InvalidInputException 种类未知或参数非法时抛出
     */
    private Satellite createSatellite(CosparId id, String type, String name, double orbit,
                                      String functionDesc) throws InvalidInputException {
        String t = type == null ? "" : type.trim().toUpperCase();
        if ("STABLE".equals(t) || "1".equals(t)) {
            return new StableSatellite(id, name, orbit, functionDesc);
        }
        if ("SUN_SYNC".equals(t) || "2".equals(t)) {
            return new SunSyncSatellite(id, name, orbit, functionDesc);
        }
        throw new InvalidInputException("未知的卫星种类：" + type + "，可选 STABLE(静止轨道) / SUN_SYNC(太阳同步轨道)");
    }

    /**
     * 按种类校验附加轨道规则，无需创建临时对象。
     *
     * @param type  卫星种类
     * @param orbit 轨道高度
     * @throws InvalidInputException 种类未知或不满足子类规则时抛出
     */
    private void checkSpecialOrbitRule(String type, double orbit) throws InvalidInputException {
        String t = type == null ? "" : type.trim().toUpperCase();
        if ("STABLE".equals(t) || "1".equals(t)) {
            if (orbit < StableSatellite.MIN_STABLE_ORBIT) {
                throw new InvalidInputException("静止轨道卫星的轨道高度不得低于 "
                        + String.format("%.1f", StableSatellite.MIN_STABLE_ORBIT)
                        + "，当前 " + String.format("%.2f", orbit));
            }
        } else if ("SUN_SYNC".equals(t) || "2".equals(t)) {
            if (orbit > SunSyncSatellite.MAX_SUN_SYNC_ORBIT) {
                throw new InvalidInputException("太阳同步轨道卫星的轨道高度不得高于 "
                        + String.format("%.1f", SunSyncSatellite.MAX_SUN_SYNC_ORBIT)
                        + "，当前 " + String.format("%.2f", orbit));
            }
        } else {
            throw new InvalidInputException("未知的卫星种类：" + type
                    + "，可选 STABLE(静止轨道) / SUN_SYNC(太阳同步轨道)");
        }
    }

    /**
     * 将卫星放入在用集合，并登记其 COSPARID。
     *
     * @param satellite 卫星
     */
    private void add(Satellite satellite) {
        satellites.add(satellite);
        usedCosparIds.add(satellite.getCosparId().getValue());
    }

    /**
     * 校验轨道规则：最低高度与最小间距。
     *
     * @param orbit 待校验轨道
     * @throws InvalidInputException 不满足规则时抛出
     */
    public void checkOrbit(double orbit) throws InvalidInputException {
        checkOrbitFloor(orbit);
        checkOrbitGap(orbit);
    }

    /**
     * 校验轨道是否满足系统最低高度（规则 R1）。
     *
     * @param orbit 待校验轨道
     * @throws InvalidInputException 低于下限或不是有限数值时抛出
     */
    public void checkOrbitFloor(double orbit) throws InvalidInputException {
        if (Double.isNaN(orbit) || Double.isInfinite(orbit)) {
            throw new InvalidInputException("轨道高度必须是有限数值");
        }
        if (orbit < Satellite.MIN_ORBIT - EPSILON) {
            throw new InvalidInputException("轨道高度不得低于 " + Satellite.MIN_ORBIT
                    + "，当前 " + String.format("%.2f", orbit));
        }
    }

    /**
     * 校验轨道与所有在用卫星的间距（规则 R2）。
     *
     * @param orbit 待校验轨道
     * @throws InvalidInputException 与任一在用卫星过近时抛出
     */
    public void checkOrbitGap(double orbit) throws InvalidInputException {
        for (Satellite s : satellites) {
            double gap = Math.abs(s.getOrbit() - orbit);
            if (gap < Satellite.MIN_ORBIT_GAP - EPSILON) {
                throw new InvalidInputException("轨道高度 " + String.format("%.2f", orbit)
                        + " 与在用卫星 " + s.getCosparId().getValue() + "（" + s.getName()
                        + "，轨道 " + String.format("%.2f", s.getOrbit()) + "）间距 "
                        + String.format("%.2f", gap) + "，小于最小间距 " + Satellite.MIN_ORBIT_GAP);
            }
        }
    }

    /** ====================== 激活 / 封锁 ====================== */

    /**
     * 激活指定卫星（面向 {@link Activable} 接口编写）。
     *
     * @param cosparId 标识
     * @return 激活后的卫星
     * @throws SatelliteNotFoundException 找不到时抛出
     * @throws IllegalStateException     状态不允许时抛出
     */
    public Satellite activate(String cosparId) throws SatelliteNotFoundException {
        Satellite s = requireActiveScope(cosparId, "激活");
        s.activate();
        return s;
    }

    /**
     * 封锁指定卫星。
     *
     * @param cosparId 标识
     * @return 封锁后的卫星
     * @throws SatelliteNotFoundException 找不到时抛出
     * @throws IllegalStateException     状态不允许时抛出
     */
    public Satellite deactivate(String cosparId) throws SatelliteNotFoundException {
        Satellite s = requireActiveScope(cosparId, "封锁");
        s.deactivate();
        return s;
    }

    /** ====================== 删除并归档 ====================== */

    /**
     * 删除在用卫星并归档。
     *
     * <p>规则 R8：报废删除后要腾出轨道空间（从在用集合移除），
     * 但信息归档保存（移入归档集合，状态标记 DELETE）。</p>
     *
     * @param cosparId 标识
     * @return 被归档的卫星
     * @throws SatelliteNotFoundException 找不到时抛出
     * @throws IllegalStateException     卫星未封锁时抛出
     */
    public Satellite deleteAndArchive(String cosparId) throws SatelliteNotFoundException {
        Satellite s = requireActiveScope(cosparId, "删除");
        if (!s.isLocked()) {
            throw new IllegalStateException("卫星 " + s.getCosparId() + " 当前为激活状态，"
                    + "请先执行封锁（失活）后再报废删除");
        }
        Archivable archivable = s;
        archivable.archive();
        satellites.remove(s);
        archived.add(s);
        return s;
    }

    /** ====================== 信息修改 ====================== */

    /**
     * 修改卫星名称。
     *
     * @param cosparId 标识
     * @param newName  新名称
     * @return 修改后的卫星
     * @throws SatelliteNotFoundException 找不到时抛出
     * @throws InvalidInputException      名称不合法时抛出
     */
    public Satellite rename(String cosparId, String newName)
            throws SatelliteNotFoundException, InvalidInputException {
        Satellite s = requireActiveScope(cosparId, "修改名称");
        s.rename(newName);
        return s;
    }

    /**
     * 修改功能与注意事项。
     *
     * @param cosparId 标识
     * @param newDesc  新内容
     * @return 修改后的卫星
     * @throws SatelliteNotFoundException 找不到时抛出
     * @throws InvalidInputException      内容不合法时抛出
     */
    public Satellite updateFunctionDesc(String cosparId, String newDesc)
            throws SatelliteNotFoundException, InvalidInputException {
        Satellite s = requireActiveScope(cosparId, "修改功能说明");
        s.updateFunctionDesc(newDesc);
        return s;
    }

    /**
     * 在用范围内定位卫星，找不到或不允许操作时给出明确异常。
     *
     * @param cosparId 标识
     * @param action   操作名称
     * @return 卫星对象
     * @throws SatelliteNotFoundException 找不到时抛出
     */
    private Satellite requireActiveScope(String cosparId, String action)
            throws SatelliteNotFoundException {
        Satellite s = findByCosparId(cosparId);
        if (s == null) {
            Satellite arc = findArchived(cosparId);
            if (arc != null) {
                throw new SatelliteNotFoundException(cosparId,
                        "卫星 " + cosparId + " 已报废归档，不能" + action + "（归档记录只读）");
            }
            throw new SatelliteNotFoundException(cosparId, "未找到 COSPARID 为 " + cosparId + " 的在用卫星");
        }
        return s;
    }

    /**
     * 在归档集合中查找。
     *
     * @param cosparId 标识
     * @return 找到返回对象，否则 {@code null}
     */
    private Satellite findArchived(String cosparId) {
        String key = cosparId.trim().toUpperCase();
        for (Satellite s : archived) {
            if (s.getCosparId().getValue().equals(key)) {
                return s;
            }
        }
        return null;
    }

    /** ====================== 统计信息 ====================== */

    /**
     * 获取在用卫星数量。
     *
     * @return 数量
     */
    public int getActiveCount() {
        return satellites.size();
    }

    /**
     * 获取归档卫星数量。
     *
     * @return 数量
     */
    public int getArchivedCount() {
        return archived.size();
    }

    /**
     * 获取国家编码帮助文字，供注册界面提示用户。
     *
     * @return 形如 {@code CN=中国, US=美国, ...} 的文本
     */
    public String getCosparIdHelp() {
        if (satellites.isEmpty() && archived.isEmpty()) {
            return "CN=中国, US=美国, RU=俄罗斯, EU=欧洲空间局, JP=日本, IN=印度, FR=法国, DE=德国, "
                    + "GB=英国, KR=韩国, CA=加拿大, BR=巴西, AU=澳大利亚, IL=以色列, UA=乌克兰, NZ=新西兰";
        }
        Satellite sample = satellites.isEmpty() ? archived.get(0) : satellites.get(0);
        return sample.getCosparId().countryCodeHelp();
    }

    /**
     * 生成列表表头。
     *
     * @return 表头文本
     */
    public String listHeader() {
        TextLayout layout = new TextLayout();
        return layout.pad("COSPARID", 11)
                + layout.pad("名称", 22)
                + layout.pad("种类", 6)
                + layout.pad("轨道", 10)
                + layout.pad("状态", 30)
                + "注册时间";
    }
}

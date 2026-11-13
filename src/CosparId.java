import java.util.regex.Pattern;

/**
 * COSPARID 值对象：封装“2 位国家编码 + 4 位数字”的卫星唯一标识。
 *
 * <p>封装要点（对应评分项 O1）：</p>
 * <ol>
 *   <li>字段 {@code value} 使用 {@code final} 修饰，构造时一次性赋值，之后无法改变；</li>
 *   <li>构造器内部完成格式校验，非法值根本无法被创建；</li>
 *   <li>只提供 {@link #getValue()} 读取方法，<strong>不提供任何 setter</strong>；</li>
 *   <li>重写 {@link #equals(Object)} 与 {@link #hashCode()}，使它可以直接作为集合元素判重，
 *       保证“全系统唯一”可以被 {@code HashSet} 准确校验。</li>
 * </ol>
 *
 * <p>国家编码表是本设计自行定义的（作业只要求“2 位国家编码”，未给出清单），
 * 程序内按白名单校验，报告中会列出完整对照表。</p>
 *
 * @author 乐豆芽
 * @version 1.0
 */
public final class CosparId {

    /** 合法格式：2 位大写英文字母 + 4 位阿拉伯数字，总长度 6 位。 */
    private static final Pattern FORMAT = Pattern.compile("^[A-Z]{2}[0-9]{4}$");

    /**
     * 国家编码白名单：两两一组排列 {@code 编码, 国家名}。
     * 采用数组 + 下标查找，避免引入 Map 带来的额外复杂度。
     */
    private static final String[] COUNTRY_TABLE = {
        "CN", "中国", "US", "美国", "RU", "俄罗斯", "EU", "欧洲空间局",
        "JP", "日本", "IN", "印度", "FR", "法国", "DE", "德国",
        "GB", "英国", "KR", "韩国", "CA", "加拿大", "BR", "巴西",
        "AU", "澳大利亚", "IL", "以色列", "UA", "乌克兰", "NZ", "新西兰"
    };

    /** 不可变的值，长度固定为 6。 */
    private final String value;

    /** 国家编码，即 value 的前 2 位。 */
    private final String countryCode;

    /** 4 位数字序号，即 value 的后 4 位。 */
    private final String sequence;

    /**
     * 构造一个合法的 COSPARID。
     *
     * @param input 用户输入的 6 位标识（会自动去除首尾空白并转大写）
     * @throws InvalidInputException 长度、格式或国家编码不合法时抛出
     */
    public CosparId(String input) throws InvalidInputException {
        if (input == null || input.trim().isEmpty()) {
            throw new InvalidInputException("COSPARID 不能为空");
        }
        String normalized = input.trim().toUpperCase();
        if (normalized.length() != 6) {
            throw new InvalidInputException(
                "COSPARID 必须是 6 位（2 位国家编码 + 4 位数字），当前长度 " + normalized.length());
        }
        if (!FORMAT.matcher(normalized).matches()) {
            throw new InvalidInputException(
                "COSPARID 格式错误：应为 2 位大写国家编码 + 4 位数字，例如 CN0001；当前输入 " + normalized);
        }
        String code = normalized.substring(0, 2);
        if (!isKnownCountry(code)) {
            throw new InvalidInputException("未知的国家编码 " + code + "，请使用系统支持的国家编码（见帮助信息）");
        }
        this.value = normalized;
        this.countryCode = code;
        this.sequence = normalized.substring(2);
    }

    /**
     * 判断国家编码是否在白名单中。
     *
     * @param code 2 位国家编码
     * @return 在白名单中返回 {@code true}
     */
    private boolean isKnownCountry(String code) {
        for (int i = 0; i < COUNTRY_TABLE.length; i += 2) {
            if (COUNTRY_TABLE[i].equals(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 返回全部支持的国家编码说明，用于界面提示。
     *
     * <p>本方法不依赖具体实例的值，但按作业要求不使用 static 方法，
     * 因此任何合法的 CosparId 实例都可以调用它（界面层用主菜单的 manager 实例即可）。</p>
     *
     * @return 形如 {@code CN=中国, US=美国, ...} 的文本
     */
    public String countryCodeHelp() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < COUNTRY_TABLE.length; i += 2) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(COUNTRY_TABLE[i]).append('=').append(COUNTRY_TABLE[i + 1]);
        }
        return sb.toString();
    }

    /**
     * 将国家编码翻译为中文国家名。
     *
     * @return 中文国家名，未知时返回原编码
     */
    public String getCountryName() {
        for (int i = 0; i < COUNTRY_TABLE.length; i += 2) {
            if (COUNTRY_TABLE[i].equals(countryCode)) {
                return COUNTRY_TABLE[i + 1];
            }
        }
        return countryCode;
    }

    /**
     * 获取完整的 6 位标识。
     *
     * @return 6 位 COSPARID
     */
    public String getValue() {
        return value;
    }

    /**
     * 获取前 2 位国家编码。
     *
     * @return 国家编码
     */
    public String getCountryCode() {
        return countryCode;
    }

    /**
     * 获取后 4 位数字序号。
     *
     * @return 4 位数字序号
     */
    public String getSequence() {
        return sequence;
    }

    /**
     * 按值比较两个 COSPARID，用于判重。
     *
     * @param obj 对方对象
     * @return 值相同返回 {@code true}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CosparId)) {
            return false;
        }
        return value.equals(((CosparId) obj).value);
    }

    /**
     * 返回与 equals 一致的哈希码。
     *
     * @return 哈希码
     */
    @Override
    public int hashCode() {
        return value.hashCode();
    }

    /**
     * 直接输出 6 位标识，便于日志与界面拼接。
     *
     * @return 6 位标识
     */
    @Override
    public String toString() {
        return value;
    }
}

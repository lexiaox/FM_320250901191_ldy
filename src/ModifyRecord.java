import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 修改记录：一条“什么时间、做了什么修改”的不可变记录。
 *
 * <p>封装说明：时间字段与内容字段都是 {@code final}，对象创建后不能被改写，
 * 外部无法篡改历史记录；时间只由卫星对象内部调用 {@code LocalDateTime.now()} 产生，
 * 不允许界面层传入，从 constructor 层面杜绝“人工填写时间”。</p>
 *
 * @author 乐豆芽
 * @version 1.0
 */
public final class ModifyRecord {

    /** 统一的日期时间格式，精度到秒，便于在终端中对齐显示。 */
    public static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 修改发生的时刻。 */
    private final LocalDateTime time;

    /** 修改项的类别，例如“注册”“名称”“激活”等。 */
    private final String category;

    /** 修改内容说明，例如“名称：天眼一号 → 天眼二号”。 */
    private final String detail;

    /**
     * 构造一条修改记录。
     *
     * @param time     修改时间（由系统产生）
     * @param category 修改类别
     * @param detail   修改内容说明
     */
    public ModifyRecord(LocalDateTime time, String category, String detail) {
        this.time = time;
        this.category = category;
        this.detail = detail;
    }

    /**
     * 获取修改时间。
     *
     * @return 修改时间
     */
    public LocalDateTime getTime() {
        return time;
    }

    /**
     * 获取修改类别。
     *
     * @return 修改类别
     */
    public String getCategory() {
        return category;
    }

    /**
     * 获取修改内容说明。
     *
     * @return 修改内容说明
     */
    public String getDetail() {
        return detail;
    }

    /** 单行显示的最大宽度（含缩进），超出部分自动折行，保证截图不被终端截断。 */
    private static final int LINE_WIDTH = 64;

    /** 折行后续行相对首行的缩进列数。 */
    private static final int CONT_WIDTH = 26;

    /**
     * 格式化成一行文本（不折行），用于日志与代码调用。
     *
     * @return 形如 {@code 2026-10-26 21:40:01  [名称]  天眼一号 → 天眼二号}
     */
    @Override
    public String toString() {
        return FORMATTER.format(time) + "  [" + category + "]  " + detail;
    }

    /**
     * 格式化成分行文本：时间与类别固定占位，说明过长时按宽度折行。
     *
     * @param indent 每行开头缩进的空格数
     * @return 可能包含换行符的文本
     */
    public String toLines(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(' ');
        }
        sb.append(FORMATTER.format(time)).append("  [").append(category).append("]  ");
        sb.append(new TextLayout().wrap(detail, indent + CONT_WIDTH, LINE_WIDTH));
        return sb.toString();
    }
}

/**
 * 业务异常：要操作的卫星在系统中不存在，或者不符合操作前提。
 *
 * <p>例如：用不存在的 COSPARID 去激活、对封锁状态的卫星执行删除等。
 * 这类错误必须向用户明确说明“哪颗卫星、为什么不能操作”，
 * 因此单独定义异常类型，而不是统一抛 Exception。</p>
 *
 * @author 乐豆芽
 * @version 1.0
 */
public class SatelliteNotFoundException extends Exception {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 出错的 COSPARID，便于界面层定位问题数据。 */
    private final String cosparId;

    /**
     * 构造异常。
     *
     * @param cosparId 出错的卫星 COSPARID
     * @param message  面向用户的错误提示
     */
    public SatelliteNotFoundException(String cosparId, String message) {
        super(message);
        this.cosparId = cosparId;
    }

    /**
     * 获取出错的 COSPARID。
     *
     * @return 出错的 COSPARID
     */
    public String getCosparId() {
        return cosparId;
    }
}

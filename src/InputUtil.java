import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * 控制台输入工具：统一处理“读一行、转数字、失败重试”。
 *
 * <p>为什么单独抽一个类？</p>
 * <ul>
 *   <li>避免在界面层的每个方法里重复写 {@code try-catch}；</li>
 *   <li>统一解决 Scanner 的经典陷阱：{@code nextInt()} 遇到字母会抛
 *       {@link java.util.InputMismatchException}，且错误字符留在缓冲区里导致死循环；</li>
 *   <li>输入流结束（EOF，例如重定向输入用完）时抛出 {@link InputClosedException}，
 *       由主类干净退出，而不是让程序崩溃。</li>
 * </ul>
 *
 * @author 乐豆芽
 * @version 1.0
 */
public class InputUtil {

    /** 唯一持有的扫描器，整个程序只创建一个，避免多次包装 System.in。 */
    private final Scanner scanner;

    /**
     * 创建输入工具。
     *
     * @param scanner 扫描器，通常包装 {@link System#in}
     */
    public InputUtil(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * 读取下一行原始输入。
     *
     * @return 输入内容，可能为空字符串
     * @throws InputClosedException 输入流已结束时抛出
     */
    public String readLine() throws InputClosedException {
        try {
            return scanner.nextLine();
        } catch (NoSuchElementException e) {
            throw new InputClosedException();
        }
    }

    /**
     * 读取一行并去除首尾空白。
     *
     * @return 去除空白后的内容
     * @throws InputClosedException 输入流已结束时抛出
     */
    public String readTrimmedLine() throws InputClosedException {
        return readLine().trim();
    }

    /**
     * 读取一个整数，非法输入会提示并重新读取。
     *
     * @param prompt         提示语
     * @param invalidMessage 非法时的提示语
     * @param min            允许的最小值（含）
     * @param max            允许的最大值（含）
     * @return 合法整数
     * @throws InputClosedException 输入流已结束时抛出
     */
    public int readInt(String prompt, String invalidMessage, int min, int max)
            throws InputClosedException {
        while (true) {
            System.out.print(prompt);
            String line = readTrimmedLine();
            if (line.isEmpty()) {
                System.out.println(invalidMessage);
                continue;
            }
            try {
                int value = Integer.parseInt(line);
                if (value < min || value > max) {
                    System.out.println(invalidMessage);
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println(invalidMessage);
            }
        }
    }

    /**
     * 读取一个非负小数，非法输入会提示并重新读取。
     *
     * @param prompt         提示语
     * @param invalidMessage 非法时的提示语
     * @return 合法小数
     * @throws InputClosedException 输入流已结束时抛出
     */
    public double readDouble(String prompt, String invalidMessage) throws InputClosedException {
        while (true) {
            System.out.print(prompt);
            String line = readTrimmedLine();
            if (line.isEmpty()) {
                System.out.println(invalidMessage);
                continue;
            }
            try {
                double value = Double.parseDouble(line);
                if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
                    System.out.println(invalidMessage);
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println(invalidMessage);
            }
        }
    }

    /**
     * 读取非空文本，允许重复输入直到非空。
     *
     * @param prompt         提示语
     * @param invalidMessage 为空时的提示语
     * @return 非空文本
     * @throws InputClosedException 输入流已结束时抛出
     */
    public String readNonBlank(String prompt, String invalidMessage) throws InputClosedException {
        while (true) {
            System.out.print(prompt);
            String line = readTrimmedLine();
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println(invalidMessage);
        }
    }
}

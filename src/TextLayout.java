/**
 * 终端文字排版工具：按“显示宽度”补位与折行，解决中文错位与被截断的问题。
 *
 * <p>为什么需要它？终端里一个汉字占两列，而 {@link String#length()} 只算一个字符，
 * 直接用长度补空格会让中文列错位；长句不折行又会被终端截断，截图时看不到后半段。</p>
 *
 * <p>设计上把它做成普通类（而不是工具类里的 static 方法），
 * 调用方 new 出对象后使用，符合“除主方法外不使用 static 方法”的作业要求。</p>
 *
 * @author 乐豆芽
 * @version 1.0
 */
public class TextLayout {

    /**
     * 判断字符在终端中是否占两列（东亚宽字符）。
     *
     * @param c 字符
     * @return 占两列返回 {@code true}
     */
    public boolean isWide(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA
                || block == Character.UnicodeBlock.HANGUL_SYLLABLES;
    }

    /**
     * 计算字符串在终端中的显示宽度。
     *
     * @param text 文本
     * @return 列数
     */
    public int width(String text) {
        if (text == null) {
            return 0;
        }
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            w += isWide(text.charAt(i)) ? 2 : 1;
        }
        return w;
    }

    /**
     * 按显示宽度右填充空格。
     *
     * @param text  原文本
     * @param width 目标列宽
     * @return 填充后的文本
     */
    public String pad(String text, int width) {
        String value = text == null ? "" : text;
        int current = width(value);
        StringBuilder sb = new StringBuilder(value);
        while (current < width) {
            sb.append(' ');
            current++;
        }
        return sb.toString();
    }

    /**
     * 将长文本按显示宽度折行，续行缩进到指定列；尽量在空格处断行，不拆开英文单词。
     *
     * @param text       原文本
     * @param contColumn 续行缩进的列数
     * @param maxWidth   每行最大显示宽度
     * @return 可能包含换行符的文本
     */
    public String wrap(String text, int contColumn, int maxWidth) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < contColumn; i++) {
            indent.append(' ');
        }
        StringBuilder sb = new StringBuilder();
        int start = 0;
        while (start < text.length()) {
            int used = 0;
            int end = start;
            int lastSpace = -1;
            while (end < text.length()) {
                char c = text.charAt(end);
                int w = isWide(c) ? 2 : 1;
                if (used + w > maxWidth) {
                    break;
                }
                if (c == ' ') {
                    lastSpace = end;
                }
                used += w;
                end++;
            }
            if (end >= text.length()) {
                sb.append(text.substring(start));
                break;
            }
            int cut = (lastSpace > start) ? lastSpace : end;
            sb.append(text, start, cut).append('\n').append(indent);
            start = (cut == lastSpace) ? cut + 1 : cut;
        }
        return sb.toString();
    }
}

import java.util.List;
import java.util.Scanner;

/**
 * 主类：字符界面微管理系统的入口，负责菜单循环与输入输出。
 *
 * <p>面向对象约束的落实：</p>
 * <ul>
 *   <li>{@link #main(String[])} 只做一件事——创建本类实例并调用 {@link #run()}，
 *       全程序仅此一个 static 方法；</li>
 *   <li>其余方法全部是实例方法，业务逻辑委托给 {@link SatelliteManager}；</li>
 *   <li>菜单使用 {@code do-while} 循环，选择 0 时调用 {@link System#exit(int)} 干净退出。</li>
 * </ul>
 *
 * @author 乐豆芽
 * @version 1.0
 */
public class Main {

    /** 菜单分隔线。 */
    private static final String LINE = "========================================================================";

    /** 卫星管理器。 */
    private final SatelliteManager manager;

    /** 统一输入工具。 */
    private final InputUtil input;

    /**
     * 程序入口，只负责组装对象并启动。
     *
     * @param args 可选参数；传入 {@code check} 时不进入交互界面，而是运行业务规则自检
     */
    public static void main(String[] args) {
        if (args.length > 0 && "check".equalsIgnoreCase(args[0])) {
            new RuleChecker().runAll();
            return;
        }
        InputUtil in = new InputUtil(new Scanner(System.in));
        new Main(new SatelliteManager(), in).run();
    }

    /**
     * 构造主界面。
     *
     * @param manager 卫星管理器
     * @param input   输入工具
     */
    public Main(SatelliteManager manager, InputUtil input) {
        this.manager = manager;
        this.input = input;
    }

    /**
     * 主循环：显示菜单、读取选择、分发处理，直到用户选择退出。
     */
    public void run() {
        printBanner();
        boolean running = true;
        try {
            do {
                printMenu();
                int choice = input.readInt("请选择操作 [0-10]：",
                        "输入无效：请输入 0 到 10 之间的整数", 0, 10);
                System.out.println();
                if (choice == 0) {
                    running = false;
                    printGoodbye();
                } else {
                    dispatch(choice);
                }
            } while (running);
        } catch (InputClosedException e) {
            System.out.println("\n检测到输入结束，程序退出。");
            printGoodbye();
        }
        System.exit(0);
    }

    /**
     * 按菜单编号分发到对应功能。
     *
     * @param choice 菜单编号
     */
    private void dispatch(int choice) {
        switch (choice) {
            case 1:
                showCurrentList();
                break;
            case 2:
                registerSatellite();
                break;
            case 3:
                deleteAndArchive();
                break;
            case 4:
                activateSatellite();
                break;
            case 5:
                deactivateSatellite();
                break;
            case 6:
                showDeactivatedList();
                break;
            case 7:
                searchSatellite();
                break;
            case 8:
                renameSatellite();
                break;
            case 9:
                functionDescMenu();
                break;
            case 10:
                showHistory();
                break;
            default:
                System.out.println("未知的菜单项。");
        }
    }

    /** ====================== 1. 显示当前卫星列表 ====================== */

    /**
     * 显示全部在用卫星。
     */
    private void showCurrentList() {
        List<Satellite> all = manager.getSatellites();
        System.out.println("【当前卫星列表】在用 " + all.size() + " 颗，归档 "
                + manager.getArchivedCount() + " 颗");
        System.out.println(LINE);
        System.out.println(manager.listHeader());
        System.out.println(LINE);
        if (all.isEmpty()) {
            System.out.println("（当前没有在用卫星）");
        } else {
            int index = 1;
            for (Satellite s : all) {
                System.out.printf("%2d. %s%n", index++, s.toSummaryLine());
            }
        }
        System.out.println(LINE);
    }

    /** ====================== 2. 注册新卫星 ====================== */

    /**
     * 注册新卫星，按提示逐个输入并校验。
     */
    private void registerSatellite() {
        System.out.println("【注册新卫星】");
        System.out.println("支持的国家编码：" + manager.getCosparIdHelp());
        System.out.println("卫星种类：1 = 静止轨道卫星（轨道 ≥3.5），2 = 太阳同步轨道卫星（轨道 ≤3.4）");
        String cospar = input.readNonBlank("COSPARID（2 位国家编码 + 4 位数字，如 CN0007）：",
                "COSPARID 不能为空");
        int type = input.readInt("卫星种类 [1/2]：", "种类只能是 1 或 2", 1, 2);
        String name = input.readNonBlank("卫星名称：", "名称不能为空");
        double orbit = input.readDouble("轨道高度（最低 " + Satellite.MIN_ORBIT
                + "，与现有卫星间距不小于 " + Satellite.MIN_ORBIT_GAP + "）：", "轨道高度必须是大于 0 的数字");
        String desc = input.readNonBlank("功能与注意事项：", "功能与注意事项不能为空");
        try {
            Satellite created = manager.register(cospar, String.valueOf(type), name, orbit, desc);
            System.out.println("✔ 注册成功：" + created.toSummaryLine());
            System.out.println("  该卫星的 COSPARID " + created.getCosparId()
                    + " 已永久分配，今后不可修改、报废后不可复用。");
        } catch (InvalidInputException e) {
            System.out.println("✘ 注册失败：" + e.getMessage());
            System.out.println("  已放弃本次注册，未产生任何数据变更。");
        }
    }

    /** ====================== 3. 删除并归档 ====================== */

    /**
     * 删除卫星：必须先是封锁状态，删除后进入归档。
     */
    private void deleteAndArchive() {
        System.out.println("【删除旧卫星并归档】删除前必须先将卫星封锁（失活）。");
        String id = input.readNonBlank("请输入要删除的 COSPARID：", "COSPARID 不能为空");
        Satellite target = manager.findByCosparId(id);
        if (target != null) {
            System.out.println("将要报废的卫星信息：");
            System.out.println(target.toDetailText());
            String confirm = input.readNonBlank("确认报废删除？输入 Y 确认，其他任意内容取消：", "请输入 Y 或 N");
            if (!"Y".equalsIgnoreCase(confirm)) {
                System.out.println("已取消删除操作。");
                return;
            }
        }
        try {
            Satellite removed = manager.deleteAndArchive(id);
            System.out.println("✔ 已删除并归档：" + removed.getCosparId() + "「" + removed.getName()
                    + "」，状态 " + removed.getStatus().display());
            System.out.println("  轨道 " + String.format("%.2f", removed.getOrbit())
                    + " 已腾出，可供新卫星使用；该 COSPARID 永久禁用。");
            System.out.println("  归档数量：" + manager.getArchivedCount() + " 颗");
        } catch (SatelliteNotFoundException e) {
            System.out.println("✘ 删除失败：" + e.getMessage());
        } catch (IllegalStateException e) {
            System.out.println("✘ 删除失败：" + e.getMessage());
        }
    }

    /** ====================== 4. 激活 ====================== */

    /**
     * 激活一颗封锁状态的卫星。
     */
    private void activateSatellite() {
        System.out.println("【激活卫星】");
        List<Satellite> locked = manager.getDeactivatedSatellites();
        if (locked.isEmpty()) {
            System.out.println("当前没有处于封锁状态的卫星。");
            return;
        }
        System.out.print("当前封锁中的卫星：");
        for (int i = 0; i < locked.size(); i++) {
            if (i > 0) {
                System.out.print("、");
            }
            System.out.print(locked.get(i).getCosparId() + "(" + locked.get(i).getName() + ")");
        }
        System.out.println();
        String id = input.readNonBlank("请输入要激活的 COSPARID：", "COSPARID 不能为空");
        try {
            Satellite s = manager.activate(id);
            System.out.println("✔ 已激活：" + s.toSummaryLine());
            System.out.println("  最新修改记录：" + lastRecord(s));
        } catch (SatelliteNotFoundException e) {
            System.out.println("✘ 激活失败：" + e.getMessage());
        } catch (IllegalStateException e) {
            System.out.println("✘ 激活失败：" + e.getMessage());
        }
    }

    /** ====================== 5. 封锁 ====================== */

    /**
     * 封锁一颗激活中的卫星。
     */
    private void deactivateSatellite() {
        System.out.println("【封锁（失活）卫星】封锁后的卫星不能被修改和删除，只能查看。");
        String id = input.readNonBlank("请输入要封锁的 COSPARID：", "COSPARID 不能为空");
        try {
            Satellite s = manager.deactivate(id);
            System.out.println("✔ 已封锁：" + s.toSummaryLine());
            System.out.println("  最新修改记录：" + lastRecord(s));
        } catch (SatelliteNotFoundException e) {
            System.out.println("✘ 封锁失败：" + e.getMessage());
        } catch (IllegalStateException e) {
            System.out.println("✘ 封锁失败：" + e.getMessage());
        }
    }

    /** ====================== 6. 显示失活卫星列表 ====================== */

    /**
     * 显示所有封锁（失活）卫星。
     */
    private void showDeactivatedList() {
        List<Satellite> locked = manager.getDeactivatedSatellites();
        System.out.println("【失活（封锁）卫星列表】共 " + locked.size() + " 颗");
        System.out.println(LINE);
        System.out.println(manager.listHeader());
        System.out.println(LINE);
        if (locked.isEmpty()) {
            System.out.println("（当前没有失活卫星）");
        } else {
            int index = 1;
            for (Satellite s : locked) {
                System.out.printf("%2d. %s%n", index++, s.toSummaryLine());
            }
        }
        System.out.println(LINE);
        System.out.println("说明：失活卫星仍占用轨道位置，不能改名、修改说明，也不能激活后被删除；");
        System.out.println("      如需报废，对已封锁卫星直接执行 3 号菜单（删除旧卫星并归档）即可。");
    }

    /** ====================== 7. 模糊查找 ====================== */

    /**
     * 按 COSPARID 或名称模糊查找并显示详情。
     */
    private void searchSatellite() {
        System.out.println("【模糊查找】支持按 COSPARID 片段（如 CN）或名称关键字（如 天眼）查找，"
                + "范围包含在用与归档卫星。");
        String keyword = input.readNonBlank("请输入关键字：", "关键字不能为空");
        try {
            List<Satellite> result = manager.search(keyword);
            System.out.println("共找到 " + result.size() + " 条匹配记录：");
            int index = 1;
            for (Satellite s : result) {
                System.out.println(LINE);
                System.out.println("(" + index++ + ") " + (s.isArchived() ? "【已归档】" : "")
                        + s.getCosparId() + "「" + s.getName() + "」");
                System.out.println(s.toDetailText());
            }
            if (result.isEmpty()) {
                System.out.println("未找到与「" + keyword + "」匹配的卫星。");
            }
        } catch (InvalidInputException e) {
            System.out.println("✘ 查找失败：" + e.getMessage());
        }
    }

    /** ====================== 8. 修改名称 ====================== */

    /**
     * 修改卫星名称。
     */
    private void renameSatellite() {
        System.out.println("【修改卫星名称】");
        String id = input.readNonBlank("请输入要修改的 COSPARID：", "COSPARID 不能为空");
        Satellite target = locate(id, "修改名称");
        if (target == null) {
            return;
        }
        System.out.println("当前名称：" + target.getName() + "（最多 " + target.getMaxNameLength() + " 个字符）");
        String newName = input.readNonBlank("请输入新名称：", "新名称不能为空");
        try {
            Satellite s = manager.rename(id, newName);
            System.out.println("✔ 修改成功：[" + s.getCosparId() + "] 名称 → " + s.getName());
            System.out.println("  最新修改记录：" + lastRecord(s));
        } catch (SatelliteNotFoundException e) {
            System.out.println("✘ 修改失败：" + e.getMessage());
        } catch (InvalidInputException e) {
            System.out.println("✘ 修改失败：" + e.getMessage());
        } catch (IllegalStateException e) {
            System.out.println("✘ 修改失败：" + e.getMessage());
        }
    }

    /** ====================== 9. 功能与注意事项 ====================== */

    /**
     * 功能与注意事项的子菜单：查看或修改。
     */
    private void functionDescMenu() {
        System.out.println("【功能与注意事项】1 = 查看，2 = 修改，0 = 返回主菜单");
        int sub = input.readInt("请选择 [0-2]：", "请输入 0、1 或 2", 0, 2);
        if (sub == 0) {
            return;
        }
        String id = input.readNonBlank("请输入 COSPARID：", "COSPARID 不能为空");
        if (sub == 1) {
            viewFunctionDesc(id);
        } else {
            updateFunctionDesc(id);
        }
    }

    /**
     * 查看功能与注意事项。
     *
     * @param id COSPARID
     */
    private void viewFunctionDesc(String id) {
        Satellite s = locate(id.toUpperCase(), "查看功能说明");
        if (s == null) {
            return;
        }
        System.out.println("[" + s.getCosparId() + "]「" + s.getName() + "」的功能与注意事项：");
        System.out.println("  " + s.getFunctionDesc());
    }

    /**
     * 修改功能与注意事项。
     *
     * @param id COSPARID
     */
    private void updateFunctionDesc(String id) {
        Satellite target = locate(id, "修改功能说明");
        if (target == null) {
            return;
        }
        System.out.println("当前内容：" + target.getFunctionDesc());
        String desc = input.readNonBlank("请输入新的功能与注意事项：", "内容不能为空");
        try {
            Satellite s = manager.updateFunctionDesc(id, desc);
            System.out.println("✔ 修改成功，当前内容：" + s.getFunctionDesc());
            System.out.println("  最新修改记录：" + lastRecord(s));
        } catch (SatelliteNotFoundException e) {
            System.out.println("✘ 修改失败：" + e.getMessage());
        } catch (InvalidInputException e) {
            System.out.println("✘ 修改失败：" + e.getMessage());
        } catch (IllegalStateException e) {
            System.out.println("✘ 修改失败：" + e.getMessage());
        }
    }

    /**
     * 在修改类操作开始前先定位卫星，避免用户白白输入后续内容。
     *
     * @param id     用户输入的标识
     * @param action 操作名称，用于提示
     * @return 找到的在用卫星；找不到时已打印原因并返回 {@code null}
     */
    private Satellite locate(String id, String action) {
        Satellite target = manager.findByCosparId(id);
        if (target == null) {
            System.out.println("✘ 未找到 COSPARID 为 " + id.toUpperCase() + " 的在用卫星，无法" + action);
        }
        return target;
    }

    /** ====================== 10. 修改历史 ====================== */

    /**
     * 查看注册时间与全部修改记录。
     */
    private void showHistory() {
        System.out.println("【注册时间与修改历史】");
        String id = input.readNonBlank("请输入 COSPARID：", "COSPARID 不能为空");
        Satellite s = manager.findByCosparId(id);
        if (s == null) {
            try {
                List<Satellite> hits = manager.search(id);
                if (!hits.isEmpty()) {
                    s = hits.get(0);
                }
            } catch (InvalidInputException ignored) {
                // 关键字已在上面保证非空，不会走到这里
            }
        }
        if (s == null) {
            System.out.println("✘ 未找到 COSPARID 为 " + id + " 的卫星");
            return;
        }
        System.out.println("[" + s.getCosparId() + "]「" + s.getName() + "」"
                + (s.isArchived() ? "（已归档，记录只读）" : ""));
        System.out.println("  注册时间：" + ModifyRecord.FORMATTER.format(s.getRegisterTime()));
        System.out.println("  共 " + s.getHistory().size() + " 条修改记录：");
        int i = 1;
        for (ModifyRecord r : s.getHistory()) {
            System.out.printf("  %2d. %s%n", i++, r.toLines(5));
        }
    }

    /** ====================== 界面输出 ====================== */

    /**
     * 获取某颗卫星最新一条修改记录，长文本自动折行。
     *
     * @param s 卫星
     * @return 最新记录的文本（可能包含换行）
     */
    private String lastRecord(Satellite s) {
        List<ModifyRecord> history = s.getHistory();
        if (history.isEmpty()) {
            return "（无）";
        }
        ModifyRecord latest = history.get(history.size() - 1);
        // 首行前面已有“最新修改记录：”占位，这里首行不再缩进
        return latest.toLines(0);
    }

    /**
     * 打印系统标题。
     */
    private void printBanner() {
        System.out.println(LINE);
        System.out.println("              飞马星球卫星微管理系统  v1.0");
        System.out.println("        飞马星球 · 卫星轨道资源与状态管理中心");
        System.out.println(LINE);
        System.out.println("当前已加载卫星：在用 " + manager.getActiveCount() + " 颗（含失活 "
                + manager.getDeactivatedSatellites().size() + " 颗），归档 " + manager.getArchivedCount() + " 颗");
        System.out.println("运行环境：" + System.getProperty("os.name") + " / " + System.getProperty("os.arch")
                + " / Java " + System.getProperty("java.version"));
    }

    /**
     * 打印主菜单。
     */
    private void printMenu() {
        System.out.println();
        System.out.println(LINE);
        System.out.println(" 1. 显示当前卫星列表            2. 注册添加新卫星");
        System.out.println(" 3. 删除旧卫星并归档            4. 激活卫星");
        System.out.println(" 5. 封锁（失活）卫星            6. 显示失活卫星列表");
        System.out.println(" 7. 按 id 或名称模糊查找        8. 修改卫星名称");
        System.out.println(" 9. 查看/修改功能与注意事项     10. 查看注册与修改历史");
        System.out.println(" 0. 退出系统");
        System.out.println(LINE);
    }

    /**
     * 打印退出提示。
     */
    private void printGoodbye() {
        System.out.println();
        System.out.println("感谢使用飞马星球卫星微管理系统，再见！");
    }
}

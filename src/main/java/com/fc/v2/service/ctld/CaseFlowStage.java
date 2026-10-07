package com.fc.v2.service.ctld;

/**
 * 档目只有这一处定：四档钉死，0..3，中间没有旁路；报面序号一概不认。
 * 面上报给人看的档名用一至四，库存档数用 0..3，换算也只在这一处。
 *
 * @author fuce
 * @date 2026-10-06
 */
public final class CaseFlowStage {

    /** 初核受理（头一档） */
    public static final int ACCEPT_OPEN = 0;
    /** 纳入受限 */
    public static final int LIMIT_IN = 1;
    /** 措施核查 */
    public static final int MEASURE_CHECK = 2;
    /** 解除复核（末一档，走满即闭上） */
    public static final int RELEASE_REVIEW = 3;

    /** 总档数由后端数出来：四档。 */
    public static final int STAGE_COUNT = 4;

    private static final String[] NAMES = {"初核受理", "纳入受限", "措施核查", "解除复核"};

    private CaseFlowStage() {
    }

    /** 库存档数（0..3）→ 面上人话档名。 */
    public static String nameOf(int stage) {
        if (stage < 0 || stage >= STAGE_COUNT) {
            return "档外(" + stage + ")";
        }
        return NAMES[stage];
    }

    /** 面上报错指名用：「第2档（纳入受限）」。 */
    public static String labelOf(int stage) {
        return "第" + (stage + 1) + "档（" + nameOf(stage) + "）";
    }
}

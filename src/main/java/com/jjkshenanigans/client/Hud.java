package com.jjkshenanigans.client;

import com.jjkshenanigans.common.ClientState;
import com.jjkshenanigans.common.GojoState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public final class Hud {
    public static final IGuiOverlay OVERLAY = (gui, g, partialTick, w, h) -> {
        GojoState s = ClientState.STATE;
        if (!s.active) return;
        Font font = gui.getFont();
        int x = w - 124;
        int y = h - 100;

        g.drawString(font, "Cursed Energy", x, y - 10, 0xFFFFFF, true);
        bar(g, x, y, 112, 6, s.energy / GojoState.MAX_ENERGY, 0xFF2E7BFF);

        float six = s.awakened() ? s.awakenTicks / (float) GojoState.AWAKEN_TICKS
                : s.sixEyes / GojoState.MAX_SIX_EYES;
        boolean ready = !s.awakened() && s.sixEyes >= GojoState.MAX_SIX_EYES;
        g.drawString(font, ready ? "Six Eyes [G]" : "Six Eyes", x, y + 10, ready ? 0x66E0FF : 0xFFFFFF, true);
        bar(g, x, y + 20, 112, 6, six, 0xFF66E0FF);

        boolean aw = s.awakened();
        String[] names = aw ? GojoState.AWAKE_NAMES : GojoState.BASE_NAMES;
        int[] maxCd = aw ? GojoState.AWAKE_CD : GojoState.BASE_CD;
        for (int i = 0; i < 4; i++) {
            int cd = s.cooldown[i + (aw ? 4 : 0)];
            float frac = maxCd[i] > 0 ? 1f - cd / (float) maxCd[i] : 1f;
            int ry = y + 34 + i * 15;
            g.fill(x - 1, ry - 1, x + 113, ry + 13, 0xAA000000);
            g.fill(x, ry, x + (int) (112 * Mth01(frac)), ry + 12, frac >= 1f ? 0xFF2E7BFF : 0xFF555555);
            g.drawString(font, "[" + GojoState.KEYS[i] + "] " + names[i], x + 3, ry + 2, 0xFFFFFF, true);
        }
    };

    private static float Mth01(float f) {
        return f < 0 ? 0 : Math.min(f, 1f);
    }

    private static void bar(GuiGraphics g, int x, int y, int w, int h, float frac, int color) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xAA000000);
        g.fill(x, y, x + (int) (w * Mth01(frac)), y + h, color);
    }

    private Hud() {}
}

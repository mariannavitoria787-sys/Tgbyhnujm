package com.painel.mod;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public class PanelGui extends GuiScreen {

    private static int tab = 0;
    private static final String[] TABS = {"Aimbot", "ESP", "Extras", "Cores"};
    private static final int W = 240, H = 170, ROW = 26;

    private int px, py;
    private Slider dragging;
    private final List<Row> rows = new ArrayList<Row>();

    // ---------- Linhas do painel ----------
    private abstract static class Row {
        final String name;
        Row(String name) { this.name = name; }
        abstract void draw(FontRenderer f, int x, int y, int w);
        abstract void click(int mx, int x, int w);
    }

    private static class Toggle extends Row {
        final BooleanSupplier get;
        final Consumer<Boolean> set;
        Toggle(String n, BooleanSupplier g, Consumer<Boolean> s) { super(n); get = g; set = s; }

        void draw(FontRenderer f, int x, int y, int w) {
            boolean on = get.getAsBoolean();
            f.drawStringWithShadow(name, x + 6, y + 8, 0xFFFFFF);
            drawRect(x + w - 40, y + 4, x + w - 6, y + 20, on ? PanelMod.accent() : 0xFF444444);
            f.drawStringWithShadow(on ? "ON" : "OFF", x + w - 32, y + 8, 0xFFFFFF);
        }

        void click(int mx, int x, int w) { set.accept(!get.getAsBoolean()); }
    }

    private static class Slider extends Row {
        final double min, max;
        final DoubleSupplier get;
        final DoubleConsumer set;
        Slider(String n, double min, double max, DoubleSupplier g, DoubleConsumer s) {
            super(n); this.min = min; this.max = max; get = g; set = s;
        }

        void draw(FontRenderer f, int x, int y, int w) {
            double v = get.getAsDouble();
            f.drawStringWithShadow(name + ": " + String.format("%.2f", v), x + 6, y + 3, 0xFFFFFF);
            int bx = x + 6, bw = w - 12;
            drawRect(bx, y + 15, bx + bw, y + 19, 0xFF444444);
            int fill = (int) (bw * ((v - min) / (max - min)));
            drawRect(bx, y + 15, bx + fill, y + 19, PanelMod.accent());
            drawRect(bx + fill - 2, y + 13, bx + fill + 2, y + 21, 0xFFFFFFFF);
        }

        void click(int mx, int x, int w) { drag(mx, x, w); }

        void drag(int mx, int x, int w) {
            double r = (mx - (x + 6)) / (double) (w - 12);
            r = Math.max(0, Math.min(1, r));
            set.accept(min + r * (max - min));
        }
    }

    // ---------- Montagem das abas ----------
    private void buildRows() {
        rows.clear();
        switch (tab) {
            case 0:
                rows.add(new Toggle("Aimbot", () -> PanelMod.aimbot, v -> PanelMod.aimbot = v));
                rows.add(new Slider("Foco (FOV)", 10, 180, () -> PanelMod.aimFov, v -> PanelMod.aimFov = (float) v));
                rows.add(new Slider("Alcance", 3, 10, () -> PanelMod.aimRange, v -> PanelMod.aimRange = (float) v));
                rows.add(new Slider("Velocidade", 0.05, 1, () -> PanelMod.aimSpeed, v -> PanelMod.aimSpeed = (float) v));
                break;
            case 1:
                rows.add(new Toggle("ESP Caixa", () -> PanelMod.espBox, v -> PanelMod.espBox = v));
                rows.add(new Toggle("ESP Linha", () -> PanelMod.espLine, v -> PanelMod.espLine = v));
                rows.add(new Toggle("ESP Esqueleto", () -> PanelMod.espSkeleton, v -> PanelMod.espSkeleton = v));
                break;
            case 2:
                rows.add(new Toggle("Speed", () -> PanelMod.speed, v -> PanelMod.speed = v));
                rows.add(new Slider("Multiplicador", 1.0, 1.6, () -> PanelMod.speedMult, v -> PanelMod.speedMult = (float) v));
                rows.add(new Toggle("Spinbot 360", () -> PanelMod.spinbot, v -> PanelMod.spinbot = v));
                rows.add(new Slider("Giro", 5, 60, () -> PanelMod.spinSpeed, v -> PanelMod.spinSpeed = (float) v));
                break;
            default:
                rows.add(new Slider("Vermelho", 0, 1, () -> PanelMod.cr, v -> PanelMod.cr = (float) v));
                rows.add(new Slider("Verde", 0, 1, () -> PanelMod.cg, v -> PanelMod.cg = (float) v));
                rows.add(new Slider("Azul", 0, 1, () -> PanelMod.cb, v -> PanelMod.cb = (float) v));
        }
    }

    @Override
    public void initGui() {
        px = (width - W) / 2;
        py = (height - H) / 2;
        buildRows();
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawRect(px, py, px + W, py + H, 0xDD101014);
        drawRect(px, py, px + W, py + 18, PanelMod.accent());
        fontRendererObj.drawStringWithShadow("PAINEL  -  F9 para fechar", px + 6, py + 5, 0xFFFFFF);

        int tw = W / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            int tx = px + i * tw;
            drawRect(tx, py + 20, tx + tw - 1, py + 38, i == tab ? PanelMod.accent() : 0xFF2A2A30);
            fontRendererObj.drawStringWithShadow(TABS[i],
                    tx + (tw - fontRendererObj.getStringWidth(TABS[i])) / 2, py + 25, 0xFFFFFF);
        }

        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).draw(fontRendererObj, px, py + 44 + i * ROW, W);
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int btn) throws IOException {
        if (btn != 0) return;
        if (my >= py + 20 && my <= py + 38 && mx >= px && mx < px + W) {
            tab = Math.min(TABS.length - 1, (mx - px) / (W / TABS.length));
            buildRows();
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            int ry = py + 44 + i * ROW;
            if (my >= ry && my < ry + ROW && mx >= px && mx < px + W) {
                Row r = rows.get(i);
                r.click(mx, px, W);
                if (r instanceof Slider) dragging = (Slider) r;
            }
        }
    }

    @Override
    protected void mouseClickMove(int mx, int my, int btn, long time) {
        if (dragging != null) dragging.drag(mx, px, W);
    }

    @Override
    protected void mouseReleased(int mx, int my, int state) {
        dragging = null;
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == Keyboard.KEY_F9) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(c, key);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

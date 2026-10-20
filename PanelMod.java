package com.painel.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

@Mod(modid = "painelmod", name = "Painel", version = "1.0", clientSideOnly = true)
public class PanelMod {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // ---------- Configurações (o painel altera estes valores) ----------
    public static boolean aimbot = false;
    public static float aimFov = 60f;      // foco: ângulo máximo (graus) a partir da mira
    public static float aimRange = 6f;     // alcance em blocos
    public static float aimSpeed = 0.35f;  // 0.05 = suave/lento, 1 = instantâneo

    public static boolean espBox = false;
    public static boolean espLine = false;
    public static boolean espSkeleton = false;

    public static boolean speed = false;
    public static float speedMult = 1.2f;

    public static boolean spinbot = false;
    public static float spinSpeed = 30f;

    // cor do painel e do ESP (0 a 1)
    public static float cr = 0.2f, cg = 0.6f, cb = 1.0f;

    private float spinAngle = 0f;

    public static int accent() {
        return 0xFF000000 | ((int) (cr * 255) << 16) | ((int) (cg * 255) << 8) | (int) (cb * 255);
    }

    @EventHandler
    public void init(FMLInitializationEvent e) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    // ---------- F9 abre o painel ----------
    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        if (Keyboard.getEventKey() == Keyboard.KEY_F9 && Keyboard.getEventKeyState() && mc.currentScreen == null) {
            mc.displayGuiScreen(new PanelGui());
        }
    }

    // ---------- Lógica por tick: aimbot, speed, spinbot ----------
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        EntityPlayerSP me = mc.thePlayer;
        if (me == null || mc.theWorld == null) return;

        if (speed && me.onGround && (me.movementInput.moveForward != 0 || me.movementInput.moveStrafe != 0)) {
            me.motionX *= speedMult;
            me.motionZ *= speedMult;
        }

        if (spinbot) spinAngle = (spinAngle + spinSpeed) % 360f;

        if (aimbot && mc.currentScreen == null) {
            EntityPlayer target = null;
            float best = aimFov;
            for (EntityPlayer p : mc.theWorld.playerEntities) {
                if (p == me || p.isDead || p.getHealth() <= 0) continue;
                if (me.getDistanceToEntity(p) > aimRange) continue;
                float diff = Math.abs(MathHelper.wrapAngleTo180_float(rotsTo(me, p)[0] - me.rotationYaw));
                if (diff <= best) {
                    best = diff;
                    target = p;
                }
            }
            if (target != null) {
                float[] r = rotsTo(me, target);
                float dy = MathHelper.wrapAngleTo180_float(r[0] - me.rotationYaw);
                float dp = r[1] - me.rotationPitch;
                me.rotationYaw += dy * aimSpeed;
                me.rotationPitch = MathHelper.clamp_float(me.rotationPitch + dp * aimSpeed, -90f, 90f);
            }
        }
    }

    private float[] rotsTo(EntityPlayerSP me, EntityPlayer t) {
        double dx = t.posX - me.posX;
        double dz = t.posZ - me.posZ;
        double dy = (t.posY + t.getEyeHeight() * 0.9) - (me.posY + me.getEyeHeight());
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90f;
        float pitch = (float) -(Math.atan2(dy, dist) * 180.0 / Math.PI);
        return new float[]{yaw, pitch};
    }

    // ---------- Spinbot: só o corpo gira, a câmera (rotationYaw) fica normal ----------
    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Pre e) {
        if (!spinbot || e.entityPlayer != mc.thePlayer) return;
        EntityPlayer p = e.entityPlayer;
        p.renderYawOffset = spinAngle;
        p.prevRenderYawOffset = spinAngle;
        p.rotationYawHead = spinAngle;
        p.prevRotationYawHead = spinAngle;
    }

    // ---------- ESP ----------
    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!(espBox || espLine || espSkeleton) || mc.theWorld == null) return;

        RenderManager rm = mc.getRenderManager();
        float pt = e.partialTicks;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glLineWidth(2f);

        int ri = (int) (cr * 255), gi = (int) (cg * 255), bi = (int) (cb * 255);

        for (EntityPlayer p : mc.theWorld.playerEntities) {
            if (p == mc.thePlayer || p.isDead) continue;

            double x = p.lastTickPosX + (p.posX - p.lastTickPosX) * pt - rm.viewerPosX;
            double y = p.lastTickPosY + (p.posY - p.lastTickPosY) * pt - rm.viewerPosY;
            double z = p.lastTickPosZ + (p.posZ - p.lastTickPosZ) * pt - rm.viewerPosZ;

            GlStateManager.color(cr, cg, cb, 1f);

            if (espBox) {
                AxisAlignedBB b = p.getEntityBoundingBox();
                AxisAlignedBB box = new AxisAlignedBB(
                        b.minX - p.posX + x, b.minY - p.posY + y, b.minZ - p.posZ + z,
                        b.maxX - p.posX + x, b.maxY - p.posY + y, b.maxZ - p.posZ + z);
                RenderGlobal.drawOutlinedBoundingBox(box, ri, gi, bi, 255);
            }

            Tessellator t = Tessellator.getInstance();
            WorldRenderer wr = t.getWorldRenderer();

            if (espLine) {
                Vec3 look = mc.thePlayer.getLook(pt);
                wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
                wr.pos(look.xCoord, look.yCoord, look.zCoord).endVertex();
                wr.pos(x, y + p.height / 2.0, z).endVertex();
                t.draw();
            }

            if (espSkeleton) {
                double yaw = Math.toRadians(p.renderYawOffset);
                double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
                wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);
                // coluna e cabeça
                seg(wr, x, y + 0.75, z, x, y + 1.4, z);
                seg(wr, x, y + 1.4, z, x, y + 1.65, z);
                // ombros
                seg(wr, x + rx * 0.3, y + 1.4, z + rz * 0.3, x - rx * 0.3, y + 1.4, z - rz * 0.3);
                // braços
                seg(wr, x + rx * 0.3, y + 1.4, z + rz * 0.3, x + rx * 0.3, y + 0.8, z + rz * 0.3);
                seg(wr, x - rx * 0.3, y + 1.4, z - rz * 0.3, x - rx * 0.3, y + 0.8, z - rz * 0.3);
                // quadril e pernas
                seg(wr, x + rx * 0.12, y + 0.75, z + rz * 0.12, x - rx * 0.12, y + 0.75, z - rz * 0.12);
                seg(wr, x + rx * 0.12, y + 0.75, z + rz * 0.12, x + rx * 0.12, y, z + rz * 0.12);
                seg(wr, x - rx * 0.12, y + 0.75, z - rz * 0.12, x - rx * 0.12, y, z - rz * 0.12);
                t.draw();
            }
        }

        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void seg(WorldRenderer wr, double x1, double y1, double z1, double x2, double y2, double z2) {
        wr.pos(x1, y1, z1).endVertex();
        wr.pos(x2, y2, z2).endVertex();
    }
}

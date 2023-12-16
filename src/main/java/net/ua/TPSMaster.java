package net.ua;

import net.minecraft.client.MinecraftClient;
import net.ua.mixin.client.MinecraftClientMixin;
import net.ua.mixin.otherwise.RenderTickCounterMixin;

public class TPSMaster {
    public static RenderTickCounterMixin rtc = null;

    public static void change(float tps) {
        if (rtc == null) {
            MinecraftClientMixin client = (MinecraftClientMixin) MinecraftClient.getInstance();
            rtc = (RenderTickCounterMixin) client.getRenderTickCounter();
        }
        if (rtc.getTickTime() != 1000f / tps) {
            rtc.setTickTime(1000f / tps);
        }
    }
}
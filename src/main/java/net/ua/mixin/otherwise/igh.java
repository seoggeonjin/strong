package net.ua.mixin.otherwise;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Colors;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;


@Mixin(InGameHud.class)
@Environment(value=EnvType.CLIENT)
public class igh {
    @Overwrite
    public void renderHealthBar(@NotNull DrawContext context, @NotNull PlayerEntity player, int x, int y, int lines, int regeneratingHeartIndex, float maxHealth, int lastHealth, int health, int absorption, boolean blinking) {
        context.drawText(((InGameHud) (Object) this).getTextRenderer(), String.valueOf(lastHealth), x, y, Colors.LIGHT_RED, true);
        context.drawText(((InGameHud) (Object) this).getTextRenderer(), String.valueOf(player.getMaxHealth() - player.getHealth()), x - 80, y, Colors.RED, true);
        context.drawText(((InGameHud) (Object) this).getTextRenderer(), String.valueOf(lastHealth - health), x + String.valueOf(lastHealth).length() + 15 + String.valueOf(player.getHealth()).length() * 4 , y, -65536 - 256, true);
    }
}
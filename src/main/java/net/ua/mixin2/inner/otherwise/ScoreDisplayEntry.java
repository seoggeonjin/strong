package net.ua.mixin2.inner.otherwise;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

@Environment(value= EnvType.CLIENT)
public record ScoreDisplayEntry(Text name, int score, @Nullable Text formattedScore, int scoreWidth) {

    @Nullable
    public Text formattedScore() {
        return this.formattedScore;
    }
}

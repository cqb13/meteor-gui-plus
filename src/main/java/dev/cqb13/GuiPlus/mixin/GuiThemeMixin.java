package dev.cqb13.GuiPlus.mixin;

import dev.cqb13.GuiPlus.gui.GuiThemeAccessor;
import meteordevelopment.meteorclient.gui.GuiTheme;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GuiTheme.class)
public abstract class GuiThemeMixin implements GuiThemeAccessor {
    @Shadow
    @Mutable
    public String name;

    @Override
    public void guiplus$setName(String name) {
        this.name = name;
    }
}

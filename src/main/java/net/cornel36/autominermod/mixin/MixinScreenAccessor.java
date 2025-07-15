package net.cornel36.autominermod.mixin;

import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(Screen.class)
public interface MixinScreenAccessor {
    @Accessor("drawables")
    List<Element> getDrawables();

    @Accessor("children")
    List<Element> getChildren();
}
package settingdust.lightmanscurrency.claimshop.mixin;

import io.github.lightman314.lightmanscurrency.api.misc.client.rendering.EasyGuiGraphics;
import io.github.lightman314.lightmanscurrency.client.util.ScreenPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = EasyGuiGraphics.class, remap = false)
public interface EasyGuiGraphicsAccessor {
    @Accessor
    ScreenPosition getOffset();
}

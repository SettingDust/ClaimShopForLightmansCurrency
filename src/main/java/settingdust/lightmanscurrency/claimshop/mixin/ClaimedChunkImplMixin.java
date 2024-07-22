package settingdust.lightmanscurrency.claimshop.mixin;

import dev.ftb.mods.ftbchunks.data.ClaimedChunkImpl;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClaimedChunkImpl.class)
public class ClaimedChunkImplMixin {
    @Redirect(
        method = "getMessage", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"
    )
    )
    private MutableComponent lightmanscurrency$useTranslatable(final String pText) {
        return Component.translatable("ftbchunks.claim_result.ok");
    }
}

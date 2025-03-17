package settingdust.lightmanscurrency.claimshop.mixin;

import io.github.lightman314.lightmanscurrency.api.traders.trade.client.TradeInteractionHandler;
import io.github.lightman314.lightmanscurrency.api.traders.trade.client.TradeRenderManager;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.trade.TradeButton;
import io.github.lightman314.lightmanscurrency.client.gui.widget.easy.EasyButton;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TradeButton.class, remap = false)
public abstract class TradeButtonMixin extends EasyButton {

    private TradeButtonMixin(@NotNull final EasyButton.EasyButtonBuilder<?> builder) {
        super(builder);
    }

    @Shadow
    public abstract TradeRenderManager<?> getTradeRenderer();

    @Shadow public abstract void HandleInteractionClick(
        final int mouseX,
        final int mouseY,
        final int button,
        @NotNull final TradeInteractionHandler handler
    );

    @Override
    public boolean mouseClicked(final double pMouseX, final double pMouseY, final int pButton) {
        if (getTradeRenderer() instanceof TradeInteractionHandler consumer) {
            HandleInteractionClick((int) pMouseX, (int) pMouseY, pButton, consumer);
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }
}

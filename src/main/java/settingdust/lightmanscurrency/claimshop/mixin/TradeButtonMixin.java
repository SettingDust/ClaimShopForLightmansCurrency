package settingdust.lightmanscurrency.claimshop.mixin;

import io.github.lightman314.lightmanscurrency.api.traders.trade.client.TradeRenderManager;
import io.github.lightman314.lightmanscurrency.client.gui.widget.TradeButtonArea;
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.trade.TradeButton;
import io.github.lightman314.lightmanscurrency.client.gui.widget.easy.EasyButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = TradeButton.class, remap = false)
public abstract class TradeButtonMixin extends EasyButton {

    @Shadow
    public abstract TradeRenderManager<?> getTradeRenderer();

    @Shadow
    public abstract void onInteractionClick(
        final int mouseX,
        final int mouseY,
        final int button,
        final TradeButtonArea.InteractionConsumer consumer
    );

    private TradeButtonMixin(final int x, final int y, final int width, final int height, final Runnable press) {
        super(x, y, width, height, press);
    }

    @Override
    public boolean mouseClicked(final double pMouseX, final double pMouseY, final int pButton) {
        if (getTradeRenderer() instanceof TradeButtonArea.InteractionConsumer consumer) {
            onInteractionClick((int) pMouseX, (int) pMouseY, pButton, consumer);
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }
}

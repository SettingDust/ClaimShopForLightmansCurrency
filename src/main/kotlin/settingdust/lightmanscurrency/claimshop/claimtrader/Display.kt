package settingdust.lightmanscurrency.claimshop.claimtrader

import com.mojang.blaze3d.systems.RenderSystem
import dev.ftb.mods.ftbchunks.client.FTBChunksClient
import dev.ftb.mods.ftbchunks.client.gui.LargeMapScreen
import dev.ftb.mods.ftbchunks.client.map.MapDimension
import dev.ftb.mods.ftbchunks.client.map.MapManager
import dev.ftb.mods.ftbchunks.client.map.RenderMapImageTask
import dev.ftb.mods.ftbchunks.net.RequestMapDataPacket
import dev.ftb.mods.ftblibrary.icon.Color4I
import dev.ftb.mods.ftblibrary.icon.FaceIcon
import dev.ftb.mods.ftblibrary.math.MathUtils
import dev.ftb.mods.ftblibrary.ui.BaseScreen
import dev.ftb.mods.ftblibrary.ui.GuiHelper
import dev.ftb.mods.ftblibrary.ui.Theme
import dev.ftb.mods.ftblibrary.ui.ThemeManager
import io.github.lightman314.lightmanscurrency.LCText
import io.github.lightman314.lightmanscurrency.api.misc.client.rendering.EasyGuiGraphics
import io.github.lightman314.lightmanscurrency.api.misc.player.PlayerReference
import io.github.lightman314.lightmanscurrency.api.money.input.MoneyValueWidget
import io.github.lightman314.lightmanscurrency.api.money.value.MoneyValue
import io.github.lightman314.lightmanscurrency.api.network.LazyPacketData
import io.github.lightman314.lightmanscurrency.api.notifications.Notification
import io.github.lightman314.lightmanscurrency.api.notifications.NotificationAPI
import io.github.lightman314.lightmanscurrency.api.notifications.NotificationType
import io.github.lightman314.lightmanscurrency.api.traders.TradeContext
import io.github.lightman314.lightmanscurrency.api.traders.TraderData
import io.github.lightman314.lightmanscurrency.api.traders.menu.storage.ITraderStorageMenu
import io.github.lightman314.lightmanscurrency.api.traders.menu.storage.TraderStorageClientTab
import io.github.lightman314.lightmanscurrency.api.traders.menu.storage.TraderStorageTab
import io.github.lightman314.lightmanscurrency.api.traders.trade.TradeData
import io.github.lightman314.lightmanscurrency.api.traders.trade.client.TradeInteractionData
import io.github.lightman314.lightmanscurrency.api.traders.trade.client.TradeInteractionHandler
import io.github.lightman314.lightmanscurrency.api.traders.trade.client.TradeRenderManager
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.trade.AlertData
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.trade.DisplayData
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.trade.DisplayEntry
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.trade.TradeButton
import io.github.lightman314.lightmanscurrency.client.util.ScreenArea
import io.github.lightman314.lightmanscurrency.client.util.ScreenPosition
import io.github.lightman314.lightmanscurrency.client.util.TextRenderUtil
import io.github.lightman314.lightmanscurrency.common.notifications.categories.TraderCategory
import io.github.lightman314.lightmanscurrency.common.notifications.types.TaxableNotification
import io.github.lightman314.lightmanscurrency.common.util.IconData
import io.github.lightman314.lightmanscurrency.common.util.IconUtil
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.Slot
import net.minecraft.world.level.ChunkPos
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import settingdust.lightmanscurrency.claimshop.ClaimShopForLightmansCurrency
import settingdust.lightmanscurrency.claimshop.mixin.EasyGuiGraphicsAccessor
import java.util.function.Function
import kotlin.jvm.optionals.getOrNull

class ClaimNotification : TaxableNotification {

    companion object {
        val TYPE =
            NotificationType(
                ClaimShopForLightmansCurrency.location("claim_trade"), ::ClaimNotification
            )

        init {
            NotificationAPI.registerNotification(TYPE)
        }
    }

    lateinit var pos: ChunkPos
        private set

    var cost: MoneyValue = MoneyValue.empty()
        private set

    lateinit var customer: String
        private set

    private lateinit var category: TraderCategory

    constructor(
        taxesPaid: MoneyValue,
        trade: ClaimTradeData,
        cost: MoneyValue,
        customer: PlayerReference,
        category: TraderCategory
    ) : super(taxesPaid) {
        this.pos = trade.pos
        this.cost = cost
        this.customer = customer.getName(false)
        this.category = category
    }

    constructor()

    override fun getType() = TYPE

    override fun getCategory() = category

    override fun canMerge(other: Notification) =
        if (other is ClaimNotification) {
            if (!other.category.matches(category)) false
            else if (other.pos != pos) false
            else if (other.customer != customer) false
            else if (other.cost == cost) false else TaxesMatch(other)
        } else false

    override fun getNormalMessage() =
        ClaimShopForLightmansCurrency.Texts.NOTIFICATION_TRADE_CLAIM[customer, pos]

    override fun saveNormal(tag: CompoundTag) {
        tag.put("TraderInfo", category.save())
        tag.putLong("Pos", pos.toLong())
        tag.put("Price", cost.save())
        tag.putString("Customer", customer)
    }

    override fun loadNormal(tag: CompoundTag) {
        category = TraderCategory(tag.getCompound("TraderInfo"))
        pos = ChunkPos(tag.getLong("Pos"))
        cost = MoneyValue.safeLoad(tag, "Price")
        customer = tag.getString("Customer")
    }
}

class ClaimTradeEditTab(menu: ITraderStorageMenu) : TraderStorageTab(menu) {
    companion object {
        const val NEW_PRICE = "NewPrice"
    }

    class Display(screen: Any, commonTab: ClaimTradeEditTab?) :
        TraderStorageClientTab<ClaimTradeEditTab>(screen, commonTab) {

        lateinit var tradeDisplay: TradeButton
        var priceSelection: MoneyValueWidget? = null

        override fun tabVisible(): Boolean {
            return false
        }

        override fun blockInventoryClosing(): Boolean {
            return true
        }

        override fun getIcon(): IconData = IconUtil.ICON_TRADELIST

        override fun getTooltip() = LCText.TOOLTIP_TRADER_EDIT_TRADES.get()

        override fun initialize(screenArea: ScreenArea, firstOpen: Boolean) {
            val trade = commonTab.getTrade()
            tradeDisplay =
                addChild(
                    TradeButton.builder()
                        .position(screenArea.pos.offset(10, 18))
                        .context(menu::getContext)
                        .trade { trade }
                        .build())
            tradeDisplay.position = screenArea.pos.offset(10, 18)
            priceSelection =
                addChild(
                    MoneyValueWidget.builder()
                        .position(screenArea.pos.offset(15, 55))
                        .oldIfNotFirst(firstOpen, priceSelection)
                        .startingValue(trade?.cost ?: MoneyValue.empty())
                        .valueHandler(::onValueChanged)
                        .build()
                )
        }

        override fun renderBG(gui: EasyGuiGraphics) {
            // TODO 渲染地图
        }

        fun onValueChanged(value: MoneyValue) = commonTab.setPrice(value)
    }

    @OnlyIn(Dist.CLIENT)
    override fun createClientTab(screen: Any) = Display(screen, this)

    override fun canOpen(player: Player) = true

    override fun onTabOpen() {}

    override fun onTabClose() {}

    override fun addStorageMenuSlots(addSlot: Function<Slot, Slot>) {}

    override fun receiveMessage(data: LazyPacketData) {
        when {
            NEW_PRICE in data -> {
                setPrice(data.getMoneyValue(NEW_PRICE))
            }
        }
    }

    fun getTrade(): ClaimTradeData? {
        val trader = menu.trader
        if (trader !is ClaimTraderData) return null
        return trader.trade
    }

    fun setPrice(value: MoneyValue) {
        val trade = getTrade() ?: return
        trade.cost = value
        menu.trader!!.markTradesDirty()
        if (menu.isClient) menu.SendMessage(LazyPacketData.simpleMoneyValue(NEW_PRICE, value))
    }
}

@OnlyIn(Dist.CLIENT)
class ClaimTradeButtonRenderer(trade: ClaimTradeData) :
    TradeRenderManager<ClaimTradeData>(trade), TradeInteractionHandler {
    override fun tradeButtonWidth(context: TradeContext) = 132

    override fun arrowPosition(context: TradeContext) = ScreenPosition.ofOptional(36, 1)

    override fun inputDisplayArea(context: TradeContext) = DisplayData(1, 1, 34, 16)

    override fun getInputDisplays(context: TradeContext) =
        mutableListOf(
            DisplayEntry.of(
                trade.cost,
                if (context.isStorageMode) LCText.TOOLTIP_TRADE_EDIT_PRICE.getAsList() else null
            )
        )

    override fun outputDisplayArea(context: TradeContext) = DisplayData(58, 1, 72, 16)

    override fun getOutputDisplays(context: TradeContext) =
        mutableListOf<DisplayEntry>(
            DisplayEntry.of(
                Component.literal(trade.pos.toString()),
                TextRenderUtil.TextFormatting.create(),
                listOf(
                    Component.translatable(
                        "gui.claim_shop_for_lightmans_currency.result", trade.pos
                    )
                )
            )
        )

    //        mutableListOf<DisplayEntry>(
    //            TraderChunkDisplayEntry(
    //                ClaimTraderBlockEntity.CLAIM_TRADER.getBlockEntity(
    //                    Minecraft.getInstance().level!!, context.trader.pos)!!,
    //                listOf(
    //                    Component.translatable(
    //                        "gui.claim_shop_for_lightmans_currency.result", trade.pos))))

    override fun getAdditionalAlertData(context: TradeContext, alerts: MutableList<AlertData>) {
        val seller = context.trader.owner.playerForContext
        val buyer = context.playerReference
        //        if (seller.id == buyer.id) alerts.add(AlertData.warn(LCText.TOOLTIP_OUT_OF_STOCK))
        if (!context.hasFunds(trade.cost)) alerts.add(AlertData.warn(LCText.TOOLTIP_CANNOT_AFFORD))
    }

    override fun HandleTradeInputInteraction(
        trader: TraderData,
        trade: TradeData,
        data: TradeInteractionData,
        mouseButton: Int
    ) {
    }

    override fun HandleTradeOutputInteraction(
        trader: TraderData,
        trade: TradeData,
        data: TradeInteractionData,
        mouseButton: Int
    ) {
        if (mouseButton == 0) {
            Minecraft.getInstance().player!!.closeContainer()
            LargeMapScreen.openMap()
        }
    }

    override fun HandleOtherTradeInteraction(
        trader: TraderData,
        trade: TradeData,
        data: TradeInteractionData
    ) {
    }
}

class TraderChunkDisplayEntry(
    blockEntity: ClaimTraderBlockEntity,
    tooltip: List<Component> = listOf()
) : DisplayEntry(tooltip) {
    private val screen = TraderChunkScreen.get(blockEntity)!!

    override fun render(graphics: EasyGuiGraphics, x: Int, y: Int, data: DisplayData) {
        screen.draw(
            graphics.gui,
            ThemeManager.INSTANCE.activeTheme,
            (graphics as EasyGuiGraphicsAccessor).offset.x + x + 5,
            (graphics as EasyGuiGraphicsAccessor).offset.y + y + 19,
            84,
            84
        )
    }

    override fun isMouseOver(x: Int, y: Int, data: DisplayData, mouseX: Int, mouseY: Int): Boolean {
        val left: Int = x + data.xOffset()
        val top: Int = y + data.yOffset()
        return mouseX >= left &&
                mouseX < left + data.width() &&
                mouseY >= top &&
                mouseY < top + data.height()
    }
}

class TraderChunkScreen(private val blockEntity: ClaimTraderBlockEntity) : BaseScreen() {
    companion object {
        fun get(blockEntity: ClaimTraderBlockEntity): TraderChunkScreen? {
            val screen =
                MapDimension.getCurrent().map { TraderChunkScreen(blockEntity) }.getOrNull()

            if (screen == null) {
                ClaimShopForLightmansCurrency.LOGGER.warn(
                    "MapDimension data missing?? not opening chunk screen"
                )
            }

            return screen
        }
    }

    init {
        RenderMapImageTask.setAlwaysRenderChunksOnMap(true)
        MapManager.getInstance().ifPresent { it.updateAllRegions(false) }
    }

    override fun onInit(): Boolean {
        return setSizeProportional(0.32F, 0.32F)
    }

    override fun onClosed() {
        RenderMapImageTask.setAlwaysRenderChunksOnMap(false)
        MapManager.getInstance().ifPresent { it.updateAllRegions(false) }
        super.onClosed()
    }

    override fun addWidgets() {
        val chunkPos = ChunkPos(blockEntity.blockPos)
        RequestMapDataPacket(chunkPos.x - 2, chunkPos.z - 2, chunkPos.x + 2, chunkPos.z + 2)
            .sendToServer()
    }

    override fun drawBackground(
        graphics: GuiGraphics,
        theme: Theme,
        x: Int,
        y: Int,
        w: Int,
        h: Int
    ) {
        val player = Minecraft.getInstance().player!!
        RenderSystem.setShaderTexture(0, FTBChunksClient.INSTANCE.minimapTextureId)
        GuiHelper.drawTexturedRect(graphics, x, y, w, h, Color4I.WHITE, 0.0f, 0.0f, 1.0f, 1.0f)

        val hx = (x + w / 2 - 8).toDouble() + MathUtils.mod(player.x, 16.0)
        val hy = (y + h / 2 - 8).toDouble() + MathUtils.mod(player.z, 16.0)
        FaceIcon.getFace(player.gameProfile)
            .draw(graphics, (hx - 2).toInt(), (hy - 2).toInt(), 4, 4)
    }
}

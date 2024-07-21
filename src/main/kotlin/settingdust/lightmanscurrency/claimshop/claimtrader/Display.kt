package settingdust.lightmanscurrency.claimshop.claimtrader

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
import io.github.lightman314.lightmanscurrency.api.traders.trade.client.TradeRenderManager
import io.github.lightman314.lightmanscurrency.client.gui.widget.TradeButtonArea
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.icon.IconData
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.trade.AlertData
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.trade.DisplayData
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.trade.DisplayEntry
import io.github.lightman314.lightmanscurrency.client.gui.widget.button.trade.TradeButton
import io.github.lightman314.lightmanscurrency.client.util.IconAndButtonUtil
import io.github.lightman314.lightmanscurrency.client.util.ScreenArea
import io.github.lightman314.lightmanscurrency.client.util.ScreenPosition
import io.github.lightman314.lightmanscurrency.common.notifications.categories.TraderCategory
import io.github.lightman314.lightmanscurrency.common.notifications.types.TaxableNotification
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.Slot
import net.minecraft.world.level.ChunkPos
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import settingdust.lightmanscurrency.claimshop.ClaimShopForLightmansCurrency
import java.util.function.Function

class ClaimNotification : TaxableNotification {

    companion object {
        val TYPE =
            NotificationType(
                ClaimShopForLightmansCurrency.location("claim_trade"), ::ClaimNotification)

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
        TraderStorageClientTab<ClaimTradeEditTab>(screen, commonTab),
        TradeButtonArea.InteractionConsumer {

        lateinit var tradeDisplay: TradeButton
        lateinit var priceSelection: MoneyValueWidget

        override fun tabButtonVisible(): Boolean {
            return false
        }

        override fun blockInventoryClosing(): Boolean {
            return true
        }

        override fun getIcon(): IconData = IconAndButtonUtil.ICON_TRADELIST

        override fun getTooltip() = LCText.TOOLTIP_TRADER_EDIT_TRADES.get()

        override fun initialize(screenArea: ScreenArea, firstOpen: Boolean) {
            val trade = commonTab.getTrade()
            tradeDisplay = addChild(TradeButton(menu::getContext, { trade }) {})
            tradeDisplay.position = screenArea.pos.offset(10, 18)
            priceSelection =
                addChild(
                    MoneyValueWidget(
                        screenArea.pos.offset(15, 55),
                        if (firstOpen) null else priceSelection,
                        trade?.cost ?: MoneyValue.empty(),
                        ::onValueChanged))
            priceSelection.drawBG = false
        }

        override fun renderBG(gui: EasyGuiGraphics) {
            // TODO 渲染地图
        }

        override fun onTradeButtonInputInteraction(
            trader: TraderData,
            trade: TradeData,
            index: Int,
            mouseButton: Int
        ) {}

        fun onValueChanged(value: MoneyValue) = commonTab.setPrice(value)

        override fun onTradeButtonOutputInteraction(
            p0: TraderData?,
            p1: TradeData?,
            p2: Int,
            p3: Int
        ) {}

        override fun onTradeButtonInteraction(
            p0: TraderData?,
            p1: TradeData?,
            p2: Int,
            p3: Int,
            p4: Int
        ) {}
    }

    @OnlyIn(Dist.CLIENT) override fun createClientTab(screen: Any) = Display(screen, this)

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
class ClaimTradeButtonRenderer(trade: ClaimTradeData) : TradeRenderManager<ClaimTradeData>(trade) {
    override fun tradeButtonWidth(context: TradeContext) = 94

    override fun arrowPosition(context: TradeContext) = ScreenPosition.ofOptional(36, 1)!!

    override fun inputDisplayArea(context: TradeContext) = DisplayData(1, 1, 34, 16)

    override fun getInputDisplays(context: TradeContext) =
        mutableListOf(
            DisplayEntry.of(
                trade.cost,
                if (context.isStorageMode) LCText.TOOLTIP_TRADE_EDIT_PRICE.getAsList() else null))

    override fun outputDisplayArea(context: TradeContext) = DisplayData(58, 1, 34, 16)

    override fun getOutputDisplays(context: TradeContext) = mutableListOf<DisplayEntry>()

    override fun getAdditionalAlertData(context: TradeContext, alerts: MutableList<AlertData>) {}
}

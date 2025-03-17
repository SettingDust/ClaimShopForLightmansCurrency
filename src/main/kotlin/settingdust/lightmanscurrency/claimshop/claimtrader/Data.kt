package settingdust.lightmanscurrency.claimshop.claimtrader

import com.google.gson.JsonObject
import dev.ftb.mods.ftbchunks.api.ClaimResult.StandardProblem
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI
import dev.ftb.mods.ftblibrary.math.ChunkDimPos
import io.github.lightman314.lightmanscurrency.api.money.value.MoneyValue
import io.github.lightman314.lightmanscurrency.api.network.LazyPacketData
import io.github.lightman314.lightmanscurrency.api.ownership.builtin.PlayerOwner
import io.github.lightman314.lightmanscurrency.api.stats.StatKeys
import io.github.lightman314.lightmanscurrency.api.traders.TradeContext
import io.github.lightman314.lightmanscurrency.api.traders.TradeResult
import io.github.lightman314.lightmanscurrency.api.traders.TraderData
import io.github.lightman314.lightmanscurrency.api.traders.TraderType
import io.github.lightman314.lightmanscurrency.api.traders.menu.storage.ITraderStorageMenu
import io.github.lightman314.lightmanscurrency.api.traders.menu.storage.TraderStorageTab
import io.github.lightman314.lightmanscurrency.api.traders.permissions.PermissionOption
import io.github.lightman314.lightmanscurrency.api.traders.trade.TradeData
import io.github.lightman314.lightmanscurrency.api.traders.trade.TradeDirection
import io.github.lightman314.lightmanscurrency.api.traders.trade.client.TradeInteractionData
import io.github.lightman314.lightmanscurrency.api.traders.trade.comparison.TradeComparisonResult
import io.github.lightman314.lightmanscurrency.api.upgrades.UpgradeType
import io.github.lightman314.lightmanscurrency.common.menus.TraderMenu
import io.github.lightman314.lightmanscurrency.common.menus.traderstorage.core.BasicTradeEditTab
import io.github.lightman314.lightmanscurrency.common.util.IconData
import io.github.lightman314.lightmanscurrency.common.util.IconUtil
import io.github.lightman314.lightmanscurrency.util.InventoryUtil
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraftforge.server.ServerLifecycleHooks
import settingdust.lightmanscurrency.claimshop.ClaimShopForLightmansCurrency

data class ClaimTradeData(var pos: ChunkPos) : TradeData(true) {
    override fun getTradeDirection() = TradeDirection.SALE

    override fun getStock(context: TradeContext) = if (isValid) 1 else 0

    override fun compare(data: TradeData): TradeComparisonResult {
        ClaimShopForLightmansCurrency.LOGGER.warn(
            "Attempting to compare claim trades, but claim trades do not support this interaction."
        )
        return TradeComparisonResult()
    }

    override fun AcceptableDifferences(result: TradeComparisonResult): Boolean {
        ClaimShopForLightmansCurrency.LOGGER.warn(
            "Attempting to determine if the claim trades differences are acceptable, but claim trades do not support this interaction."
        )
        return false
    }

    override fun GetDifferenceWarnings(differences: TradeComparisonResult): List<Component> {
        ClaimShopForLightmansCurrency.LOGGER.warn(
            "Attempting to get warnings for different claim trades, but claim trades do not support this interaction."
        )
        return listOf()
    }

    override fun getButtonRenderer() = ClaimTradeButtonRenderer(this)

    override fun OnInputDisplayInteraction(
        tab: BasicTradeEditTab,
        index: Int,
        data: TradeInteractionData,
        heldItem: ItemStack
    ) {
        val traderData = tab.menu.trader
        if (traderData !is ClaimTraderData) return
        val tradeIndex = traderData.tradeData.indexOf(this)
        if (tradeIndex < 0) return
        tab.sendOpenTabMessage(
            TraderStorageTab.TAB_TRADE_ADVANCED, LazyPacketData.simpleInt("TradeIndex", tradeIndex)
        )
    }

    override fun OnOutputDisplayInteraction(
        tab: BasicTradeEditTab,
        index: Int,
        data: TradeInteractionData,
        heldItem: ItemStack
    ) {
        val traderData = tab.menu.trader
        if (traderData !is ClaimTraderData) return
        val tradeIndex = traderData.tradeData.indexOf(this)
        if (tradeIndex < 0) return
        tab.sendOpenTabMessage(
            TraderStorageTab.TAB_TRADE_ADVANCED, LazyPacketData.simpleInt("TradeIndex", tradeIndex)
        )
    }

    override fun OnInteraction(
        tab: BasicTradeEditTab,
        data: TradeInteractionData,
        heldItem: ItemStack
    ) {
        val traderData = tab.menu.trader
        if (traderData !is ClaimTraderData) return
        val tradeIndex = traderData.tradeData.indexOf(this)
        if (tradeIndex < 0) return
        tab.sendOpenTabMessage(
            TraderStorageTab.TAB_TRADE_ADVANCED, LazyPacketData.simpleInt("TradeIndex", tradeIndex)
        )
    }

    override fun getAsNBT(): CompoundTag {
        val tag = super.getAsNBT()
        tag.putLong("Pos", pos.toLong())
        return tag
    }

    public override fun loadFromNBT(nbt: CompoundTag) {
        super.loadFromNBT(nbt)
        pos = ChunkPos(nbt.getLong("Pos"))
    }
}

open class ClaimTraderData : TraderData {

    companion object {
        val TYPE = TraderType(ClaimShopForLightmansCurrency.location("claim")) { ClaimTraderData() }
    }

    val trade: ClaimTradeData = ClaimTradeData(ChunkPos(worldPosition.pos))

    constructor(level: Level, pos: BlockPos) : this(TYPE, level, pos)

    protected constructor(
        type: TraderType<ClaimTraderData>,
        level: Level,
        pos: BlockPos
    ) : super(type, level, pos)

    private constructor() : this(TYPE)

    protected constructor(type: TraderType<ClaimTraderData>) : super(type)

    override fun getIcon(): IconData = IconUtil.ICON_TRADER

    override fun allowAdditionalUpgradeType(p0: UpgradeType) = false

    override fun getTradeCount() = 1

    override fun getTradeStock(p0: Int) = 1

    override fun saveTrades(tag: CompoundTag) {
        trade.asNBT.let { tag.put("Trade", it) }
    }

    override fun saveAdditional(tag: CompoundTag) {
        saveTrades(tag)
    }

    override fun saveAdditionalToJson(json: JsonObject) {}

    override fun loadAdditional(tag: CompoundTag) {
        if ("Trade" in tag) {
            trade.loadFromNBT(tag.getCompound("Trade"))
        }
    }

    override fun loadAdditionalFromJson(json: JsonObject) {}

    override fun saveAdditionalPersistentData(p0: CompoundTag?) {}

    override fun loadAdditionalPersistentData(p0: CompoundTag?) {}

    override fun getAdditionalContents(p0: MutableList<ItemStack>?) {}

    override fun getTradeData() = listOf(trade)

    override fun getTrade(slot: Int) =
        if (slot == 0) trade
        else {
            ClaimShopForLightmansCurrency.LOGGER.warn(
                "Can't get trade in index $slot in claim trade since there is only one trade"
            )
            null
        }

    override fun addTrade(requestor: Player) {
        if (isClient) return
        ClaimShopForLightmansCurrency.LOGGER.warn(
            "${requestor.name} attempted to add trade slot of a claim trader that has only 1 trade"
        )
    }

    override fun removeTrade(requestor: Player) {
        if (isClient) return
        ClaimShopForLightmansCurrency.LOGGER.warn(
            "${requestor.name} attempted to remove trade slot of a claim trader that has only 1 trade"
        )
    }

    override fun ExecuteTrade(context: TradeContext, tradeIndex: Int): TradeResult {
        require(tradeIndex == 0)
        if (!trade.isValid) {
            ClaimShopForLightmansCurrency.LOGGER.error(
                "Trade $trade isn't a valid trade. Can't execute trade."
            )
            return TradeResult.FAIL_INVALID_TRADE
        }
        if (!context.hasPlayerReference()) {
            return TradeResult.FAIL_NULL
        }
        if (runPreTradeEvent(trade, context).isCanceled) {
            return TradeResult.FAIL_TRADE_RULE_DENIAL
        }
        val price = trade.getCost(context)

        val buyer = context.playerReference!!

        val commandSourceStack =
            (buyer.player?.createCommandSourceStack()
                ?: ServerLifecycleHooks.getCurrentServer().createCommandSourceStack())
        val chunkDimPos = ChunkDimPos(level, ChunkPos(pos))

        val sellerData =
            FTBChunksAPI.api().manager.getPersonalData(owner.playerForContext.id)
                ?: FTBChunksAPI.api()
                    .manager
                    .getPersonalData(ClaimShopForLightmansCurrency.FAKE_PROFILE.id)

        val buyerData = FTBChunksAPI.api().manager.getPersonalData(buyer.id)

        //        if (buyerData == sellerData) return TradeResult.FAIL_OUT_OF_STOCK

        sellerData.unclaim(commandSourceStack, chunkDimPos, false)

        val result =
            buyerData.claim(
                buyer.player?.createCommandSourceStack()
                    ?: ServerLifecycleHooks.getCurrentServer().createCommandSourceStack(),
                chunkDimPos,
                false
            )

        val fakeData =
            FTBChunksAPI.api().manager.getPersonalData(ClaimShopForLightmansCurrency.FAKE_PROFILE.id)

        if (!result.isSuccess) {
            buyerData.unclaim(commandSourceStack, chunkDimPos, false)
            fakeData.claim(commandSourceStack, chunkDimPos, false)
            buyer.player.sendSystemMessage(result.message)
            return when (result) {
                StandardProblem.NOT_ENOUGH_POWER -> {
                    TradeResult.FAIL_NO_OUTPUT_SPACE
                }

                else -> TradeResult.FAIL_INVALID_TRADE
            }
        }

        if (!context.getPayment(price)) {
            ClaimShopForLightmansCurrency.LOGGER.debug("No enough money for the trade {}.", trade)
            buyerData.unclaim(commandSourceStack, chunkDimPos, false)
            fakeData.claim(commandSourceStack, chunkDimPos, false)
            return TradeResult.FAIL_CANNOT_AFFORD
        }

        buyer.player.sendSystemMessage(ClaimShopForLightmansCurrency.Texts.MESSAGE_SUCCESSFUL_TRANSACTION.get(buyer.player.displayName))

        val taxesPaid =
            if (!isCreative) {
                addStoredMoney(price, true)
            } else MoneyValue.empty()

        this.incrementStat(StatKeys.Traders.MONEY_EARNED, price)

        if (!taxesPaid.isEmpty) this.incrementStat(StatKeys.Taxables.TAXES_PAID, taxesPaid)

        pushNotification {
            ClaimNotification(
                taxesPaid, trade, price, context.playerReference, this.notificationCategory
            )
        }

        this.runPostTradeEvent(trade, context, price, taxesPaid)

        owner.SetOwner(PlayerOwner.of(buyer))

        for (player in users) {
            val containerMenu = player.containerMenu
            if (containerMenu is TraderMenu && containerMenu.singleTrader == this) {
                player.closeContainer()
            }
        }

        ServerLifecycleHooks.getCurrentServer().submitAsync {
            val level = ServerLifecycleHooks.getCurrentServer().getLevel(level)!!
            ClaimTraderBlockEntity.CLAIM_TRADER.getBlockEntity(level, pos)!!.flagAsLegitBreak()
            InventoryUtil.dumpContents(
                level, pos, getContents(level, pos, level.getBlockState(pos), true)
            )
            level.destroyBlock(pos, true)
        }

        return TradeResult.SUCCESS
    }

    override fun canMakePersistent() = false

    override fun initStorageTabs(menu: ITraderStorageMenu) {
        menu.setTab(2, ClaimTradeEditTab(menu))
    }

    override fun addPermissionOptions(p0: MutableList<PermissionOption>?) {}
}

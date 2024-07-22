package settingdust.lightmanscurrency.claimshop

import com.mojang.authlib.GameProfile
import io.github.lightman314.lightmanscurrency.ModCreativeGroups
import io.github.lightman314.lightmanscurrency.api.traders.TraderAPI
import io.github.lightman314.lightmanscurrency.common.text.TextEntry
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import org.apache.logging.log4j.LogManager
import settingdust.lightmanscurrency.claimshop.claimtrader.ClaimNotification
import settingdust.lightmanscurrency.claimshop.claimtrader.ClaimTraderBlock
import settingdust.lightmanscurrency.claimshop.claimtrader.ClaimTraderBlockEntity
import settingdust.lightmanscurrency.claimshop.claimtrader.ClaimTraderData.Companion.TYPE
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import java.util.*

@Mod(ClaimShopForLightmansCurrency.ID)
object ClaimShopForLightmansCurrency {
    const val ID = "claim_shop_for_lightmans_currency"
    val LOGGER = LogManager.getLogger()!!

    val FAKE_PROFILE =
        GameProfile(UUID.fromString("c0a38ea7-6794-43e4-b7e5-19497b6390cc"), "[ClaimTrader]")

    init {
        requireNotNull(Registries)
    }

    fun location(path: String) = ResourceLocation(ID, path)

    @EventBusSubscriber(modid = ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    internal object Registries {
        val ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID)
        val BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID)
        val BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ID)

        init {
            requireNotNull(ClaimTraderBlock)
            requireNotNull(ClaimTraderBlockEntity)

            BLOCKS.register(MOD_BUS)
            BLOCK_ENTITIES.register(MOD_BUS)
            ITEMS.register(MOD_BUS)

            TraderAPI.registerTrader(TYPE)
        }

        @SubscribeEvent
        fun addToTab(event: BuildCreativeModeTabContentsEvent) {
            if (event.tabKey.location() == ModCreativeGroups.TRADER_GROUP_ID) {
                event.accept(ClaimTraderBlock.CLAIM_TRADER)
            }
        }
    }

    object Texts {
        val NOTIFICATION_TRADE_CLAIM: TextEntry = TextEntry.notification(ClaimNotification.TYPE)
        val NOTIFICATION_TRADE_CLAIM_NOT_OWNER: TextEntry = TextEntry.notification(ClaimNotification.TYPE, "not_owner")
        val MESSAGE_SUCCESSFUL_TRANSACTION = TextEntry.message(ID, "successful_transaction")
    }
}

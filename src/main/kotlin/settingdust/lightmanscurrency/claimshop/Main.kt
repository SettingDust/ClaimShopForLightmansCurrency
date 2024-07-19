package settingdust.lightmanscurrency.claimshop

import io.github.lightman314.lightmanscurrency.common.text.TextEntry
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import org.apache.logging.log4j.LogManager
import settingdust.lightmanscurrency.claimshop.claimtrader.ClaimNotification
import settingdust.lightmanscurrency.claimshop.claimtrader.ClaimTraderBlock
import settingdust.lightmanscurrency.claimshop.claimtrader.ClaimTraderBlockEntity

@Mod(ClaimShopForLightmansCurrency.ID)
object ClaimShopForLightmansCurrency {
    const val ID = "claim_shop_for_lightmans_currency"
    val LOGGER = LogManager.getLogger()!!

    init {
        requireNotNull(Blocks)
        requireNotNull(BlockEntities)
    }

    fun location(path: String) = ResourceLocation(ID, path)

    object Blocks {
        private val registry = DeferredRegister.create(ForgeRegistries.BLOCKS, ID)

        init {
            registry.register("claim_trader") { ClaimTraderBlock.CLAIM_TRADER }
        }
    }

    object BlockEntities {
        private val registry = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ID)

        init {
            registry.register("claim_trader") { ClaimTraderBlockEntity.CLAIM_TRADER }
        }
    }

    object Texts {
        val NOTIFICATION_TRADE_CLAIM: TextEntry = TextEntry.notification(ClaimNotification.TYPE)
    }
}

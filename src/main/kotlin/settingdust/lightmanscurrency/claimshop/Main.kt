package settingdust.lightmanscurrency.claimshop

import com.mojang.authlib.GameProfile
import io.github.lightman314.lightmanscurrency.common.text.TextEntry
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import org.apache.logging.log4j.LogManager
import settingdust.lightmanscurrency.claimshop.claimtrader.ClaimNotification
import settingdust.lightmanscurrency.claimshop.claimtrader.ClaimTraderBlock
import settingdust.lightmanscurrency.claimshop.claimtrader.ClaimTraderBlockEntity
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import java.util.*

@Mod(ClaimShopForLightmansCurrency.ID)
object ClaimShopForLightmansCurrency {
    const val ID = "claim_shop_for_lightmans_currency"
    val LOGGER = LogManager.getLogger()!!

    val FAKE_PROFILE = GameProfile(UUID.fromString("c0a38ea7-6794-43e4-b7e5-19497b6390cc"), "[ClaimTrader]");

    init {
        requireNotNull(Registries)
    }

    fun location(path: String) = ResourceLocation(ID, path)

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
        }
    }

    object Texts {
        val NOTIFICATION_TRADE_CLAIM: TextEntry = TextEntry.notification(ClaimNotification.TYPE)
    }
}

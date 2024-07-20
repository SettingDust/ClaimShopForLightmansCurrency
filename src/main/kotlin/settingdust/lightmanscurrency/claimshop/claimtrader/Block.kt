package settingdust.lightmanscurrency.claimshop.claimtrader

import io.github.lightman314.lightmanscurrency.api.traders.TraderData
import io.github.lightman314.lightmanscurrency.api.traders.blockentity.TraderBlockEntity
import io.github.lightman314.lightmanscurrency.api.traders.blocks.TraderBlockRotatable
import net.minecraft.core.BlockPos
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.MapColor
import settingdust.lightmanscurrency.claimshop.ClaimShopForLightmansCurrency
import thedarkcolour.kotlinforforge.forge.ObjectHolderDelegate
import thedarkcolour.kotlinforforge.forge.registerObject

class ClaimTraderBlock(properties: Properties) : TraderBlockRotatable(properties) {
    companion object {
        val CLAIM_TRADER by
            register(
                "claim_trader",
            ) {
                ClaimTraderBlock(
                    Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(5.0f, Float.POSITIVE_INFINITY)
                        .sound(SoundType.METAL))
            }

        private fun register(
            name: String,
            block: () -> ClaimTraderBlock
        ): ObjectHolderDelegate<ClaimTraderBlock> {
            val delegate = ClaimShopForLightmansCurrency.Registries.BLOCKS.registerObject(name, block)
            ClaimShopForLightmansCurrency.Registries.ITEMS.registerObject(name) {
                BlockItem(delegate(), Item.Properties())
            }
            return delegate
        }
    }

    override fun makeTrader(pos: BlockPos, state: BlockState) = ClaimTraderBlockEntity(pos, state)

    override fun traderType() = ClaimTraderBlockEntity.CLAIM_TRADER
}

class ClaimTraderBlockEntity(pos: BlockPos, state: BlockState) :
    TraderBlockEntity<ClaimTraderData>(CLAIM_TRADER, pos, state) {
    companion object {
        val CLAIM_TRADER by
            register("claim_trader") {
                BlockEntityType.Builder.of(::ClaimTraderBlockEntity, ClaimTraderBlock.CLAIM_TRADER)
                    .build(null)
            }

        private fun register(
            name: String,
            type: () -> BlockEntityType<ClaimTraderBlockEntity>
        ): ObjectHolderDelegate<BlockEntityType<ClaimTraderBlockEntity>> {
            return ClaimShopForLightmansCurrency.Registries.BLOCK_ENTITIES.registerObject(
                name, type)
        }
    }

    override fun buildNewTrader() = ClaimTraderData(level!!, worldPosition)

    override fun castOrNullify(data: TraderData) = data as? ClaimTraderData
}

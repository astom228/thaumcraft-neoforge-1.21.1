package thaumcraft.common.blocks.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.lib.utils.Utils;

/**
 * Ящик или урна с добычей (порт BlockLoot): разбивается с одного удара, выпадает 1+редкость+0–2 предметов из мешка
 * добычи этой редкости; с шёлковым касанием — сам блок.
 */
public class BlockLoot extends Block {
    public enum LootType {
        COMMON,
        UNCOMMON,
        RARE
    }

    private static final VoxelShape URN = Block.box(2.0, 1.0, 2.0, 14.0, 13.0, 14.0);
    private static final VoxelShape CRATE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);
    private final LootType type;
    private final boolean urn;

    public BlockLoot(LootType type, boolean urn, Properties properties) {
        super(properties);
        this.type = type;
        this.urn = urn;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return this.urn ? URN : CRATE;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
        if (tool != null && !tool.isEmpty()) {
            var silk = params.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
            if (tool.getEnchantmentLevel(silk) > 0) {
                return List.of(new ItemStack(this));
            }
        }
        var level = params.getLevel();
        List<ItemStack> ret = new ArrayList<>();
        int q = 1 + this.type.ordinal() + level.random.nextInt(3);
        for (int a = 0; a < q; a++) {
            ItemStack is = Utils.generateLoot(level, this.type.ordinal(), level.random);
            if (is != null && !is.isEmpty()) {
                ret.add(is.copy());
            }
        }
        return ret;
    }
}

package thaumcraft.api.research;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;

/**
 * Реестр сканируемого и само сканирование (порт ScanningManager). Список дополняется при запуске (и при загрузке реестра
 * зачарований), читается на обеих сторонах — поэтому CopyOnWriteArrayList.
 */
public class ScanningManager {
    static final List<IScanThing> things = new CopyOnWriteArrayList<>();

    public static void addScannableThing(IScanThing obj) {
        things.add(obj);
    }

    /** Сервер: отсканировать объект — открыть исследования подходящих IScanThing и сообщить игроку. */
    public static void scanTheThing(Player player, Object object) {
        boolean found = false;
        boolean suppress = false;
        for (IScanThing thing : things) {
            if (thing.checkThing(player, object)) {
                String key = thing.getResearchKey(player, object);
                if (key == null || key.isEmpty() || ThaumcraftApi.internalMethods.progressResearch(player, key)) {
                    if (key == null || key.isEmpty()) {
                        suppress = true;
                    }
                    found = true;
                    thing.onSuccess(player, object);
                }
            }
        }
        if (!suppress) {
            if (!found) {
                player.displayClientMessage(Component.translatable("tc.unknownobject").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
            } else {
                player.displayClientMessage(Component.translatable("tc.knownobject").withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC), true);
            }
        }
        if (object instanceof BlockPos pos) {
            IItemHandler handler = ThaumcraftApiHelper.getItemHandlerAt(player.level(), pos, Direction.UP);
            if (handler != null) {
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    ItemStack stack = handler.getStackInSlot(slot);
                    if (!stack.isEmpty()) {
                        scanTheThing(player, stack);
                    }
                }
            }
        }
    }

    /** Есть ли у объекта ещё не полученный игроком ключ (подсветка таумометра на клиенте). */
    public static boolean isThingStillScannable(Player player, Object object) {
        for (IScanThing thing : things) {
            if (thing.checkThing(player, object)) {
                try {
                    String key = thing.getResearchKey(player, object);
                    if (key != null && !ThaumcraftCapabilities.knowsResearch(player, key)) {
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return false;
    }

    /** Предмет, соответствующий объекту: стек, выброшенный предмет или блок (как «взять блок»; вода и лава — вёдра). */
    public static ItemStack getItemFromParms(Player player, Object obj) {
        ItemStack is = ItemStack.EMPTY;
        if (obj instanceof ItemStack stack) {
            is = stack;
        }
        if (obj instanceof ItemEntity ie) {
            is = ie.getItem();
        }
        if (obj instanceof BlockPos pos) {
            BlockState state = player.level().getBlockState(pos);
            try {
                is = state.getBlock().getCloneItemStack(player.level(), pos, state);
            } catch (Exception ignored) {
            }
            try {
                if (is.isEmpty()) {
                    is = state.getCloneItemStack(rayTrace(player), player.level(), pos, player);
                }
            } catch (Exception ignored) {
            }
            if (is.isEmpty() && state.getFluidState().is(FluidTags.WATER)) {
                is = new ItemStack(Items.WATER_BUCKET);
            }
            if (is.isEmpty() && state.getFluidState().is(FluidTags.LAVA)) {
                is = new ItemStack(Items.LAVA_BUCKET);
            }
        }
        return is;
    }

    private static HitResult rayTrace(Player player) {
        Vec3 vec3d = player.getEyePosition(0.0F);
        Vec3 vec3d1 = player.getViewVector(0.0F);
        Vec3 vec3d2 = vec3d.add(vec3d1.x * 4.0, vec3d1.y * 4.0, vec3d1.z * 4.0);
        BlockHitResult hit = player.level().clip(new ClipContext(vec3d, vec3d2, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));
        return hit;
    }
}

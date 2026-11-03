package thaumcraft.common.container;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import thaumcraft.common.entities.construct.EntityTurretCrossbowAdvanced;
import thaumcraft.common.lib.TCMenus;

/** Окно улучшенного арбалета (порт ContainerTurretAdvanced): кнопки 1–4 переключают цели (животные, монстры, игроки, свои). */
public class ContainerTurretAdvanced extends ContainerTurretBasic {
    public ContainerTurretAdvanced(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, (EntityTurretCrossbowAdvanced) inv.player.level().getEntity(buf.readVarInt()));
    }

    public ContainerTurretAdvanced(int id, Inventory inv, EntityTurretCrossbowAdvanced turret) {
        super(TCMenus.TURRET_ADVANCED.get(), id, inv, turret, 42);
    }

    public EntityTurretCrossbowAdvanced getAdvanced() {
        return (EntityTurretCrossbowAdvanced) this.turret;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        EntityTurretCrossbowAdvanced t = this.getAdvanced();
        if (!t.isOwner(player)) {
            return false;
        }
        switch (id) {
            case 1 -> t.setTargetAnimal(!t.getTargetAnimal());
            case 2 -> t.setTargetMob(!t.getTargetMob());
            case 3 -> t.setTargetPlayer(!t.getTargetPlayer());
            case 4 -> t.setTargetFriendly(!t.getTargetFriendly());
            default -> {
                return false;
            }
        }
        return true;
    }
}

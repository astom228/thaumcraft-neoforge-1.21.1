package thaumcraft.common.tiles.crafting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.casters.IInteractWithCaster;
import thaumcraft.api.crafting.IInfusionStabiliser;
import thaumcraft.api.crafting.IStabilizable;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.blocks.basic.BlockPillar;
import thaumcraft.common.blocks.devices.BlockPedestal;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.config.ConfigPotions;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.lib.events.EssentiaHandler;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.lib.network.fx.PacketFXBlockArc;
import thaumcraft.common.lib.network.fx.PacketFXInfusionSource;
import thaumcraft.common.tiles.TileThaumcraft;

/**
 * Руническая матрица (порт TileInfusionMatrix). Перчаткой: включить (если алтарь собран), затем запустить наполнение по
 * предметам на пьедесталах. Каждый цикл (cycleTime / 2 тиков) тянет единицу эссенции из банок в 12 блоках, затем по одному
 * забирает компоненты с пьедесталов; нестабильность рецепта снижает стабильность, симметрия пьедесталов и стабилизаторов —
 * повышает. При отрицательной стабильности возможны события (выброс предметов, Выброс, Искажение, молнии, вред, взрыв).
 * Всё состояние процесса сохраняется; сервер — единственный источник правды, клиенту уходят active, crafting, stability, эссенция.
 */
public class TileInfusionMatrix extends TileThaumcraft implements IInteractWithCaster, IAspectContainer, IStabilizable {
    private final ArrayList<BlockPos> pedestals = new ArrayList<>();
    private int dangerCount = 0;
    public boolean active = false;
    public boolean crafting = false;
    public boolean checkSurroundings = true;
    public float costMult = 0.0F;
    private int cycleTime = 20;
    public int stabilityCap = 25;
    public float stability = 0.0F;
    public float symmetryStability = 0.0F;
    private AspectList recipeEssentia = new AspectList();
    private ArrayList<ItemStack> recipeIngredients = null;
    private Object recipeOutput = null;
    private String recipePlayer = null;
    private String recipeOutputLabel = null;
    private ItemStack recipeInput = ItemStack.EMPTY;
    private int recipeInstability = 0;
    private int recipeXP = 0;
    private int recipeType = 0;
    /** Только клиент: источники частиц (PacketFXInfusionSource). */
    public final Map<String, SourceFX> sourceFX = new HashMap<>();
    public int count = 0;
    public int craftCount = 0;
    public float startUp;
    private int countDelay = this.cycleTime / 2;
    int itemCount = 0;

    public TileInfusionMatrix(BlockPos pos, BlockState state) {
        super(ConfigBlocks.INFUSION_MATRIX_TILE.get(), pos, state);
    }

    @Override
    public void readSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        this.active = nbt.getBoolean("active");
        this.crafting = nbt.getBoolean("crafting");
        this.stability = nbt.getFloat("stability");
        this.recipeEssentia = new AspectList();
        this.recipeEssentia.readFromNBT(nbt);
    }

    @Override
    public CompoundTag writeSyncNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        nbt.putBoolean("active", this.active);
        nbt.putBoolean("crafting", this.crafting);
        nbt.putFloat("stability", this.stability);
        this.recipeEssentia.writeToNBT(nbt);
        return nbt;
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        ListTag list = nbt.getList("recipein", Tag.TAG_COMPOUND);
        this.recipeIngredients = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            ItemStack s = ItemStack.parseOptional(registries, list.getCompound(i));
            if (!s.isEmpty()) {
                this.recipeIngredients.add(s);
            }
        }
        this.recipeOutput = null;
        this.recipeOutputLabel = null;
        String rot = nbt.getString("rotype");
        if (rot.equals("@")) {
            this.recipeOutput = ItemStack.parseOptional(registries, nbt.getCompound("recipeout"));
        } else if (rot.startsWith("c:")) {
            // пара {тип компонента, значение} (в 1.12 — {"имя NBT", значение})
            DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.tryParse(rot.substring(2)));
            if (type != null && type.codec() != null) {
                Object value = type.codec().parse(registries.createSerializationContext(NbtOps.INSTANCE), nbt.get("recipeout")).result().orElse(null);
                if (value != null) {
                    this.recipeOutput = new Object[] {type, value};
                    this.recipeOutputLabel = rot.substring(2);
                }
            }
        } else if (rot.startsWith("e:")) {
            this.recipeOutput = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT,
                    ResourceLocation.parse(rot.substring(2)));
        }
        this.recipeInput = ItemStack.parseOptional(registries, nbt.getCompound("recipeinput"));
        this.recipeInstability = nbt.getInt("recipeinst");
        this.recipeType = nbt.getInt("recipetype");
        this.recipeXP = nbt.getInt("recipexp");
        this.recipePlayer = nbt.getString("recipeplayer");
        if (this.recipePlayer.isEmpty()) {
            this.recipePlayer = null;
        }
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        // в оригинале при сохранении счётчик тиков count увеличивался на число компонентов — DEVIATIONS 34
        if (this.recipeIngredients != null && !this.recipeIngredients.isEmpty()) {
            ListTag list = new ListTag();
            for (ItemStack stack : this.recipeIngredients) {
                if (!stack.isEmpty()) {
                    list.add(stack.save(registries));
                }
            }
            nbt.put("recipein", list);
        }
        if (this.recipeOutput instanceof ItemStack is && !is.isEmpty()) {
            nbt.putString("rotype", "@");
            nbt.put("recipeout", is.save(registries));
        } else if (this.recipeOutput instanceof Object[] obj && obj.length == 2 && obj[0] instanceof DataComponentType type && type.codec() != null) {
            ResourceLocation id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
            type.codec().encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), obj[1]).result().ifPresent(t -> {
                nbt.putString("rotype", "c:" + id);
                nbt.put("recipeout", (Tag) t);
            });
        } else if (this.recipeOutput instanceof net.minecraft.resources.ResourceKey<?> key) {
            nbt.putString("rotype", "e:" + key.location());
        }
        if (!this.recipeInput.isEmpty()) {
            nbt.put("recipeinput", this.recipeInput.save(registries));
        }
        nbt.putInt("recipeinst", this.recipeInstability);
        nbt.putInt("recipetype", this.recipeType);
        nbt.putInt("recipexp", this.recipeXP);
        nbt.putString("recipeplayer", this.recipePlayer == null ? "" : this.recipePlayer);
    }

    @Override
    public void addStability(int multiples) {
        this.stability += 0.02F * multiples;
        if (this.stability > this.stabilityCap) {
            this.stability = this.stabilityCap;
        }
        this.setChanged();
        this.syncTile(false);
    }

    @Override
    public EnumStability getStability() {
        return this.stability > this.stabilityCap / 2 ? EnumStability.VERY_STABLE
                : (this.stability >= 0.0F ? EnumStability.STABLE : (this.stability > -25.0F ? EnumStability.UNSTABLE : EnumStability.VERY_UNSTABLE));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileInfusionMatrix tile) {
        tile.update();
    }

    void update() {
        this.count++;
        if (this.level.isClientSide) {
            this.doEffects();
            return;
        }
        // getSurroundings оригинала шёл и на клиенте, но там его результат не использовался
        if (this.checkSurroundings) {
            this.checkSurroundings = false;
            this.getSurroundings();
        }
        if (this.count % (this.crafting ? 20 : 100) == 0 && !this.validLocation()) {
            this.active = false;
            this.setChanged();
            this.syncTile(false);
            return;
        }
        if (!this.crafting && this.stability < 0.0F && this.count % 20 == 0) {
            this.stability++;
            if (this.stability > 0.0F) {
                this.stability = 0.0F;
            }
            this.setChanged();
            this.syncTile(false);
        }
        if (this.active && this.crafting && this.count % this.countDelay == 0) {
            this.craftCycle();
            this.setChanged();
        }
    }

    public boolean validLocation() {
        return this.level.getBlockState(this.worldPosition.offset(0, -2, 0)).getBlock() instanceof BlockPedestal
                && this.level.getBlockState(this.worldPosition.offset(1, -2, 1)).getBlock() instanceof BlockPillar
                && this.level.getBlockState(this.worldPosition.offset(-1, -2, 1)).getBlock() instanceof BlockPillar
                && this.level.getBlockState(this.worldPosition.offset(1, -2, -1)).getBlock() instanceof BlockPillar
                && this.level.getBlockState(this.worldPosition.offset(-1, -2, -1)).getBlock() instanceof BlockPillar;
    }

    private TilePedestal pedestalAt(BlockPos p) {
        return this.level.isLoaded(p) && this.level.getBlockEntity(p) instanceof TilePedestal ped ? ped : null;
    }

    public void craftingStart(Player player) {
        if (!this.validLocation()) {
            this.active = false;
            this.setChanged();
            this.syncTile(false);
            return;
        }
        this.getSurroundings();
        this.recipeInput = ItemStack.EMPTY;
        TilePedestal centre = this.pedestalAt(this.worldPosition.below(2));
        if (centre != null && !centre.getStackInSlot(0).isEmpty()) {
            this.recipeInput = centre.getStackInSlot(0).copy();
        }
        if (this.recipeInput.isEmpty()) {
            return;
        }
        ArrayList<ItemStack> components = new ArrayList<>();
        for (BlockPos cc : this.pedestals) {
            TilePedestal ped = this.pedestalAt(cc);
            if (ped != null && !ped.getStackInSlot(0).isEmpty()) {
                components.add(ped.getStackInSlot(0).copy());
            }
        }
        if (components.isEmpty()) {
            return;
        }
        InfusionRecipe recipe = ThaumcraftCraftingManager.findMatchingInfusionRecipe(components, this.recipeInput, player);
        if (this.costMult < 0.5) {
            this.costMult = 0.5F;
        }
        if (recipe != null) {
            this.recipeType = 0;
            this.recipeIngredients = components;
            Object out = recipe.getRecipeOutput(player, this.recipeInput, components);
            this.recipeOutput = out instanceof ItemStack is ? is.copy() : out;
            this.recipeOutputLabel = null;
            this.recipeInstability = recipe.getInstability(player, this.recipeInput, components);
            AspectList al = recipe.getAspects(player, this.recipeInput, components);
            AspectList al2 = new AspectList();
            for (Aspect as : al.getAspects()) {
                if ((int) (al.getAmount(as) * this.costMult) > 0) {
                    al2.add(as, (int) (al.getAmount(as) * this.costMult));
                }
            }
            this.recipeEssentia = al2;
            this.recipePlayer = player.getGameProfile().getName();
            this.crafting = true;
            this.level.playSound(null, this.worldPosition, SoundsTC.craftstart.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
            this.syncTile(false);
            this.setChanged();
        }
    }

    public void craftCycle() {
        boolean valid = false;
        this.stability = this.stability - this.recipeInstability / 5.0F;
        this.stability = this.stability + this.symmetryStability;
        if (this.stability < -100.0F) {
            this.stability = -100.0F;
        }
        if (this.stability > this.stabilityCap) {
            this.stability = this.stabilityCap;
        }
        TilePedestal centre = this.pedestalAt(this.worldPosition.below(2));
        if (centre != null && !centre.getStackInSlot(0).isEmpty()) {
            ItemStack i2 = centre.getStackInSlot(0).copy();
            if (ThaumcraftApiHelper.areItemStacksEqualForCrafting(i2, this.recipeInput)) {
                valid = true;
            }
        }
        var rand = this.level.random;
        if (!valid || this.stability < 0.0F && rand.nextInt(1000) <= Math.abs(this.stability)) {
            switch (rand.nextInt(24)) {
                case 0, 1, 2, 3 -> this.inEvEjectItem(0);
                case 4, 5, 6 -> this.inEvWarp();
                case 7, 8, 9 -> this.inEvZap(false);
                case 10, 11 -> this.inEvZap(true);
                case 12, 13 -> this.inEvEjectItem(1);
                case 14, 15 -> this.inEvEjectItem(2);
                case 16 -> this.inEvEjectItem(3);
                case 17 -> this.inEvEjectItem(4);
                case 18, 19 -> this.inEvHarm(false);
                case 20, 21 -> this.inEvEjectItem(5);
                case 22 -> this.inEvHarm(true);
                case 23 -> this.level.explode(null, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5,
                        1.5F + rand.nextFloat(), Level.ExplosionInteraction.NONE);
                default -> {
                }
            }
            this.stability = this.stability + (1.0F + rand.nextFloat() * 5.0F);
            this.inResAdd();
            if (valid) {
                return;
            }
        }
        if (!valid) {
            this.crafting = false;
            this.recipeEssentia = new AspectList();
            this.syncTile(false);
            this.level.playSound(null, this.worldPosition, SoundsTC.craftfail.get(), SoundSource.BLOCKS, 1.0F, 0.6F);
            this.setChanged();
        } else if (this.recipeType == 1 && this.recipeXP > 0) {
            List<Player> targets = this.level.getEntitiesOfClass(Player.class, new AABB(this.worldPosition).inflate(10.0));
            if (!targets.isEmpty()) {
                for (Player target : targets) {
                    if (target.getAbilities().instabuild || target.experienceLevel > 0) {
                        if (!target.getAbilities().instabuild) {
                            target.giveExperienceLevels(-1);
                        }
                        this.recipeXP--;
                        target.hurt(this.level.damageSources().magic(), rand.nextInt(2));
                        this.sendNear(new PacketFXInfusionSource(this.worldPosition, (byte) 0, (byte) 0, (byte) 0, target.getId()));
                        target.playNotifySound(SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 2.0F + rand.nextFloat() * 0.4F);
                        this.level.playSound(target, target.getX(), target.getY(), target.getZ(), SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 1.0F,
                                2.0F + rand.nextFloat() * 0.4F);
                        this.countDelay = this.cycleTime;
                        return;
                    }
                }
                Aspect[] ingEss = this.recipeEssentia.getAspects();
                if (ingEss != null && ingEss.length > 0 && rand.nextInt(3) == 0) {
                    Aspect as = ingEss[rand.nextInt(ingEss.length)];
                    this.recipeEssentia.add(as, 1);
                    this.stability -= 0.25F;
                    this.syncTile(false);
                    this.setChanged();
                }
            }
        } else {
            if (this.recipeType == 1 && this.recipeXP == 0) {
                this.countDelay = this.cycleTime / 2;
            }
            if (this.countDelay < 1) {
                this.countDelay = 1;
            }
            if (this.recipeEssentia.visSize() > 0) {
                for (Aspect aspect : this.recipeEssentia.getAspects()) {
                    int na = this.recipeEssentia.getAmount(aspect);
                    if (na > 0) {
                        if (EssentiaHandler.drainEssentia(this, aspect, null, 12, na > 1 ? this.countDelay : 0)) {
                            this.recipeEssentia.reduce(aspect, 1);
                            this.syncTile(false);
                            this.setChanged();
                            return;
                        }
                        this.stability -= 0.25F;
                        this.syncTile(false);
                        this.setChanged();
                    }
                }
                this.checkSurroundings = true;
            } else if (this.recipeIngredients == null || this.recipeIngredients.isEmpty()) {
                this.crafting = false;
                this.craftingFinish(this.recipeOutput, this.recipeOutputLabel);
                this.recipeOutput = null;
                this.syncTile(false);
                this.setChanged();
            } else {
                for (int a = 0; a < this.recipeIngredients.size(); a++) {
                    for (BlockPos cc : this.pedestals) {
                        TilePedestal ped = this.pedestalAt(cc);
                        if (ped != null && !ped.getStackInSlot(0).isEmpty()
                                && ThaumcraftApiHelper.areItemStacksEqualForCrafting(ped.getStackInSlot(0), this.recipeIngredients.get(a))) {
                            if (this.itemCount == 0) {
                                this.itemCount = 5;
                                this.sendNear(new PacketFXInfusionSource(this.worldPosition, (byte) (this.worldPosition.getX() - cc.getX()),
                                        (byte) (this.worldPosition.getY() - cc.getY()), (byte) (this.worldPosition.getZ() - cc.getZ()), 0));
                            } else if (this.itemCount-- <= 1) {
                                ItemStack is = ped.getStackInSlot(0).getCraftingRemainingItem();
                                ped.setInventorySlotContents(0, is.isEmpty() ? ItemStack.EMPTY : is.copy());
                                ped.setChanged();
                                ped.syncTile(false);
                                this.recipeIngredients.remove(a);
                                this.setChanged();
                            }
                            return;
                        }
                    }
                    Aspect[] ingEss = this.recipeEssentia.getAspects();
                    if (ingEss != null && ingEss.length > 0 && rand.nextInt(1 + a) == 0) {
                        Aspect as = ingEss[rand.nextInt(ingEss.length)];
                        this.recipeEssentia.add(as, 1);
                        this.stability -= 0.25F;
                        this.syncTile(false);
                        this.setChanged();
                    }
                }
            }
        }
    }

    /** Пакет эффекта — игрокам в 32 блоках от матрицы (TargetPoint оригинала). */
    private void sendNear(net.minecraft.network.protocol.common.custom.CustomPacketPayload msg) {
        this.sendNear(this.worldPosition, msg);
    }

    private void sendNear(BlockPos at, net.minecraft.network.protocol.common.custom.CustomPacketPayload msg) {
        if (this.level instanceof ServerLevel sl) {
            PacketDistributor.sendToPlayersNear(sl, null, at.getX(), at.getY(), at.getZ(), 32.0, msg);
        }
    }

    private AABB around() {
        return new AABB(this.worldPosition).inflate(10.0, 10.0, 10.0);
    }

    private void inEvZap(boolean all) {
        List<LivingEntity> targets = this.level.getEntitiesOfClass(LivingEntity.class, this.around());
        for (LivingEntity target : targets) {
            var rand = this.level.random;
            this.sendNear(new PacketFXBlockArc(this.worldPosition, target, 0.3F - rand.nextFloat() * 0.1F, 0.0F, 0.3F - rand.nextFloat() * 0.1F));
            target.hurt(this.level.damageSources().magic(), 4 + rand.nextInt(4));
            if (!all) {
                break;
            }
        }
    }

    private void inEvHarm(boolean all) {
        List<LivingEntity> targets = this.level.getEntitiesOfClass(LivingEntity.class, this.around());
        for (LivingEntity target : targets) {
            if (this.level.random.nextBoolean()) {
                target.addEffect(new MobEffectInstance(ConfigPotions.FLUX_TAINT, 120, 0, false, true));
            } else {
                MobEffectInstance pe = new MobEffectInstance(ConfigPotions.VIS_EXHAUST, 2400, 0, true, true);
                pe.getCures().clear();
                target.addEffect(pe);
            }
            if (!all) {
                break;
            }
        }
    }

    private void inResAdd() {
        List<Player> targets = this.level.getEntitiesOfClass(Player.class, new AABB(this.worldPosition).inflate(10.0));
        for (Player player : targets) {
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
            if (knowledge != null && !knowledge.isResearchKnown("!INSTABILITY")) {
                knowledge.addResearch("!INSTABILITY");
                if (player instanceof ServerPlayer sp) {
                    knowledge.sync(sp);
                }
                player.displayClientMessage(Component.translatable("got.instability").withStyle(ChatFormatting.DARK_PURPLE), true);
            }
        }
    }

    private void inEvWarp() {
        List<Player> targets = this.level.getEntitiesOfClass(Player.class, new AABB(this.worldPosition).inflate(10.0));
        if (!targets.isEmpty()) {
            Player target = targets.get(this.level.random.nextInt(targets.size()));
            if (this.level.random.nextFloat() < 0.25F) {
                ThaumcraftApi.internalMethods.addWarpToPlayer(target, 1, IPlayerWarp.EnumWarpType.NORMAL);
            } else {
                ThaumcraftApi.internalMethods.addWarpToPlayer(target, 2 + this.level.random.nextInt(4), IPlayerWarp.EnumWarpType.TEMPORARY);
            }
        }
    }

    private void inEvEjectItem(int type) {
        for (int q = 0; q < 50 && !this.pedestals.isEmpty(); q++) {
            BlockPos cc = this.pedestals.get(this.level.random.nextInt(this.pedestals.size()));
            TilePedestal ped = this.pedestalAt(cc);
            if (ped != null && !ped.getStackInSlot(0).isEmpty()) {
                if (type >= 3 && type != 5) {
                    ped.setInventorySlotContents(0, ItemStack.EMPTY);
                } else {
                    ped.dropContents();
                    ped.setInventorySlotContents(0, ItemStack.EMPTY);
                }
                ped.setChanged();
                ped.syncTile(false);
                if (type == 1 || type == 3) {
                    // TODO этап 15: флюксовая слизь над пьедесталом (BlocksTC.fluxGoo); пока — её Выброс в ауру (DECISIONS 93, DEVIATIONS 35)
                    AuraHelper.polluteAura(this.level, cc.above(), 7, true);
                    this.level.playSound(null, cc, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.3F, 1.0F);
                } else if (type == 2 || type == 4) {
                    int a = 5 + this.level.random.nextInt(5);
                    AuraHelper.polluteAura(this.level, cc, a, true);
                } else if (type == 5) {
                    this.level.explode(null, cc.getX() + 0.5F, cc.getY() + 0.5F, cc.getZ() + 0.5F, 1.0F, Level.ExplosionInteraction.NONE);
                }
                this.level.blockEvent(cc, this.level.getBlockState(cc).getBlock(), 11, 0);
                var rand = this.level.random;
                this.sendNear(cc, new PacketFXBlockArc(this.worldPosition, cc.above(), 0.3F - rand.nextFloat() * 0.1F, 0.0F, 0.3F - rand.nextFloat() * 0.1F));
                return;
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void craftingFinish(Object out, String label) {
        TilePedestal te = this.pedestalAt(this.worldPosition.below(2));
        if (te == null) {
            return;
        }
        if (out instanceof ItemStack os) {
            ItemStack qs = os.copy();
            ItemStack cur = te.getStackInSlot(0);
            if (cur.isDamageableItem() && cur.isDamaged()) {
                float dmg = (float) cur.getDamageValue() / cur.getMaxDamage();
                if (qs.isDamageableItem() && !qs.isDamaged()) {
                    qs.setDamageValue((int) (qs.getMaxDamage() * dmg));
                }
            }
            te.setInventorySlotContentsFromInfusion(0, qs);
        } else if (out instanceof Object[] obj && obj.length == 2 && obj[0] instanceof DataComponentType type) {
            ItemStack temp = te.getStackInSlot(0).copy();
            temp.set(type, obj[1]);
            te.setInventorySlotContentsFromInfusion(0, temp);
            this.syncTile(false);
        } else if (out instanceof net.minecraft.resources.ResourceKey<?> key) {
            ItemStack temp = te.getStackInSlot(0).copy();
            Holder<Enchantment> ench = this.level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .getHolderOrThrow((net.minecraft.resources.ResourceKey<Enchantment>) key);
            EnchantmentHelper.updateEnchantments(temp, m -> m.set(ench, m.getLevel(ench) + 1));
            te.setInventorySlotContentsFromInfusion(0, temp);
            this.syncTile(false);
        }
        if (this.recipePlayer != null && this.level.getServer() != null) {
            Player p = this.level.getServer().getPlayerList().getPlayerByName(this.recipePlayer);
            if (p != null) {
                SimpleContainer inv = new SimpleContainer(Math.max(1, this.recipeIngredients == null ? 1 : this.recipeIngredients.size()));
                EventHooks.firePlayerCraftingEvent(p, te.getStackInSlot(0), inv);
            }
        }
        this.recipeEssentia = new AspectList();
        this.syncTile(false);
        this.setChanged();
        this.level.blockEvent(this.worldPosition.below(2), this.level.getBlockState(this.worldPosition.below(2)).getBlock(), 12, 0);
        this.level.playSound(null, this.worldPosition, SoundsTC.wand.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
    }

    private static boolean isStabiliser(Level world, BlockPos testPos, BlockPos stabPos) {
        Block bi = world.getBlockState(testPos).getBlock();
        try {
            return bi instanceof AbstractSkullBlock || bi instanceof IInfusionStabiliser st && st.canStabaliseInfusion(world, stabPos);
        } catch (Exception e) {
            return false;
        }
    }

    /** Пьедесталы (до 8 по горизонтали, ниже матрицы) и стабилизаторы (до 12), стоимость, скорость и симметрия. */
    private void getSurroundings() {
        ArrayList<BlockPos> stuff = new ArrayList<>();
        this.pedestals.clear();
        for (int xx = -12; xx <= 12; xx++) {
            for (int zz = -12; zz <= 12; zz++) {
                boolean skip = false;
                for (int yy = -5; yy <= 10; yy++) {
                    if (xx != 0 || zz != 0) {
                        BlockPos bp = new BlockPos(this.worldPosition.getX() + xx, this.worldPosition.getY() - yy, this.worldPosition.getZ() + zz);
                        if (!this.level.isLoaded(bp)) {
                            continue;
                        }
                        BlockEntity te = this.level.getBlockEntity(bp);
                        if (!skip && yy > 0 && Math.abs(xx) <= 8 && Math.abs(zz) <= 8 && te instanceof TilePedestal) {
                            this.pedestals.add(bp);
                            skip = true;
                        } else if (isStabiliser(this.level, bp, bp)) {
                            stuff.add(bp);
                        }
                    }
                }
            }
        }
        this.cycleTime = 10;
        this.symmetryStability = 0.0F;
        this.costMult = 1.0F;
        Block p1 = this.level.getBlockState(this.worldPosition.offset(-1, -2, -1)).getBlock();
        Block p2 = this.level.getBlockState(this.worldPosition.offset(1, -2, -1)).getBlock();
        Block p3 = this.level.getBlockState(this.worldPosition.offset(1, -2, 1)).getBlock();
        Block p4 = this.level.getBlockState(this.worldPosition.offset(-1, -2, 1)).getBlock();
        if (p1 instanceof BlockPillar && p2 instanceof BlockPillar && p3 instanceof BlockPillar && p4 instanceof BlockPillar) {
            Block anc = BlocksTC.pillarAncient.get();
            Block eld = BlocksTC.pillarEldritch.get();
            if (p1 == anc && p2 == anc && p3 == anc && p4 == anc) {
                this.cycleTime--;
                this.costMult -= 0.1F;
                this.symmetryStability -= 0.1F;
            }
            if (p1 == eld && p2 == eld && p3 == eld && p4 == eld) {
                this.cycleTime -= 3;
                this.costMult += 0.05F;
                this.symmetryStability += 0.2F;
            }
        }
        int[] xm = new int[] {-1, 1, 1, -1};
        int[] zm = new int[] {-1, -1, 1, 1};
        for (int a = 0; a < 4; a++) {
            Block b = this.level.getBlockState(this.worldPosition.offset(xm[a], -3, zm[a])).getBlock();
            if (b == BlocksTC.matrixSpeed.get()) {
                this.cycleTime--;
                this.costMult += 0.01F;
            }
            if (b == BlocksTC.matrixCost.get()) {
                this.cycleTime++;
                this.costMult -= 0.02F;
            }
        }
        this.countDelay = this.cycleTime / 2;
        for (BlockPos cc : this.pedestals) {
            boolean items = false;
            int x = this.worldPosition.getX() - cc.getX();
            int z = this.worldPosition.getZ() - cc.getZ();
            BlockState ccState = this.level.getBlockState(cc);
            if (this.level.getBlockEntity(cc) instanceof TilePedestal ped) {
                this.symmetryStability -= 0.1F;
                if (ccState.is(BlocksTC.pedestalEldritch.get())) {
                    this.symmetryStability -= 0.1F;
                }
                if (!ped.getStackInSlot(0).isEmpty()) {
                    this.symmetryStability -= 0.1F;
                    items = true;
                }
                if (ccState.is(BlocksTC.pedestalEldritch.get())) {
                    this.costMult += 0.0025F;
                }
                if (ccState.is(BlocksTC.pedestalAncient.get())) {
                    this.costMult -= 0.01F;
                }
            }
            BlockPos cc2 = new BlockPos(this.worldPosition.getX() + x, cc.getY(), this.worldPosition.getZ() + z);
            if (this.level.isLoaded(cc2) && this.level.getBlockEntity(cc2) instanceof TilePedestal ped2 && this.level.getBlockState(cc2) == ccState) {
                this.symmetryStability += 0.1F;
                if (this.level.getBlockState(cc2).is(BlocksTC.pedestalEldritch.get())) {
                    this.symmetryStability += 0.1F;
                }
                if (!ped2.getStackInSlot(0).isEmpty() && items) {
                    this.symmetryStability += 0.1F;
                }
            }
        }
        float sym = 0.0F;
        for (BlockPos cc : stuff) {
            int x = this.worldPosition.getX() - cc.getX();
            int z = this.worldPosition.getZ() - cc.getZ();
            if (isStabiliser(this.level, cc, cc)) {
                sym += 0.1F;
            }
            BlockPos mirror = new BlockPos(this.worldPosition.getX() + x, cc.getY(), this.worldPosition.getZ() + z);
            // как в оригинале, у зеркальной клетки canStabaliseInfusion спрашивается для исходной позиции
            if (this.level.isLoaded(mirror) && isStabiliser(this.level, mirror, cc)) {
                sym -= 0.115F;
            }
        }
        this.symmetryStability = this.symmetryStability - Math.max(sym, -0.15F);
    }

    @Override
    public boolean onCasterRightClick(Level world, ItemStack wandstack, Player player, BlockPos pos, Direction side, InteractionHand hand) {
        if (!world.isClientSide && this.active && !this.crafting) {
            this.craftingStart(player);
            return false;
        }
        if (!world.isClientSide && !this.active && this.validLocation()) {
            world.playSound(null, pos, SoundsTC.craftstart.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
            this.active = true;
            this.syncTile(false);
            this.setChanged();
            return false;
        }
        return false;
    }

    /** Клиент: звуки, руны над пьедесталом, частицы от источников, молнии при нестабильности, разгон/остановка колец. */
    private void doEffects() {
        var rand = this.level.random;
        if (this.crafting) {
            if (this.craftCount == 0) {
                this.level.playLocalSound(this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), SoundsTC.infuserstart.get(),
                        SoundSource.BLOCKS, 0.5F, 1.0F, false);
            } else if (this.craftCount % 65 == 0) {
                this.level.playLocalSound(this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), SoundsTC.infuser.get(),
                        SoundSource.BLOCKS, 0.5F, 1.0F, false);
            }
            this.craftCount++;
            IFXDispatcher.get().blockRunes(this.worldPosition.getX(), this.worldPosition.getY() - 2, this.worldPosition.getZ(), 0.5F + rand.nextFloat() * 0.2F,
                    0.1F, 0.7F + rand.nextFloat() * 0.3F, 25, -0.03F);
        } else if (this.craftCount > 0) {
            this.craftCount -= 2;
            if (this.craftCount < 0) {
                this.craftCount = 0;
            }
            if (this.craftCount > 50) {
                this.craftCount = 50;
            }
        }
        if (this.active && this.startUp != 1.0F) {
            if (this.startUp < 1.0F) {
                this.startUp = this.startUp + Math.max(this.startUp / 10.0F, 0.001F);
            }
            if (this.startUp > 0.999) {
                this.startUp = 1.0F;
            }
        }
        if (!this.active && this.startUp > 0.0F) {
            this.startUp = this.startUp - this.startUp / 10.0F;
            if (this.startUp < 0.001) {
                this.startUp = 0.0F;
            }
        }
        for (String fxk : this.sourceFX.keySet().toArray(new String[0])) {
            SourceFX fx = this.sourceFX.get(fxk);
            if (fx.ticks <= 0) {
                this.sourceFX.remove(fxk);
                continue;
            }
            if (fx.loc.equals(this.worldPosition)) {
                Entity player = this.level.getEntity(fx.color);
                if (player != null) {
                    for (int a = 0; a < 4; a++) {
                        IFXDispatcher.get().drawInfusionParticles4(player.getX() + (rand.nextFloat() - rand.nextFloat()) * player.getBbWidth(),
                                player.getBoundingBox().minY + rand.nextFloat() * player.getBbHeight(),
                                player.getZ() + (rand.nextFloat() - rand.nextFloat()) * player.getBbWidth(), this.worldPosition.getX(),
                                this.worldPosition.getY(), this.worldPosition.getZ());
                    }
                }
            } else if (this.level.getBlockEntity(fx.loc) instanceof TilePedestal ped) {
                ItemStack is = ped.getStackInSlot(0);
                if (!is.isEmpty()) {
                    if (rand.nextInt(3) == 0) {
                        IFXDispatcher.get().drawInfusionParticles3(fx.loc.getX() + rand.nextFloat(), fx.loc.getY() + rand.nextFloat() + 1.0F,
                                fx.loc.getZ() + rand.nextFloat(), this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ());
                    } else if (is.getItem() instanceof BlockItem bi) {
                        for (int a = 0; a < 4; a++) {
                            IFXDispatcher.get().drawInfusionParticles2(fx.loc.getX() + rand.nextFloat(), fx.loc.getY() + rand.nextFloat() + 1.0F,
                                    fx.loc.getZ() + rand.nextFloat(), this.worldPosition, bi.getBlock().defaultBlockState());
                        }
                    } else {
                        for (int a = 0; a < 4; a++) {
                            IFXDispatcher.get().drawInfusionParticles1(fx.loc.getX() + 0.4F + rand.nextFloat() * 0.2F,
                                    fx.loc.getY() + 1.23F + rand.nextFloat() * 0.2F, fx.loc.getZ() + 0.4F + rand.nextFloat() * 0.2F, this.worldPosition, is);
                        }
                    }
                }
            } else {
                fx.ticks = 0;
            }
            fx.ticks--;
        }
        if (this.crafting && this.stability < 0.0F && rand.nextInt(200) <= Math.abs(this.stability)) {
            float xx = this.worldPosition.getX() + 0.5F + (rand.nextFloat() - rand.nextFloat()) * 4.0F;
            float zz = this.worldPosition.getZ() + 0.5F + (rand.nextFloat() - rand.nextFloat()) * 4.0F;
            int yy = this.worldPosition.getY() - 2;
            while (!this.level.isEmptyBlock(BlockPos.containing(xx, yy, zz)) && yy < this.level.getMaxBuildHeight()) {
                yy++;
            }
            IFXDispatcher.get().arcLightning(this.worldPosition.getX() + 0.5F, this.worldPosition.getY() + 0.5F, this.worldPosition.getZ() + 0.5F, xx, yy, zz,
                    0.8F, 0.1F, 1.0F, 0.1F);
        }
    }

    @Override
    public AspectList getAspects() {
        return this.recipeEssentia;
    }

    @Override
    public void setAspects(AspectList aspects) {
    }

    @Override
    public int addToContainer(Aspect tag, int amount) {
        return 0;
    }

    @Override
    public boolean takeFromContainer(Aspect tag, int amount) {
        return false;
    }

    @Override
    public boolean takeFromContainer(AspectList ot) {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect tag, int amount) {
        return false;
    }

    @Override
    public boolean doesContainerContain(AspectList ot) {
        return false;
    }

    @Override
    public int containerContains(Aspect tag) {
        return 0;
    }

    @Override
    public boolean doesContainerAccept(Aspect tag) {
        return true;
    }

    /** Источник частиц (клиент). color — у пьедестала 0, у игрока — id существа. */
    public static class SourceFX {
        public BlockPos loc;
        public int ticks;
        public int color;
        public int entity;

        public SourceFX(BlockPos loc, int ticks, int color) {
            this.loc = loc;
            this.ticks = ticks;
            this.color = color;
        }
    }
}

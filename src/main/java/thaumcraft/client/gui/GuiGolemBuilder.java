package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.api.golems.parts.GolemAddon;
import thaumcraft.api.golems.parts.GolemArm;
import thaumcraft.api.golems.parts.GolemHead;
import thaumcraft.api.golems.parts.GolemLeg;
import thaumcraft.api.golems.parts.GolemMaterial;
import thaumcraft.client.gui.plugins.GuiHoverButton;
import thaumcraft.client.gui.plugins.GuiScrollButton;
import thaumcraft.common.container.ContainerGolemBuilder;
import thaumcraft.common.entities.construct.golem.GolemProperties;
import thaumcraft.common.entities.construct.golem.gui.GuiGolemCraftButton;
import thaumcraft.common.lib.network.misc.PacketStartGolemCraftToServer;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.tiles.crafting.TileGolemBuilder;

/**
 * Окно пресса големов (порт GuiGolemBuilder): выбор материала, головы, рук, ног и дополнения из изученных стрелками;
 * черты, здоровье/броня/урон, нужные детали (недостающие — затемнены) и стоимость Machina; кнопка «собрать». Пока
 * пресс работает, выбор заблокирован. Выбранные номера деталей запоминаются между открытиями (только клиент).
 */
public class GuiGolemBuilder extends AbstractContainerScreen<ContainerGolemBuilder> {
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_golembuilder.png");
    static final ResourceLocation MAT_ICON = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/items/golem.png");
    static int headIndex = 0;
    static int matIndex = 0;
    static int armIndex = 0;
    static int legIndex = 0;
    static int addonIndex = 0;
    private final TileGolemBuilder builder;
    private final Player player;
    final List<GolemHead> valHeads = new ArrayList<>();
    final List<GolemMaterial> valMats = new ArrayList<>();
    final List<GolemArm> valArms = new ArrayList<>();
    final List<GolemLeg> valLegs = new ArrayList<>();
    final List<GolemAddon> valAddons = new ArrayList<>();
    IGolemProperties props = GolemProperties.fromLong(0L);
    float hearts = 0.0F;
    float armor = 0.0F;
    float damage = 0.0F;
    GuiGolemCraftButton craftButton = null;
    int cost = 0;
    boolean allfound = false;
    ItemStack[] components = null;
    boolean[] owns = null;
    boolean disableAll = false;
    final List<AbstractWidget> compButtons = new ArrayList<>();

    public GuiGolemBuilder(ContainerGolemBuilder menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.player = inv.player;
        this.builder = menu.getTile();
        this.imageWidth = 208;
        this.imageHeight = 224;
    }

    @Override
    protected void init() {
        super.init();
        this.valHeads.clear();
        for (GolemHead head : GolemHead.getHeads()) {
            if (ThaumcraftCapabilities.knowsResearchStrict(this.player, head.research)) {
                this.valHeads.add(head);
            }
        }
        this.valMats.clear();
        for (GolemMaterial mat : GolemMaterial.getMaterials()) {
            if (ThaumcraftCapabilities.knowsResearchStrict(this.player, mat.research)) {
                this.valMats.add(mat);
            }
        }
        this.valArms.clear();
        for (GolemArm arm : GolemArm.getArms()) {
            if (ThaumcraftCapabilities.knowsResearchStrict(this.player, arm.research)) {
                this.valArms.add(arm);
            }
        }
        this.valLegs.clear();
        for (GolemLeg leg : GolemLeg.getLegs()) {
            if (ThaumcraftCapabilities.knowsResearchStrict(this.player, leg.research)) {
                this.valLegs.add(leg);
            }
        }
        this.valAddons.clear();
        for (GolemAddon addon : GolemAddon.getAddons()) {
            if (ThaumcraftCapabilities.knowsResearchStrict(this.player, addon.research)) {
                this.valAddons.add(addon);
            }
        }
        if (headIndex >= this.valHeads.size()) {
            headIndex = 0;
        }
        if (matIndex >= this.valMats.size()) {
            matIndex = 0;
        }
        if (armIndex >= this.valArms.size()) {
            armIndex = 0;
        }
        if (legIndex >= this.valLegs.size()) {
            legIndex = 0;
        }
        if (addonIndex >= this.valAddons.size()) {
            addonIndex = 0;
        }
        this.gatherInfo();
    }

    private static int cycle(int index, int size, int delta) {
        index += delta;
        if (index < 0) {
            index = size - 1;
        }
        if (index >= size) {
            index = 0;
        }
        return index;
    }

    private void gatherInfo() {
        this.clearWidgets();
        this.compButtons.clear();
        int gl = this.leftPos;
        int gt = this.topPos;
        this.craftButton = this.addRenderableWidget(new GuiGolemCraftButton(gl + 120, gt + 104, this::craft));
        if (this.valHeads.size() > 1) {
            this.addRenderableWidget(new GuiScrollButton(gl + 112 - 5 - 6, gt - 5 + 16 + 8, 10, 10, true, () -> this.select(0, -1)));
            this.addRenderableWidget(new GuiScrollButton(gl + 112 - 5 + 22, gt - 5 + 16 + 8, 10, 10, false, () -> this.select(0, 1)));
        }
        if (this.valMats.size() > 1) {
            this.addRenderableWidget(new GuiScrollButton(gl + 16 - 5 - 6, gt - 5 + 16 + 8, 10, 10, true, () -> this.select(1, -1)));
            this.addRenderableWidget(new GuiScrollButton(gl + 16 - 5 + 22, gt - 5 + 16 + 8, 10, 10, false, () -> this.select(1, 1)));
        }
        if (this.valArms.size() > 1) {
            this.addRenderableWidget(new GuiScrollButton(gl + 112 - 5 - 6, gt - 5 + 40 + 8, 10, 10, true, () -> this.select(2, -1)));
            this.addRenderableWidget(new GuiScrollButton(gl + 112 - 5 + 22, gt - 5 + 40 + 8, 10, 10, false, () -> this.select(2, 1)));
        }
        if (this.valLegs.size() > 1) {
            this.addRenderableWidget(new GuiScrollButton(gl + 112 - 5 - 6, gt - 5 + 64 + 8, 10, 10, true, () -> this.select(3, -1)));
            this.addRenderableWidget(new GuiScrollButton(gl + 112 - 5 + 22, gt - 5 + 64 + 8, 10, 10, false, () -> this.select(3, 1)));
        }
        if (this.valAddons.size() > 1) {
            this.addRenderableWidget(new GuiScrollButton(gl + 16 - 5 - 6, gt - 5 + 64 + 8, 10, 10, true, () -> this.select(4, -1)));
            this.addRenderableWidget(new GuiScrollButton(gl + 16 - 5 + 22, gt - 5 + 64 + 8, 10, 10, false, () -> this.select(4, 1)));
        }
        if (this.valHeads.isEmpty() || this.valMats.isEmpty() || this.valArms.isEmpty() || this.valLegs.isEmpty() || this.valAddons.isEmpty()) {
            this.craftButton.active = false;
            return;
        }
        GolemHead head = this.valHeads.get(headIndex);
        GolemMaterial mat = this.valMats.get(matIndex);
        GolemArm arm = this.valArms.get(armIndex);
        GolemLeg leg = this.valLegs.get(legIndex);
        GolemAddon addon = this.valAddons.get(addonIndex);
        this.addRenderableWidget(new GuiHoverButton(gl + 120, gt + 24, 16, 16, head.getLocalizedName(), head.getLocalizedDescription(), head.icon));
        this.addRenderableWidget(new GuiHoverButton(gl + 24, gt + 24, 16, 16, mat.getLocalizedName(), mat.getLocalizedDescription(), MAT_ICON, mat.itemColor));
        this.addRenderableWidget(new GuiHoverButton(gl + 120, gt + 48, 16, 16, arm.getLocalizedName(), arm.getLocalizedDescription(), arm.icon));
        this.addRenderableWidget(new GuiHoverButton(gl + 120, gt + 72, 16, 16, leg.getLocalizedName(), leg.getLocalizedDescription(), leg.icon));
        if (!addon.key.equalsIgnoreCase("none")) {
            this.addRenderableWidget(new GuiHoverButton(gl + 24, gt + 72, 16, 16, addon.getLocalizedName(), addon.getLocalizedDescription(), addon.icon));
        }
        this.props = GolemProperties.fromLong(0L);
        this.props.setHead(head);
        this.props.setMaterial(mat);
        this.props.setArms(arm);
        this.props.setLegs(leg);
        this.props.setAddon(addon);
        this.redoComps();
        EnumGolemTrait[] tags = this.props.getTraits().toArray(new EnumGolemTrait[0]);
        if (tags.length > 0) {
            int yy = tags.length <= 4 ? (tags.length - 1) % 4 * 8 : 24;
            int xx = (tags.length - 1) / 4 % 4 * 8;
            int i = 0;
            int q = 0;
            for (EnumGolemTrait tag : tags) {
                this.addRenderableWidget(new GuiHoverButton(gl + 72 + q * 16 - xx, gt + 48 + 16 * i - yy, 16, 16, tag.getLocalizedName(),
                        tag.getLocalizedDescription(), tag.icon));
                if (++i > 3) {
                    i = 0;
                    q++;
                }
            }
        }
        int hh = 10 + mat.healthMod;
        if (this.props.hasTrait(EnumGolemTrait.FRAGILE)) {
            hh = (int) (hh * 0.75);
        }
        this.hearts = hh / 2.0F;
        int aa = mat.armor;
        if (this.props.hasTrait(EnumGolemTrait.ARMORED)) {
            aa = (int) Math.max(aa * 1.5, aa + 1);
        }
        if (this.props.hasTrait(EnumGolemTrait.FRAGILE)) {
            aa = (int) (aa * 0.75);
        }
        this.armor = aa / 2.0F;
        double dd = this.props.hasTrait(EnumGolemTrait.FIGHTER) ? mat.damage : 0.0;
        if (this.props.hasTrait(EnumGolemTrait.BRUTAL)) {
            dd = Math.max(dd * 1.5, dd + 1.0);
        }
        this.damage = (float) (dd / 2.0);
        this.updateEnabled();
    }

    private void select(int which, int delta) {
        switch (which) {
            case 0 -> headIndex = cycle(headIndex, this.valHeads.size(), delta);
            case 1 -> matIndex = cycle(matIndex, this.valMats.size(), delta);
            case 2 -> armIndex = cycle(armIndex, this.valArms.size(), delta);
            case 3 -> legIndex = cycle(legIndex, this.valLegs.size(), delta);
            default -> addonIndex = cycle(addonIndex, this.valAddons.size(), delta);
        }
        this.gatherInfo();
    }

    /** Детали состава и их наличие у игрока; значки деталей пересоздаются. */
    private void redoComps() {
        for (AbstractWidget w : this.compButtons) {
            this.removeWidget(w);
        }
        this.compButtons.clear();
        this.allfound = true;
        this.cost = this.props.getTraits().size() * 2;
        this.components = this.props.generateComponents();
        this.owns = new boolean[this.components.length];
        for (int a = 0; a < this.components.length; a++) {
            this.cost += this.components[a].getCount();
            this.owns[a] = InventoryUtils.isPlayerCarryingAmount(this.player, this.components[a], true);
            if (!this.owns[a]) {
                this.allfound = false;
            }
        }
        if (this.components.length > 0) {
            int gl = this.leftPos;
            int gt = this.topPos;
            this.compButtons.add(this.addRenderableWidget(new GuiHoverButton(gl + 152, gt + 24, 16, 16, Component.literal(Aspect.MECHANISM.getName()),
                    Aspect.MECHANISM.getLocalizedDescription(), Aspect.MECHANISM)));
            int i = 1;
            int q = 0;
            for (ItemStack stack : this.components) {
                this.compButtons.add(this.addRenderableWidget(new GuiHoverButton(gl + 152 + q * 16, gt + 24 + 16 * i, 16, 16, stack.getHoverName(), null, stack)));
                if (++i > 3) {
                    i = 0;
                    q++;
                }
            }
        }
    }

    private void updateEnabled() {
        for (GuiEventListener l : this.children()) {
            if (l instanceof AbstractWidget w) {
                w.active = !this.disableAll;
            }
        }
        if (!this.disableAll && this.craftButton != null) {
            this.craftButton.active = this.allfound;
        }
    }

    private void craft() {
        if (this.allfound && !this.disableAll) {
            PacketDistributor.sendToServer(new PacketStartGolemCraftToServer(this.builder.getBlockPos(), this.props.toLong()));
            this.disableAll = true;
            this.updateEnabled();
        }
    }

    /** Наличие деталей пересчитывается раз в тик (в оригинале — по щелчку в окне через общий флаг redo). */
    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.components != null) {
            boolean all = true;
            for (int a = 0; a < this.components.length; a++) {
                this.owns[a] = InventoryUtils.isPlayerCarryingAmount(this.player, this.components[a], true);
                all &= this.owns[a];
            }
            if (all != this.allfound) {
                this.allfound = all;
                this.updateEnabled();
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        super.render(g, mouseX, mouseY, partialTicks);
        for (GuiEventListener l : this.children()) {
            if (l instanceof GuiHoverButton b && b.isMouseOver(mouseX, mouseY)) {
                b.drawButtonForegroundLayer(this, g, mouseX, mouseY);
                break;
            }
        }
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEX, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if (this.components != null && this.components.length > 0) {
            int i = 1;
            int q = 0;
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
            for (int a = 0; a < this.components.length; a++) {
                if (!this.owns[a]) {
                    g.blit(TEX, this.leftPos + 144 + q * 16, this.topPos + 16 + 16 * i, 240, 0, 16, 16);
                }
                if (++i > 3) {
                    i = 0;
                    q++;
                }
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        if (this.builder.cost > 0) {
            if (this.builder.maxCost > 0) {
                g.blit(TEX, this.leftPos + 145, this.topPos + 89, 209, 89, (int) (46.0F * (1.0F - (float) this.builder.cost / this.builder.maxCost)), 6);
            }
            if (!this.disableAll) {
                this.disableAll = true;
                this.updateEnabled();
            }
        } else if (this.disableAll) {
            this.disableAll = false;
            this.redoComps();
            this.updateEnabled();
        }
        g.drawCenteredString(this.font, "" + this.hearts, this.leftPos + 48, this.topPos + 108, 16777215);
        g.drawCenteredString(this.font, "" + this.armor, this.leftPos + 72, this.topPos + 108, 16777215);
        g.drawCenteredString(this.font, "" + this.damage, this.leftPos + 97, this.topPos + 108, 16777215);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        if (this.components != null && this.components.length > 0) {
            g.drawString(this.font, "" + this.cost, 162 - this.font.width("" + this.cost), 24, 16777215);
        }
        RenderSystem.enableBlend();
        g.blit(TEX, 12, 12, 228, 124, 24, 24);
        g.blit(TEX, 12, 60, 228, 124, 24, 24);
        g.blit(TEX, 108, 12, 228, 124, 24, 24);
        g.blit(TEX, 108, 36, 228, 124, 24, 24);
        g.blit(TEX, 108, 60, 228, 124, 24, 24);
    }
}

package thaumcraft.api.crafting;

/**
 * Клетка схемы многоблочной структуры (порт Part). source — что должно стоять: Block, BlockState, ItemStack (блок предмета)
 * или TagKey&lt;Fluid&gt; (вместо Material жидкости 1.12); null — что угодно. target — во что превратить: Block (блок с
 * горизонтальной стороной поворачивается по структуре или к игроку), BlockState, ItemStack (не-блок появляется парящим),
 * строка "AIR" — убрать; null — не трогать. opp — сторона против структуры; priority — задержка превращения в тиках.
 */
public class Part {
    private Object source;
    private Object target;
    private boolean opp;
    private int priority;
    private boolean applyPlayerFacing;

    public Part(Object source, Object target, boolean opp, int priority) {
        this.setSource(source);
        this.setTarget(target);
        this.setOpp(opp);
        this.setPriority(priority);
    }

    public Part(Object source, Object target, boolean opp) {
        this(source, target, opp, 50);
    }

    public Part(Object source, Object target) {
        this(source, target, false, 50);
    }

    public Object getSource() {
        return this.source;
    }

    public void setSource(Object source) {
        this.source = source;
    }

    public Object getTarget() {
        return this.target;
    }

    public void setTarget(Object target) {
        this.target = target;
    }

    public boolean isOpp() {
        return this.opp;
    }

    public void setOpp(boolean opp) {
        this.opp = opp;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean getApplyPlayerFacing() {
        return this.applyPlayerFacing;
    }

    public Part setApplyPlayerFacing(boolean applyFacing) {
        this.applyPlayerFacing = applyFacing;
        return this;
    }
}

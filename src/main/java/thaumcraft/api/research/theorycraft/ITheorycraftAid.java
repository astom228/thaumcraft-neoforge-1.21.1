package thaumcraft.api.research.theorycraft;

/**
 * Помощник теоретизирования: предмет рядом со столом исследований (блок, предмет блока или класс существа в радиусе),
 * добавляющий в колоду свои карточки.
 */
public interface ITheorycraftAid {
    /** Block, ItemStack (предмет блока) или Class существа. */
    Object getAidObject();

    Class<? extends TheorycraftCard>[] getCards();
}

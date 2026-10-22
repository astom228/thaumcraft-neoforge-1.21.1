package thaumcraft.api.aspects;

/** Блок или предмет, хранящий аспекты (тигель, банки, тауматориум…). */
public interface IAspectContainer {
    AspectList getAspects();

    void setAspects(AspectList aspects);

    boolean doesContainerAccept(Aspect tag);

    int addToContainer(Aspect tag, int amount);

    boolean takeFromContainer(Aspect tag, int amount);

    @Deprecated
    boolean takeFromContainer(AspectList ot);

    boolean doesContainerContainAmount(Aspect tag, int amount);

    @Deprecated
    boolean doesContainerContain(AspectList ot);

    int containerContains(Aspect tag);
}

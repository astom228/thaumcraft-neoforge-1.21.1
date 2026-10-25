package thaumcraft.api.aspects;

/** Хранилище эссенции, из которого устройства (тауматориум, матрица наполнения…) могут брать её без труб. */
public interface IAspectSource extends IAspectContainer {
    boolean isBlocked();
}

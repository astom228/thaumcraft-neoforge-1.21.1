package thaumcraft.api;

import thaumcraft.api.internal.IInternalMethodHandler;
import thaumcraft.common.lib.InternalMethodHandler;

/** Порт ThaumcraftApi (содержимое добавляется по этапам). */
public class ThaumcraftApi {
    /** В оригинале задаётся в CommonProxy.preInit. */
    public static IInternalMethodHandler internalMethods = new InternalMethodHandler();
}

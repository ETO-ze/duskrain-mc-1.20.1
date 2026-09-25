package cn.duskrain;

/** No optional API types here: clients without JourneyMap keep the original information HUD. */
public final class ClientMapHooks {
    static Runnable toggle;
    static java.util.function.Supplier<java.util.Map<String,Object>> diagnostics=java.util.Map::of;
    static boolean installed(){return net.minecraftforge.fml.ModList.get().isLoaded("journeymap");}
}

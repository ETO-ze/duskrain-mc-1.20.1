package cn.duskrain;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(DuskRain.ID)
public final class DuskRain {
    public static final String ID = "duskrain";
    public static final Logger LOG = LogUtils.getLogger();
    public DuskRain() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        Decor.BLOCKS.register(bus);
        Ascension.init();MysticEnchants.init();MysticEnchants.REGISTRY.register(bus);
        Outfits.init();
        Boosts.init();
        Content.ITEMS.register(bus);
        Content.TABS.register(bus);
        CityGenerator.GENERATORS.register(bus);
        SwordFlight.init();TrialBoss.ENTITIES.register(bus);
        Resident.TYPE.getId();
        bus.addListener(TrialBoss::attributes);
        bus.addListener(Resident::attributes);
        Network.init();
        MinecraftForge.EVENT_BUS.register(new Gameplay());
        MinecraftForge.EVENT_BUS.register(new Skills());MinecraftForge.EVENT_BUS.register(new Guilds());
        MinecraftForge.EVENT_BUS.register(new Outfits());MinecraftForge.EVENT_BUS.register(new MysticEnchants());MinecraftForge.EVENT_BUS.register(new SwordFlight());
        MinecraftForge.EVENT_BUS.register(new Protection());
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(this::started);
        MinecraftForge.EVENT_BUS.addListener(this::stopping);
        MinecraftForge.EVENT_BUS.addListener(SkyBridges::loaded);
        MinecraftForge.EVENT_BUS.addListener(SkyBridges::tick);
    }
    private void commands(RegisterCommandsEvent e) { DRCommands.register(e.getDispatcher()); }
    private void started(ServerStartedEvent e) {
        Rules.load();CombatRules.load();Boosts.load();AscensionRules.load();Onboarding.prepare(e.getServer());
        Transactions.recover(e.getServer());Trials.recover(e.getServer());Guilds.recover(e.getServer());CityUpgrade.install(e.getServer());
        LOG.info("DuskRain 2.1.0 preview | group 205255670 | started");
    }
    private void stopping(ServerStoppingEvent e) {
        SkyBridges.reset(e.getServer());
        WildernessTravel.reset();CityMobility.READY.clear();
        Trials.shutdown(e.getServer());
        Store.get(e.getServer()).setDirty();
    }
}

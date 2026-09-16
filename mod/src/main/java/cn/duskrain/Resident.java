package cn.duskrain;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.RegistryObject;

/** Original human appearance with persistent villager service behavior and bindings. */
public final class Resident extends Villager {
    public static final RegistryObject<EntityType<Resident>> TYPE=TrialBoss.ENTITIES.register("resident",()->EntityType.Builder.<Resident>of(Resident::new,MobCategory.MISC).sized(.6f,1.8f).clientTrackingRange(10).build("duskrain:resident"));
    public Resident(EntityType<? extends Villager> type,Level level){super(type,level);}
    public static void attributes(EntityAttributeCreationEvent e){e.put(TYPE.get(),Villager.createAttributes().build());}
}

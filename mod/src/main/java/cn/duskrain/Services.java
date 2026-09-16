package cn.duskrain;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.Vec3;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Stable, explicit service bindings. Ambient entities never grant economic authority. */
public final class Services {
    public record Service(String id,String name,String building,String action,VillagerProfession profession,int offset) {
        public Service(String id,String name,String building,String action,VillagerProfession profession){this(id,name,building,action,profession,0);}
        public CityPlan.Building site(){return CityPlan.find(building);}
        public BlockPos counter(){var b=site();return new BlockPos(b.x()+CityPlan.counterX(b)+offset,b.y()+1,b.z()+b.d()-3);}
        public Vec3 position(){return Vec3.atBottomCenterOf(counter().north());}
        public UUID uuid(){return UUID.nameUUIDFromBytes(("duskrain:merchant:"+id).getBytes(StandardCharsets.UTF_8));}
    }
    public static final List<Service> ALL=List.of(
        new Service("enchanter","云篆 · 铭灵师","enchanter","enchant",VillagerProfession.LIBRARIAN),
        new Service("materials","沈掌柜 · 材料铺","market","shop view material",VillagerProfession.FARMER),
        new Service("pills","青萝 · 丹师","alchemy","shop view alchemy",VillagerProfession.CLERIC),
        new Service("elixirs","白芷 · 灵药师","alchemy","shop view elixir",VillagerProfession.CLERIC,-10),
        new Service("treasures","琳琅 · 珍宝使","market","shop view treasure",VillagerProfession.LIBRARIAN,-10),
        new Service("artifacts","墨衡 · 器师","forge","shop view forge",VillagerProfession.WEAPONSMITH),
        new Service("master","松玄 · 传道师","study","profile",VillagerProfession.LIBRARIAN),
        new Service("quests","闻溪 · 执事","quests","quests",VillagerProfession.CARTOGRAPHER),
        new Service("trial","渡云 · 守阵人","trial","trial",VillagerProfession.CLERIC),
        new Service("guild","云岑 · 宗门使","guildhall","guild",VillagerProfession.CARTOGRAPHER),
        new Service("travel","归舟 · 云渡使","warp","warp",VillagerProfession.FISHERMAN));
    public static Service byEntity(Entity e){return ALL.stream().filter(s->e instanceof Villager&&s.uuid().equals(e.getUUID())&&(e.level().isClientSide||e.getTags().contains("duskrain_service"))).findFirst().orElse(null);}
    public static Service byBlock(BlockPos p){return ALL.stream().filter(s->s.counter().equals(p)).findFirst().orElse(null);}
    public static Service find(String id){return ALL.stream().filter(s->s.id.equals(id)).findFirst().orElse(null);}
    public static String shopService(String category){return switch(category){case "material"->"materials";case "alchemy"->"pills";case "forge"->"artifacts";case "elixir"->"elixirs";case "treasure"->"treasures";default->"";};}
    public static boolean nearby(ServerPlayer p,String id){Service s=find(id);return !SwordFlight.flying(p)&&s!=null&&p.level().dimension().equals(Gameplay.CITY)&&p.distanceToSqr(s.position())<=36&&p.serverLevel().getBlockState(s.counter()).is(net.minecraft.world.level.block.Blocks.BARREL)&&Skills.lineClear(p.serverLevel(),p.getEyePosition(),s.position().add(0,1.5,0),p);}
    public static boolean require(ServerPlayer p,String id){if(nearby(p,id))return true;Service s=find(id);Gameplay.say(p,s==null?"此服务暂不可用。":"请前往"+s.site().name()+"，在"+s.name+"附近办理；右键村民或柜台即可打开。");return false;}
    public static boolean open(ServerPlayer p,Service s){if(s==null||!require(p,s.id))return false;
        Gameplay.progress(p,"talk",s.id);if(s.id.equals("quests"))Gameplay.progress(p,"investigate","rumor");
        p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack(),"dr "+s.action);return true;}
    public static void tick(MinecraftServer server){if(server.getTickCount()%40!=0)return;ServerLevel l=server.getLevel(Gameplay.CITY);if(l==null)return;
        for(Service s:ALL){BlockPos pos=s.counter();if(!l.hasChunk(pos.getX()>>4,pos.getZ()>>4))continue;Entity old=l.getEntity(s.uuid());
            if(old instanceof Villager&&!(old instanceof Resident)){old.discard();old=null;}
            if(old==null){Villager v=new Resident(Resident.TYPE.get(),l);v.setUUID(s.uuid());v.setVillagerData(new VillagerData(VillagerType.PLAINS,s.profession,3));v.setOffers(new MerchantOffers());v.setNoAi(true);v.setInvulnerable(true);v.setPersistenceRequired();v.setCustomName(Component.literal(s.name));v.setCustomNameVisible(true);v.addTag("duskrain_service");v.moveTo(s.position().x,s.position().y,s.position().z,0,0);l.addFreshEntity(v);old=v;}
            if(old instanceof Villager v){if(v.position().distanceToSqr(s.position())>.2)v.teleportTo(s.position().x,s.position().y,s.position().z);var p=l.getNearestPlayer(v,6);if(p!=null){float yaw=(float)(Math.toDegrees(Math.atan2(p.getZ()-v.getZ(),p.getX()-v.getX()))-90);v.setYHeadRot(yaw);v.setYRot(yaw);}}
        }
        var b=CityPlan.find("arena");UUID id=UUID.nameUUIDFromBytes("duskrain:practice".getBytes(StandardCharsets.UTF_8));
        if(l.hasChunk(b.x()>>4,b.z()>>4)&&l.getEntity(id)==null){ArmorStand dummy=new ArmorStand(l,b.x()+.5,b.y()+1,b.z()+.5);dummy.setUUID(id);dummy.setNoGravity(true);dummy.setInvulnerable(true);dummy.setCustomName(Component.literal("试剑木人 · 可练习所有流派"));dummy.setCustomNameVisible(true);dummy.addTag("duskrain_dummy");dummy.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));dummy.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.LEATHER_CHESTPLATE));l.addFreshEntity(dummy);}
    }
}

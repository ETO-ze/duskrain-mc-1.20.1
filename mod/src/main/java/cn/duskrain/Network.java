package cn.duskrain;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;
import java.util.function.Supplier;

public final class Network {
    public static final String PROTOCOL="7";
    public static final int MAX_MENU_ENTRIES=60;
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(DuskRain.ID,"main"),()->PROTOCOL,PROTOCOL::equals,PROTOCOL::equals);
    public record Snapshot(String player,int stage,int school,int xp,int needed,int mana,int manaMax,long money,String region,boolean pvp,int online,boolean visible,float scale,int main,int progress,int cooldown0,int cooldown1,int cooldown2,int combo) {
        public Snapshot(String player,int stage,int school,int xp,int needed,int mana,int manaMax,long money,String region,boolean pvp,int online,boolean visible,float scale){this(player,stage,school,xp,needed,mana,manaMax,money,region,pvp,online,visible,scale,0,0,0,0,0,0);}
    }
    public record Entry(String label,String action,String detail,String icon,boolean enabled) {
        public Entry(String label,String action){this(label,action,"","",true);}
    }
    public record Catalog(String category,String kind,int realm,String part,int page,int pages,int total) {}
    public record Menu(String title,String description,List<Entry> entries,Catalog catalog) {
        public Menu(String title,String description,List<Entry> entries){this(title,description,entries,null);}
    }
    public record Action(Actions.Kind kind,String target,int value,long sequence) {}
    public record Recording(boolean on) {}
    public record Notice(String text) {}
    public record EmptySwing() {}
    public record CityJumpImpulse(float vertical) {}
    static final Map<UUID,Long> LAST_ACTION=new HashMap<>();
    public static void init(){V14Network.init(CHANNEL);
        CHANNEL.registerMessage(8,CityJumpImpulse.class,(m,b)->b.writeFloat(m.vertical),b->new CityJumpImpulse(b.readFloat()),(m,c)->{c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ClientUI.cityJump(m.vertical)));c.get().setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(7,ClaimBorders.Snapshot.class,ClaimBorders::write,ClaimBorders::read,(m,c)->{c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ClientClaimBorders.receive(m)));c.get().setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(6,EmptySwing.class,(m,b)->{},b->new EmptySwing(),(m,c)->{var ctx=c.get();ctx.enqueueWork(()->{if(ctx.getSender()!=null)Skills.normalSwing(ctx.getSender(),-1);});ctx.setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(5,SpellFx.Event.class,SpellFx::write,SpellFx::read,(m,c)->{c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ClientSpellFx.receive(m)));c.get().setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(4,Notice.class,(m,b)->b.writeUtf(m.text,2048),b->new Notice(b.readUtf(2048)),(m,c)->{c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ClientUI.notice(m.text)));c.get().setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(3,Recording.class,(m,b)->b.writeBoolean(m.on),b->new Recording(b.readBoolean()),(m,c)->{c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->Capture.start(m.on)));c.get().setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(0,Snapshot.class,Network::writeSnapshot,Network::readSnapshot,(m,c)->{c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ClientUI.snapshot=m));c.get().setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1,Menu.class,Network::writeMenu,Network::readMenu,(m,c)->{c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ClientUI.open(m)));c.get().setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(2,Action.class,(m,b)->{b.writeEnum(m.kind);b.writeUtf(m.target,128);b.writeInt(m.value);b.writeLong(m.sequence);},b->new Action(b.readEnum(Actions.Kind.class),b.readUtf(128),b.readInt(),b.readLong()),(m,c)->{var ctx=c.get();ctx.enqueueWork(()->{if(ctx.getSender()!=null)Actions.receive(ctx.getSender(),m);});ctx.setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
    static void writeSnapshot(Snapshot m,FriendlyByteBuf b){b.writeUtf(m.player);b.writeVarInt(m.stage);b.writeVarInt(m.school);b.writeVarInt(m.xp);b.writeVarInt(m.needed);b.writeVarInt(m.mana);b.writeVarInt(m.manaMax);b.writeLong(m.money);b.writeUtf(m.region);b.writeBoolean(m.pvp);b.writeVarInt(m.online);b.writeBoolean(m.visible);b.writeFloat(m.scale);b.writeInt(m.main);b.writeInt(m.progress);b.writeInt(m.cooldown0);b.writeInt(m.cooldown1);b.writeInt(m.cooldown2);b.writeInt(m.combo);}
    static Snapshot readSnapshot(FriendlyByteBuf b){return new Snapshot(b.readUtf(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readLong(),b.readUtf(),b.readBoolean(),b.readVarInt(),b.readBoolean(),b.readFloat(),b.readInt(),b.readInt(),b.readInt(),b.readInt(),b.readInt(),b.readInt());}
    public static void sync(ServerPlayer p){if(p instanceof net.minecraftforge.common.util.FakePlayer)return;Profile r=Store.of(p);CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Snapshot(p.getGameProfile().getName(),r.stage,r.school,r.xp,Rules.current.stageXp[r.stage],r.mana,Skills.manaMax(r),r.money,Gameplay.region(p),Protection.pvpAllowed(p),p.server.getPlayerCount(),r.sidebar,r.sidebarScale,r.main,r.mainProgress,cooldown(p,0),cooldown(p,1),cooldown(p,2),Skills.COMBO.getOrDefault(p.getUUID(),0)));}
    static int cooldown(ServerPlayer p,int i){return (int)Math.max(0,(Gameplay.COOLDOWNS.getOrDefault(p.getUUID(),new long[3])[i]-System.currentTimeMillis()+999)/1000);}
    static void validate(Menu m){
        if(m.entries.size()>MAX_MENU_ENTRIES||m.title.length()>128||m.description.length()>4096)throw new IllegalArgumentException("Menu limits exceeded");
        for(var e:m.entries)if(e.label.length()>256||e.action.length()>256||e.detail.length()>1024||e.icon.length()>128)throw new IllegalArgumentException("Menu entry limits exceeded");
        if(m.catalog!=null&&(m.entries.size()>ShopCatalog.PAGE_SIZE||m.catalog.page<0||m.catalog.pages<1||m.catalog.page>=m.catalog.pages||m.catalog.total<0))throw new IllegalArgumentException("Catalog limits exceeded");
    }
    static void writeMenu(Menu m,FriendlyByteBuf b){
        validate(m);b.writeUtf(m.title,128);b.writeUtf(m.description,4096);b.writeVarInt(m.entries.size());
        for(var e:m.entries){b.writeUtf(e.label,256);b.writeUtf(e.action,256);b.writeUtf(e.detail,1024);b.writeUtf(e.icon,128);b.writeBoolean(e.enabled);}
        b.writeBoolean(m.catalog!=null);if(m.catalog!=null){var q=m.catalog;b.writeUtf(q.category,32);b.writeUtf(q.kind,16);b.writeInt(q.realm);b.writeUtf(q.part,16);b.writeInt(q.page);b.writeInt(q.pages);b.writeInt(q.total);}
    }
    static Menu readMenu(FriendlyByteBuf b){
        String title=b.readUtf(128),desc=b.readUtf(4096);int n=b.readVarInt();if(n<0||n>MAX_MENU_ENTRIES)throw new IllegalArgumentException("Menu size");
        List<Entry> es=new ArrayList<>();for(int i=0;i<n;i++)es.add(new Entry(b.readUtf(256),b.readUtf(256),b.readUtf(1024),b.readUtf(128),b.readBoolean()));
        Catalog q=b.readBoolean()?new Catalog(b.readUtf(32),b.readUtf(16),b.readInt(),b.readUtf(16),b.readInt(),b.readInt(),b.readInt()):null;
        Menu result=new Menu(title,desc,es,q);validate(result);return result;
    }
    public static void menu(ServerPlayer p,String title,String description,List<Entry> entries){sendMenu(p,new Menu(title,description,entries));}
    public static void sendMenu(ServerPlayer p,Menu menu){
        try{validate(menu);}catch(IllegalArgumentException ex){DuskRain.LOG.error("Rejected oversized/invalid menu {}",menu.title,ex);menu=new Menu("内容暂不可用","这份菜单超出限制，服务器已记录问题；你的连接和物品不会受影响。",List.of());}
        if(p instanceof net.minecraftforge.common.util.FakePlayer)return;CHANNEL.send(PacketDistributor.PLAYER.with(()->p),menu);
    }
    public static void action(String command){try{CHANNEL.sendToServer(Actions.parse(command));}catch(IllegalArgumentException ex){DuskRain.LOG.warn("Invalid UI action: {}",command);}}
    public static void capture(ServerPlayer p,boolean on){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Recording(on));}
    public static void notice(ServerPlayer p,String text){if(!(p instanceof net.minecraftforge.common.util.FakePlayer))CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Notice(text));}
}

package cn.duskrain;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.display.DisplayType;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.event.MappingEvent;
import journeymap.api.v2.client.model.MapPolygon;
import journeymap.api.v2.client.model.ShapeProperties;
import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.common.event.ClientEventRegistry;
import journeymap.api.v2.common.waypoint.WaypointFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import java.util.List;

/** JourneyMap alone discovers this class. The mod also loads without JourneyMap installed. */
@JourneyMapPlugin(apiVersion="2.0.0")
public final class DuskRainJourneyMap implements IClientPlugin {
    private IClientAPI api;
    private boolean mapping;
    private int ticks;
    private long borders=-1;
    private String dimension="";

    @Override public String getModId(){return DuskRain.ID;}
    @Override public void initialize(IClientAPI value){
        api=value;
        ClientMapHooks.toggle=()->{api.toggleMinimap(!api.minimapEnabled());ClientUI.notice(api.minimapEnabled()?"山河图已展开 · J 全屏地图 · F8 收起":"山河图已收起 · F8 展开");};
        ClientEventRegistry.INFO_SLOT_REGISTRY_EVENT.subscribe(DuskRain.ID,event->
            event.register(DuskRain.ID,"duskrain.map.region",1000,()->ClientUI.snapshot==null?"烟雨山河":ClientUI.snapshot.region()));
        ClientMapHooks.diagnostics=()->java.util.Map.of("enabled",api.minimapEnabled(),"mapping",mapping,
            "dimension",dimension,"publicWaypoints",api.getWaypoints(DuskRain.ID).size());
        ClientEventRegistry.MAPPING_EVENT.subscribe(DuskRain.ID,event->{
            mapping=event.getStage()==MappingEvent.Stage.MAPPING_STARTED;
            dimension="";borders=-1;
            if(!mapping){api.removeAll(DuskRain.ID);api.removeAllWaypoints(DuskRain.ID);}
        });
        ClientEventRegistry.ENTITY_RADAR_UPDATE_EVENT.subscribe(DuskRain.ID,event->{
            var mc=Minecraft.getInstance();
            var entity=event.getWrappedEntity().getEntityRef().get();
            // Never turn the PvP map into a player locator. Server policy also disables radar.
            if(entity instanceof Player&&entity!=mc.player)event.cancel();
        });
        MinecraftForge.EVENT_BUS.addListener(this::tick);
        DuskRain.LOG.info("DuskRain JourneyMap: public city guide and nearby authorized borders enabled");
    }

    private void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||++ticks%20!=0)return;
        var mc=Minecraft.getInstance();
        if(!mapping||mc.level==null||mc.player==null)return;
        String now=mc.level.dimension().location().toString();
        if(!now.equals(dimension)){
            dimension=now;borders=-1;api.removeAll(DuskRain.ID);api.removeAllWaypoints(DuskRain.ID);
            if(mc.level.dimension().equals(Gameplay.CITY))city();
        }
        if(!mc.level.dimension().equals(Level.OVERWORLD))return;
        boolean fresh=System.currentTimeMillis()-ClientClaimBorders.received<=3500;
        long revision=fresh?ClientClaimBorders.received:0;
        if(revision==borders)return;
        borders=revision;api.removeAll(DuskRain.ID,DisplayType.Polygon);
        if(!fresh||!api.playerAccepts(DuskRain.ID,DisplayType.Polygon))return;
        try{
            for(var edge:ClientClaimBorders.current.edges()){
                if(mc.player.distanceToSqr(edge.x()+8,mc.player.getY(),edge.z()+8)>64*64)continue;
                int x=edge.x(),z=edge.z(),dx=edge.alongX()?16:0,dz=edge.alongX()?0:16;
                int color=new int[]{0x60CFC0,0xD7B765,0xDB6262}[edge.relation()];
                var shape=new ShapeProperties().setStrokeColor(color).setStrokeOpacity(.85f)
                    .setStrokeWidth(1.5f).setFillColor(color).setFillOpacity(.1f);
                // Thin strip follows the outer edge already computed and authorized by the server.
                var polygon=new MapPolygon(new BlockPos(x,64,z),new BlockPos(x+dx,64,z+dz),
                    new BlockPos(x+dx+(dx==0?1:0),64,z+dz+(dz==0?1:0)),
                    new BlockPos(x+(dx==0?1:0),64,z+(dz==0?1:0)));
                api.show(new PolygonOverlay(DuskRain.ID,Level.OVERWORLD,shape,polygon));
            }
        }catch(Exception ex){DuskRain.LOG.warn("JourneyMap border overlay",ex);}
    }

    private void city(){
        for(String id:List.of("spawn","study","quests","alchemy","forge","market","inn","library",
                "trial","warp","enchanter","arena","guildhall","guildgate_west","guildgate_east","palace")){
            var site=CityPlan.find(id);
            var point=WaypointFactory.createWaypoint(DuskRain.ID,new BlockPos(site.x(),site.y()+1,site.z()+site.d()+1),
                "烟雨仙城 · "+site.name(),Gameplay.CITY,false);
            point.setColor(id.startsWith("guild")?0xBFA064:id.equals("trial")?0x9E91BA:0x629D98);
            api.addWaypoint(DuskRain.ID,point);
        }
    }
}

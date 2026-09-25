package cn.duskrain;

import com.google.gson.*;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Editable server rules. Invalid reloads keep the last validated configuration. */
public final class Rules {
    public static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static Rules current = defaults();
    public int contentVersion=4;
    public String serverName = "DuskRain", group = "205255670";
    public int claimLimit = 9, recallSeconds = 5, combatSeconds = 15;
    public int marketBoothPrice=2500;
    public int trialSeconds = 900, meditateIntervalSeconds = 5, meditateXp = 4;
    public int dailyMoney = 80, dailyXp = 60, sellDailyLimit = 256;
    public int[] stageXp = {200,300,450,800,1100,1500,2200,3000,4000,5500,7000,9000,11000,14000,17000,21000,26000,60000,120000,240000,0};
    public String[] realms = {"炼气","筑基","金丹","元婴","化神","渡劫","登神"};
    public String[] schools = {"未择道","剑修","术修","体修"};
    public String[][] skills = {{"御剑·破空","剑阵·回风","剑意·追星","剑心","剑距","剑意"},
        {"灵弹·穿云","寒霜·凝域","雷诀·落星","灵泉","法脉","灵海"},
        {"冲肩·撼岳","踏地·山鸣","护体·反震","御风步","纵云跃","金刚骨"}};
    public double swordReach=4.5,swordQiRange=12,dashRange=6,mageRange=16,bodySpeedBonus=.15,bodyJumpVelocity=.58,mageManaMultiplier=1.25;
    public float skillPvpMultiplier=.75f;
    public int[] skillCost = {15,25,40}, skillCooldown = {5,12,25};
    public List<Quest> main = new ArrayList<>(), daily = new ArrayList<>();
    public List<Offer> shops = new ArrayList<>();
    public record Quest(String id, String title, String type, String target, int count, int stage, int xp, int money) {}
    public record Offer(String id, String shop, String item, int count, int buy, int sell, int stage) {}
    public static Rules defaults() {
        Rules r = new Rules();
        String[][] story={
            {"初入烟雨·山门留名","visit","spawn","1"},{"初入烟雨·拜见松玄","talk","master","1"},{"初入烟雨·试剑听声","practice","dummy","3"},{"初入烟雨·竹林初试","trial","1","1"},
            {"筑道问心·丹师委托","talk","pills","1"},{"筑道问心·亲炼回灵","craft","duskrain:pill_0","1"},{"筑道问心·循声破阵","puzzle","1","1"},{"筑道问心·守心归来","trial","1","1"},
            {"金丹凝华·器师访谈","talk","artifacts","1"},{"金丹凝华·玄铁备炉","submit","minecraft:iron_ingot","16"},{"金丹凝华·机枢解疑","puzzle","2","1"},{"金丹凝华·破甲见心","trial","2","1"},
            {"元婴游神·藏经寻迹","visit","library","1"},{"元婴游神·闻溪旧事","investigate","rumor","1"},{"元婴游神·山外除妖","kill","hostile","30"},{"元婴游神·再问机枢","trial","2","1"},
            {"化神证道·登云问天","visit","palace","1"},{"化神证道·灵珠凝阵","submit","minecraft:ender_pearl","12"},{"化神证道·引雷入柱","puzzle","3","1"},{"化神证道·雷蛟现身","trial","3","1"},
            {"渡劫归真·师门问道","talk","master","1"},{"渡劫归真·淬炼道心","practice","dummy","12"},{"渡劫归真·百战余生","kill","hostile","60"},{"渡劫归真·烟雨长明","trial","3","1"}};
        for(int i=0;i<24;i++){int chapter=i/4;String[] q=story[i];r.main.add(new Quest("main_"+i,q[0],q[1],q[2],Integer.parseInt(q[3]),chapter*3,120*(chapter+1)*(i%4+1),100*(chapter+1)));}
        String[][] ds={{"伐木备料","submit","minecraft:oak_log","32"},{"玄铁订单","submit","minecraft:iron_ingot","12"},
            {"炉火不息","submit","minecraft:coal","24"},{"粮仓补给","submit","minecraft:wheat","32"},
            {"清剿尸妖","kill","minecraft:zombie","12"},{"山道除骸","kill","minecraft:skeleton","10"},
            {"蛛林巡查","kill","minecraft:spider","10"},{"荒野靖妖","kill","hostile","20"},
            {"商街问候","talk","materials","1"},{"丹坊问药","talk","pills","1"},{"演武调息","practice","dummy","6"},
            {"试炼再启","trial","any","1"}};
        for(int i=0;i<ds.length;i++) r.daily.add(new Quest("daily_"+i,ds[i][0],ds[i][1],ds[i][2],Integer.parseInt(ds[i][3]),0,100,100));
        String[] goods={"bread","oak_log","cobblestone","coal","iron_ingot","gold_ingot","redstone","lapis_lazuli","diamond","ender_pearl","blaze_rod","amethyst_shard"};
        int[] prices={8,5,2,8,20,40,12,15,180,120,100,20};
        for(int i=0;i<goods.length;i++) r.shops.add(new Offer(goods[i],"material","minecraft:"+goods[i],1,prices[i],Math.max(1,prices[i]/4),Math.min(6,i/4*3)));
        for(int i=0;i<6;i++) r.shops.add(new Offer("pill_"+i,"alchemy","duskrain:pill_"+i,1,60*(i+1),0,(i%2)*6));
        for(int s=1;s<=3;s++) for(int tier=0;tier<6;tier++) r.shops.add(new Offer("artifact_"+s+"_"+tier,"forge","duskrain:artifact_"+s+"_"+tier,1,300*(tier+1)*(tier+1),0,tier*3));
        addOutfits(r);addDivine(r);
        return r;
    }
    static void addOutfits(Rules r){for(int stage=0;stage<21;stage++)for(net.minecraft.world.item.ArmorItem.Type type:net.minecraft.world.item.ArmorItem.Type.values()){String id=Outfits.id(stage,type);if(r.shops.stream().noneMatch(x->x.id().equals(id)))r.shops.add(new Offer(id,"forge","duskrain:"+id,1,(type==net.minecraft.world.item.ArmorItem.Type.CHESTPLATE?90:60)*(stage+1)*(stage+1),0,stage));}
        if(r.shops.stream().noneMatch(x->x.id().equals("exploration_torch")))r.shops.add(new Offer("exploration_torch","material","duskrain:exploration_torch",1,80,0,0));
        if(r.shops.stream().noneMatch(x->x.id().equals("wayfinder")))r.shops.add(new Offer("wayfinder","material","duskrain:wayfinder",1,40,0,0));
    }
    static void addDivine(Rules r){for(int s=1;s<=3;s++){String id="artifact_"+s+"_6";if(r.shops.stream().noneMatch(x->x.id().equals(id)))r.shops.add(new Offer(id,"forge","duskrain:"+id,1,60000,0,18));}}
    public static synchronized void load() {
        Path f=FMLPaths.CONFIGDIR.get().resolve("duskrain/rules.json");
        try {
            Files.createDirectories(f.getParent());
            if(!Files.exists(f)) Files.writeString(f,JSON.toJson(current),StandardCharsets.UTF_8);
            String raw=Files.readString(f,StandardCharsets.UTF_8);
            Rules candidate=JSON.fromJson(raw,Rules.class);
            if(!JsonParser.parseString(raw).getAsJsonObject().has("contentVersion")){
                Path old=f.resolveSibling("rules-before-remake.json");if(!Files.exists(old))Files.copy(f,old);
                Rules fresh=defaults();candidate.main=fresh.main;candidate.daily=fresh.daily;candidate.skills=fresh.skills;candidate.contentVersion=2;
                Files.writeString(f,JSON.toJson(candidate),StandardCharsets.UTF_8);
            }
            if(candidate.contentVersion<3){Path backup=f.resolveSibling("rules-before-outfits.json");if(!Files.exists(backup))Files.copy(f,backup);addOutfits(candidate);candidate.contentVersion=3;}
            if(candidate.contentVersion<4){Files.copy(f,f.resolveSibling("rules-before-ascension-"+System.currentTimeMillis()+".json"));candidate.stageXp=Arrays.copyOf(candidate.stageXp,21);candidate.stageXp[17]=60000;candidate.stageXp[18]=120000;candidate.stageXp[19]=240000;candidate.stageXp[20]=0;candidate.realms=Arrays.copyOf(candidate.realms,7);candidate.realms[6]="登神";addOutfits(candidate);addDivine(candidate);candidate.contentVersion=4;candidate.validate();Files.writeString(f,JSON.toJson(candidate),StandardCharsets.UTF_8);}
            if(candidate.shops.stream().noneMatch(o->o.id().startsWith("architecture_"))){
                for(String id:Architecture.ALL.keySet())candidate.shops.add(new Offer("architecture_"+id,"material","duskrain:"+id,8,id.contains("lamp")||id.contains("lantern")?32:16,0,0));
                Files.writeString(f,JSON.toJson(candidate),StandardCharsets.UTF_8);
            }
            candidate.validate(); current=candidate;
        } catch(Exception ex) { DuskRain.LOG.error("DuskRain rules rejected; using previous valid rules",ex); }
    }
    public void validate() {
        if(marketBoothPrice<1||marketBoothPrice>100000000)throw new IllegalArgumentException("marketBoothPrice out of range");
        if(stageXp.length!=21 || realms.length!=7 || schools.length!=4 || main.size()!=24 || daily.size()!=12 || claimLimit<1 || claimLimit>256
            || recallSeconds<0 || combatSeconds<0 || trialSeconds<30 || meditateIntervalSeconds<1 || meditateXp<0 || dailyMoney<0 || dailyXp<0)
            throw new IllegalArgumentException("Invalid rules dimensions or limits");
        if(swordReach<3||swordReach>8||dashRange<1||dashRange>12||swordQiRange<1||swordQiRange>32||mageRange<1||mageRange>32||bodySpeedBonus<0||bodySpeedBonus>1||bodyJumpVelocity<.42||bodyJumpVelocity>.8||mageManaMultiplier<1||mageManaMultiplier>3||skillPvpMultiplier<0||skillPvpMultiplier>1)throw new IllegalArgumentException("Invalid combat tuning");
        for(int i=0;i<20;i++) if(stageXp[i]<=0) throw new IllegalArgumentException("Invalid stage cost");
        for(Offer x:shops) if(x.buy<=0||x.sell<0||x.sell>=x.buy||x.count<1||x.count>64||x.stage<0||x.stage>20) throw new IllegalArgumentException("Invalid shop: "+x.id);
    }
    public static String realm(int stage) { return current.realms[Math.min(6,Math.max(0,stage/3))]+new String[]{"初期","中期","后期"}[Math.floorMod(stage,3)]; }
    public static int manaMax(int stage) {return 100+Math.min(17,stage)*20;}
}

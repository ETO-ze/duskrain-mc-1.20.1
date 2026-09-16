package cn.duskrain;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.RegistryObject;

public final class Ascension {
    public static final RegistryObject<Item> CORE=Content.ITEMS.register("tribulation_core",()->new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> CRYSTAL=Content.ITEMS.register("divine_crystal",()->new Item(new Item.Properties().rarity(Rarity.EPIC)));
    public static void init(){}
    public static String requirements(Profile r){
        if(r.stage>=20)return "登神后期 · 神道归一";
        int k=Math.max(0,r.stage-17);return Rules.current.stageXp[r.stage]+" 修为 · "+AscensionRules.current.cores[k]+(k==0?" 劫雷灵核":" 神纹晶")+" · 下界之星 × "+AscensionRules.current.stars[k]+"；完成六章主线；挑战成功才结算";
    }
    public static boolean eligible(ServerPlayer p,boolean tell){
        Profile r=Store.of(p);if(r.stage<17||r.stage>=20||r.main<24||r.school==0){if(tell)Gameplay.say(p,"登神要求渡劫后期及以上、未达圆满，且已完成24项主线。");return false;}
        int rank=r.stage-17;var c=AscensionRules.current;
        boolean ok=r.xp>=Rules.current.stageXp[r.stage]&&Gameplay.count(p,rank==0?CORE.get():CRYSTAL.get())>=c.cores[rank]&&Gameplay.count(p,Items.NETHER_STAR)>=c.stars[rank];
        if(!ok&&tell)Gameplay.say(p,"飞升准备不足："+requirements(r));return ok;
    }
    public static boolean complete(ServerPlayer p,int originalStage){
        Profile r=Store.of(p);if(r.stage!=originalStage||!eligible(p,true))return false;int rank=r.stage-17;
        boolean committed=Transactions.commit(p,"ascend:"+r.stage,()->{Gameplay.take(p,rank==0?CORE.get():CRYSTAL.get(),AscensionRules.current.cores[rank]);Gameplay.take(p,Items.NETHER_STAR,AscensionRules.current.stars[rank]);r.xp-=Rules.current.stageXp[r.stage];r.stage++;});
        if(committed){Skills.attributes(p);Network.sync(p);Gameplay.say(p,"神门已开！"+Rules.realm(Store.of(p).stage)+" · "+new String[]{"凝聚神格","铸成神躯","神道归一"}[rank]);}return committed;
    }
}

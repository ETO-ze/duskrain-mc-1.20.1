package cn.duskrain;

import java.nio.file.*;
import net.minecraftforge.fml.loading.FMLPaths;

/** Versioned additions never reinterpret the original eighteen stage IDs. */
public final class AscensionRules {
    public static AscensionRules current=new AscensionRules();
    public double[] health={160,260,400},damage={2.5,3.5,5},mana={300,600,1000},speed={.05,.08,.12},resistance={.1,.2,.3};
    public double pvp=.2;
    public int[] trialHealth={600,1800,4200},trialDamage={8,18,28},cores={3,6,12},stars={1,2,4};
    public int enchantPrice=200,enchantLapis=4,enchantSand=1,enchantLevels=2;
    public double enchantPvp=.5;
    public int[] enchantMax={5,5,5,3,3,4,3,3};
    public double[] enchantPower={.08,.05,.05,30,.08,.03,.15,.10};
    public double flightSpeed=10,divineFlightSpeed=14,verticalSpeed=5,flightMana=2;
    public int[] flightPrices={500,2000},flightMinutes={10,60};
    public int stockLimit=4096;
    public void validate(){
        for(double[] a:new double[][]{health,damage,mana,speed,resistance}){if(a.length!=3)throw new IllegalArgumentException("Three divine stages required");for(double v:a)if(!Double.isFinite(v)||v<0||v>10000)throw new IllegalArgumentException("Invalid divine number");}
        for(int[] a:new int[][]{trialHealth,trialDamage,cores,stars}){if(a.length!=3)throw new IllegalArgumentException("Three trials required");for(int v:a)if(v<1||v>100000)throw new IllegalArgumentException("Invalid trial number");}
        if(!Double.isFinite(pvp)||!Double.isFinite(enchantPvp)||pvp<0||pvp>1||enchantPvp<0||enchantPvp>1||enchantMax.length!=8||enchantPower.length!=8||enchantPrice<1||enchantPrice>1000000||enchantLapis<1||enchantSand<1||enchantLevels<1)throw new IllegalArgumentException("Invalid enchanting rules");
        for(int i=0;i<8;i++)if(enchantMax[i]<1||enchantMax[i]>5||!Double.isFinite(enchantPower[i])||enchantPower[i]<0||enchantPower[i]>100)throw new IllegalArgumentException("Invalid enchant level/effect");
        for(double v:new double[]{flightSpeed,divineFlightSpeed,verticalSpeed,flightMana})if(!Double.isFinite(v))throw new IllegalArgumentException("Non-finite flight number");
        if(flightPrices.length!=2||flightMinutes.length!=2||flightSpeed<1||flightSpeed>20||divineFlightSpeed<1||divineFlightSpeed>24||verticalSpeed<1||verticalSpeed>10||flightMana<.1||flightMana>100||stockLimit<64||stockLimit>4096)throw new IllegalArgumentException("Invalid flight/stock rules");
        for(int i=0;i<2;i++)if(flightPrices[i]<1||flightMinutes[i]<1||flightMinutes[i]>1440)throw new IllegalArgumentException("Invalid rental");
    }
    public static void load(){Path f=FMLPaths.CONFIGDIR.get().resolve("duskrain/ascension.json");try{Files.createDirectories(f.getParent());if(!Files.exists(f))Files.writeString(f,Rules.JSON.toJson(current));var next=Rules.JSON.fromJson(Files.readString(f),AscensionRules.class);next.validate();current=next;}catch(Exception e){DuskRain.LOG.error("Ascension rules rejected; retaining previous configuration",e);}}
    public static int rank(Profile r){return Math.min(2,r.stage-18);}
    public static double bonus(Profile r,double[] values){int rank=rank(r);return rank<0?0:values[rank]*(r.combatUntil>System.currentTimeMillis()?current.pvp:1);}
    public static double damage(Profile r,boolean pvp){int rank=rank(r);return rank<0?1:1+(current.damage[rank]-1)*(pvp?current.pvp:1);}
}

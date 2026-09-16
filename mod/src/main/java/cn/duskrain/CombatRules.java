package cn.duskrain;

import java.nio.file.*;
import net.minecraftforge.fml.loading.FMLPaths;

/** Independently reloadable first-round combat tuning; indexes are sword, mage, body. */
public final class CombatRules {
    public static CombatRules current=new CombatRules();
    public double base=6, stage=.8, artifact=2, citySpeed=.40, bodySpeed=.25, bodyHealth=10, bodyResistance=.60, jump=.58;
    public double reach=4.5, swordMelee=.15, intent=.30, manaMultiplier=1.5, regenMultiplier=1.5, mageDiscount=.15, pvp=.65;
    public int[][] costs={{12,24,30},{12,26,32},{10,20,25}}, cooldowns={{4,9,14},{4,10,16},{6,9,16}};
    public double[][] damage={{2,.6,3},{2.2,.35,3.2},{1.5,2,.5}}, ranges={{14,4.5,8},{18,4.5,16},{6,5,0}};
    public double normalQiRange=6,normalQiDamage=.45, guardPve=.5,guardPvp=.25,frostCombo=.3;
    public int normalQiCost=2,guardTicks=120,frostTicks=120,chargeTicks=24,controlTicks=15;
    public void validate(){
        if(costs.length!=3||cooldowns.length!=3||damage.length!=3||ranges.length!=3)throw new IllegalArgumentException("Three schools required");
        for(int s=0;s<3;s++){if(costs[s].length!=3||cooldowns[s].length!=3||damage[s].length!=3||ranges[s].length!=3)throw new IllegalArgumentException("Three skills required");for(int i=0;i<3;i++){if(costs[s][i]<0||costs[s][i]>10000||cooldowns[s][i]<1||cooldowns[s][i]>3600||!Double.isFinite(damage[s][i])||damage[s][i]<0||damage[s][i]>100||!Double.isFinite(ranges[s][i])||ranges[s][i]<0||ranges[s][i]>64)throw new IllegalArgumentException("Skill tuning outside bounds");}}
        for(double n:new double[]{base,stage,artifact,citySpeed,bodySpeed,bodyHealth,bodyResistance,jump,reach,swordMelee,intent,manaMultiplier,regenMultiplier,mageDiscount,pvp,normalQiRange,normalQiDamage,guardPve,guardPvp,frostCombo})if(!Double.isFinite(n)||n<0||n>100)throw new IllegalArgumentException("Nonfinite/negative tuning");
        if(citySpeed>1||bodySpeed>1||bodyResistance>1||pvp>1||mageDiscount>1||guardPve>1||guardPvp>1||jump>1||reach>6||normalQiRange>32||normalQiCost<0||guardTicks<1||guardTicks>1200||frostTicks<20||frostTicks>1200||chargeTicks<1||chargeTicks>200||controlTicks<0||controlTicks>15)throw new IllegalArgumentException("Unsafe combat bounds");
    }
    public static void load(){Path f=FMLPaths.CONFIGDIR.get().resolve("duskrain/combat.json");try{Files.createDirectories(f.getParent());if(!Files.exists(f))Files.writeString(f,Rules.JSON.toJson(current));var next=Rules.JSON.fromJson(Files.readString(f),CombatRules.class);next.validate();current=next;}catch(Exception e){DuskRain.LOG.error("Combat config rejected; keeping previous tuning",e);}}
}

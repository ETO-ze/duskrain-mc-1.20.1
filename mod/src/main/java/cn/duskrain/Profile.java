package cn.duskrain;

import net.minecraft.nbt.*;
import java.time.*;
import java.util.*;

public final class Profile {
    public int cityVersion;
    public String knownName="";
    public int stage,school,xp,mana=100,main,mainProgress,kills,trialWins;
    public long money=100,lastSignDay=-1,day=-1,combatUntil;
    public int[] dailyProgress=new int[12];
    public Set<Integer> dailyClaimed=new HashSet<>();
    public Map<String,Integer> sold=new HashMap<>();
    public boolean initialized,sidebar=true,meditating,arena;
    public boolean starterGranted,onboardingComplete;
    public float sidebarScale=1;
    public ListTag retained=new ListTag();
    public String homeDim="minecraft:overworld";
    public double homeX,homeY,homeZ;
    public boolean hasHome;
    public int[] upgrades=new int[4],elixirs=new int[4];
    public String borderMode="auto";
    public transient double enchantedMana,enchantedRegen;
    public long flightTicks; public boolean titles=true;
    public static long today() {return LocalDate.now(ZoneId.of("Asia/Shanghai")).toEpochDay();}
    public void refreshDay() {long now=today(); if(day!=now){day=now;dailyProgress=new int[12];dailyClaimed.clear();sold.clear();}}
    public List<Integer> dailies(UUID id) {refreshDay(); List<Integer> a=new ArrayList<>();for(int i=0;i<12;i++)a.add(i);Collections.shuffle(a,new Random(id.getMostSignificantBits()^id.getLeastSignificantBits()^day));return a.subList(0,3);}
    public CompoundTag save() {
        CompoundTag n=new CompoundTag();
        n.putLong("flightTicks",flightTicks);n.putBoolean("titles",titles);n.putIntArray("upgrades",upgrades);n.putIntArray("elixirs",elixirs);n.putString("borderMode",borderMode);
        n.putString("knownName",knownName);n.putBoolean("starterGranted",starterGranted);n.putBoolean("onboardingComplete",onboardingComplete);
        n.putInt("cityVersion",cityVersion);n.putInt("stage",stage);n.putInt("school",school);n.putInt("xp",xp);n.putInt("mana",mana);n.putInt("main",main);n.putInt("mainProgress",mainProgress);n.putInt("kills",kills);n.putInt("trialWins",trialWins);
        n.putLong("money",money);n.putLong("lastSignDay",lastSignDay);n.putLong("day",day);n.putLong("combatUntil",combatUntil);
        n.putIntArray("dailyProgress",dailyProgress);n.putIntArray("dailyClaimed",dailyClaimed.stream().mapToInt(Integer::intValue).toArray());
        CompoundTag s=new CompoundTag();sold.forEach(s::putInt);n.put("sold",s);
        n.putBoolean("initialized",initialized);n.putBoolean("sidebar",sidebar);n.putFloat("sidebarScale",sidebarScale);n.put("retained",retained.copy());
        n.putBoolean("hasHome",hasHome);n.putString("homeDim",homeDim);n.putDouble("homeX",homeX);n.putDouble("homeY",homeY);n.putDouble("homeZ",homeZ);
        return n;
    }
    public static Profile load(CompoundTag n) {
        Profile p=new Profile();
        int[] upgrades=n.getIntArray("upgrades"),elixirs=n.getIntArray("elixirs");for(int i=0;i<4;i++){p.upgrades[i]=upgrades.length==4?Math.max(0,Math.min(5,upgrades[i])):0;p.elixirs[i]=elixirs.length==4?Math.max(0,Math.min(72000,elixirs[i])):0;}if(java.util.Set.of("auto","on","off").contains(n.getString("borderMode")))p.borderMode=n.getString("borderMode");
        p.flightTicks=Math.max(0,n.getLong("flightTicks"));p.titles=!n.contains("titles")||n.getBoolean("titles");p.knownName=n.getString("knownName");
        p.cityVersion=n.getInt("cityVersion");p.stage=Math.max(0,Math.min(20,n.getInt("stage")));p.school=Math.max(0,Math.min(3,n.getInt("school")));p.xp=Math.max(0,n.getInt("xp"));p.mana=n.getInt("mana");p.main=n.getInt("main");p.mainProgress=n.getInt("mainProgress");p.kills=n.getInt("kills");p.trialWins=n.getInt("trialWins");
        p.money=Math.max(0,n.getLong("money"));p.lastSignDay=n.getLong("lastSignDay");p.day=n.getLong("day");p.combatUntil=n.getLong("combatUntil");
        int[] dp=n.getIntArray("dailyProgress");if(dp.length==12)p.dailyProgress=dp;
        for(int i:n.getIntArray("dailyClaimed"))p.dailyClaimed.add(i);
        CompoundTag sold=n.getCompound("sold");for(String k:sold.getAllKeys())p.sold.put(k,sold.getInt(k));
        p.initialized=n.getBoolean("initialized");p.starterGranted=n.contains("starterGranted")?n.getBoolean("starterGranted"):p.initialized;p.onboardingComplete=n.contains("onboardingComplete")?n.getBoolean("onboardingComplete"):p.initialized;p.sidebar=!n.contains("sidebar")||n.getBoolean("sidebar");p.sidebarScale=n.contains("sidebarScale")?n.getFloat("sidebarScale"):1;
        p.retained=n.getList("retained",Tag.TAG_COMPOUND);p.hasHome=n.getBoolean("hasHome");p.homeDim=n.getString("homeDim");p.homeX=n.getDouble("homeX");p.homeY=n.getDouble("homeY");p.homeZ=n.getDouble("homeZ");
        return p;
    }
}

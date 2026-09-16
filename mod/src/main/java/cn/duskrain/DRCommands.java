package cn.duskrain;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
import static net.minecraft.commands.Commands.*;

public final class DRCommands {
    interface PlayerAction {void run(ServerPlayer p)throws Exception;}
    static int doPlayer(CommandContext<CommandSourceStack> c,PlayerAction action){try{ServerPlayer player=c.getSource().getPlayerOrException();String input=c.getInput().replaceFirst("^/","");if(Store.of(player).initialized&&!Store.of(player).onboardingComplete&&!Set.of("dr","dr menu","dr guide","dr start").contains(input)&&!player.hasPermissions(3)){Onboarding.menu(player);return 0;}action.run(player);return 1;}catch(Exception e){c.getSource().sendFailure(Component.literal(e.getMessage()==null?"操作未完成":e.getMessage()));return 0;}}
    public static void register(CommandDispatcher<CommandSourceStack> d){
        d.register(literal("dr").executes(c->doPlayer(c,DRCommands::menu))
            .then(Guilds.commands())
            .then(literal("enchant").executes(c->doPlayer(c,Enchanting::open)))
            .then(literal("flight").executes(c->doPlayer(c,SwordFlight::menu)).then(literal("toggle").executes(c->doPlayer(c,SwordFlight::toggle))).then(literal("rent").then(argument("option",IntegerArgumentType.integer(0,1)).executes(c->doPlayer(c,p->SwordFlight.rent(p,IntegerArgumentType.getInteger(c,"option")))))))
            .then(literal("title").executes(c->doPlayer(c,p->{Gameplay.say(p,PlayerTitles.text(p));PlayerTitles.toggle(p);})).then(literal("toggle").executes(c->doPlayer(c,PlayerTitles::toggle))))
            .then(literal("start").executes(c->doPlayer(c,Onboarding::start)))
            .then(literal("guide").executes(c->doPlayer(c,Onboarding::guide)))
            .then(literal("menu").executes(c->doPlayer(c,DRCommands::menu)))
            .then(literal("spawn").executes(c->doPlayer(c,p->Gameplay.recall(p,Gameplay.CITY,new Vec3(.5,CityPlan.SPAWN_Y,CityPlan.SPAWN_Z+.5)))))
            .then(literal("warp").executes(c->doPlayer(c,DRCommands::warps)).then(argument("place",StringArgumentType.word()).executes(c->doPlayer(c,p->warp(p,StringArgumentType.getString(c,"place"))))))
            .then(literal("profile").executes(c->doPlayer(c,DRCommands::profile)))
            .then(literal("cultivate").executes(c->doPlayer(c,DRCommands::profile))
                .then(literal("choose").then(argument("school",IntegerArgumentType.integer(1,3)).executes(c->doPlayer(c,p->choose(p,IntegerArgumentType.getInteger(c,"school"))))))
                .then(literal("sit").executes(c->doPlayer(c,p->{Profile r=Store.of(p);if(r.school==0){Gameplay.say(p,"请先选择流派。");return;}r.meditating=!r.meditating;Gameplay.say(p,r.meditating?"开始调息，保持静止以获得修为。":"已停止调息。");})))
                .then(literal("breakthrough").executes(c->doPlayer(c,DRCommands::breakthrough))))
            .then(literal("skills").executes(c->doPlayer(c,DRCommands::skills)).then(literal("cast").then(argument("slot",IntegerArgumentType.integer(0,2)).executes(c->doPlayer(c,p->Gameplay.cast(p,IntegerArgumentType.getInteger(c,"slot")))))))
            .then(literal("quests").executes(c->doPlayer(c,DRCommands::quests)).then(literal("claim").then(argument("id",IntegerArgumentType.integer(0,23)).executes(c->doPlayer(c,p->claimQuest(p,IntegerArgumentType.getInteger(c,"id"),false))))))
            .then(literal("daily").executes(c->doPlayer(c,DRCommands::daily)).then(literal("sign").executes(c->doPlayer(c,DRCommands::sign))).then(literal("claim").then(argument("id",IntegerArgumentType.integer(0,11)).executes(c->doPlayer(c,p->claimQuest(p,IntegerArgumentType.getInteger(c,"id"),true))))))
            .then(literal("balance").executes(c->doPlayer(c,p->Gameplay.say(p,"灵石余额："+Store.of(p).money))))
            .then(literal("shop").executes(c->doPlayer(c,p->shop(p,"material")))
                .then(literal("page").then(argument("filter",StringArgumentType.string()).then(argument("page",IntegerArgumentType.integer(0,100000)).executes(c->doPlayer(c,p->ShopCatalog.page(p,StringArgumentType.getString(c,"filter"),IntegerArgumentType.getInteger(c,"page")))))))
                .then(literal("inspect").then(argument("id",StringArgumentType.word()).executes(c->doPlayer(c,p->product(p,StringArgumentType.getString(c,"id"))))))
                .then(literal("view").then(argument("category",StringArgumentType.word()).executes(c->doPlayer(c,p->shop(p,StringArgumentType.getString(c,"category"))))))
                .then(literal("buy").then(argument("id",StringArgumentType.word()).executes(c->doPlayer(c,p->trade(p,StringArgumentType.getString(c,"id"),1,false))).then(argument("amount",IntegerArgumentType.integer(1,64)).executes(c->doPlayer(c,p->trade(p,StringArgumentType.getString(c,"id"),IntegerArgumentType.getInteger(c,"amount"),false))))))
                .then(literal("sell").then(argument("id",StringArgumentType.word()).executes(c->doPlayer(c,p->trade(p,StringArgumentType.getString(c,"id"),1,true))).then(argument("amount",IntegerArgumentType.integer(1,64)).executes(c->doPlayer(c,p->trade(p,StringArgumentType.getString(c,"id"),IntegerArgumentType.getInteger(c,"amount"),true)))))))
            .then(literal("city_jump").executes(c->doPlayer(c,CityMobility::jump)))
            .then(literal("stalls").executes(c->doPlayer(c,PlayerMarket::open))
                .then(literal("buy").then(argument("id",IntegerArgumentType.integer(0,11)).executes(c->doPlayer(c,p->PlayerMarket.buy(p,IntegerArgumentType.getInteger(c,"id"))))))
                .then(literal("sell").then(argument("price",IntegerArgumentType.integer(1,100000000)).then(argument("amount",IntegerArgumentType.integer(1,64)).executes(c->doPlayer(c,p->PlayerMarket.sell(p,IntegerArgumentType.getInteger(c,"price"),IntegerArgumentType.getInteger(c,"amount")))))))
                .then(literal("purchase").then(argument("listing",StringArgumentType.word()).executes(c->doPlayer(c,p->PlayerMarket.purchase(p,StringArgumentType.getString(c,"listing"),false)))))
                .then(literal("take").then(argument("listing",StringArgumentType.word()).executes(c->doPlayer(c,p->PlayerMarket.purchase(p,StringArgumentType.getString(c,"listing"),true))))))
            .then(literal("claim").executes(c->doPlayer(c,DRCommands::claimMenu))
                .then(literal("members").executes(c->doPlayer(c,LandClaims::members)))
                .then(literal("list").executes(c->doPlayer(c,LandClaims::list)))
                .then(literal("border").executes(c->doPlayer(c,LandClaims::border)).then(argument("mode",StringArgumentType.word()).executes(c->doPlayer(c,p->ClaimBorders.mode(p,StringArgumentType.getString(c,"mode"))))))
                .then(literal("add").executes(c->doPlayer(c,DRCommands::claimAdd)))
                .then(literal("remove").executes(c->doPlayer(c,DRCommands::claimRemove)))
                .then(literal("trust").then(argument("player",StringArgumentType.word()).executes(c->doPlayer(c,p->LandClaims.trust(p,StringArgumentType.getString(c,"player"),true)))))
                .then(literal("untrust").then(argument("player",StringArgumentType.word()).executes(c->doPlayer(c,p->LandClaims.trust(p,StringArgumentType.getString(c,"player"),false))))))
            .then(literal("home").executes(c->doPlayer(c,DRCommands::home)).then(literal("set").executes(c->doPlayer(c,DRCommands::homeSet))))
            .then(literal("party").executes(c->doPlayer(c,DRCommands::party))
                .then(literal("invite").then(argument("player",EntityArgument.player()).executes(c->doPlayer(c,p->Trials.invite(p,EntityArgument.getPlayer(c,"player"))))))
                .then(literal("accept").executes(c->doPlayer(c,Trials::accept))).then(literal("leave").executes(c->doPlayer(c,Trials::partyLeave))))
            .then(literal("trial").executes(c->doPlayer(c,DRCommands::trials)).then(literal("enter").then(argument("tier",IntegerArgumentType.integer(1,6)).executes(c->doPlayer(c,p->Trials.enter(p,IntegerArgumentType.getInteger(c,"tier"),false))))).then(literal("leave").executes(c->doPlayer(c,p->Trials.leave(p,false)))))
            .then(literal("arena").executes(c->doPlayer(c,p->{Store.of(p).arena=!Store.of(p).arena;Gameplay.say(p,"擂台对战意愿："+(Store.of(p).arena?"已开启，双方均同意且进入擂台后可战斗":"已关闭"));})))
            .then(literal("sidebar").executes(c->doPlayer(c,DRCommands::sidebar)).then(literal("toggle").executes(c->doPlayer(c,p->{Store.of(p).sidebar=!Store.of(p).sidebar;Network.sync(p);}))).then(literal("scale").then(argument("scale",FloatArgumentType.floatArg(.65f,1.5f)).executes(c->doPlayer(c,p->{Store.of(p).sidebarScale=FloatArgumentType.getFloat(c,"scale");Network.sync(p);})))))
            .then(literal("build").requires(s->s.hasPermission(3)).executes(c->doPlayer(c,Construction::menu))
            .then(literal("start").then(argument("scope",StringArgumentType.word()).executes(c->doPlayer(c,p->Construction.start(p,StringArgumentType.getString(c,"scope"))))))
                .then(literal("pause").executes(c->{Construction.paused=true;return 1;})).then(literal("resume").executes(c->{Construction.paused=false;return 1;}))
                .then(literal("speed").then(argument("blocks",IntegerArgumentType.integer(1,20000)).executes(c->{Construction.budget=IntegerArgumentType.getInteger(c,"blocks");return 1;})))
                .then(literal("camera").then(argument("view",StringArgumentType.word()).executes(c->doPlayer(c,p->Construction.camera(p,StringArgumentType.getString(c,"view"))))))
                .then(literal("record").then(argument("state",BoolArgumentType.bool()).executes(c->doPlayer(c,p->Network.capture(p,BoolArgumentType.getBool(c,"state")))))))
            .then(literal("admin").requires(s->s.hasPermission(3)).executes(c->doPlayer(c,DRCommands::admin))
                .then(literal("maintenance").executes(c->doPlayer(c,p->{if(!Protection.MAINTENANCE.remove(p.getUUID()))Protection.MAINTENANCE.add(p.getUUID());Gameplay.say(p,"管理员维护："+Protection.MAINTENANCE.contains(p.getUUID()));})))
                .then(literal("reload").executes(c->{Rules.load();CombatRules.load();Boosts.load();AscensionRules.load();c.getSource().sendSuccess(()->Component.literal("DuskRain 配置已检查；无效配置保留上个版本，详情见日志。"),false);return 1;}))
                .then(literal("pregen").executes(c->{Construction.pregen(c.getSource().getServer());return 1;}))
                .then(literal("audit").executes(c->{Audit.run(c.getSource().getServer());return 1;}))
                .then(literal("palace").executes(c->doPlayer(c,GuildPalace::begin)))
                .then(literal("walk_audit").executes(c->{WalkAudit.run(c.getSource().getServer());return 1;}))
                .then(literal("fixture_audit").executes(c->{FixtureAudit.run(c.getSource().getServer());return 1;}))
                .then(literal("site_audit").executes(c->{SiteAudit.run(c.getSource().getServer());return 1;}))
                .then(literal("route_audit").executes(c->{RouteNetworkAudit.run(c.getSource().getServer());return 1;}))
                .then(literal("stage").then(argument("player",EntityArgument.player()).then(argument("stage",IntegerArgumentType.integer(0,20)).executes(c->{ServerPlayer p=EntityArgument.getPlayer(c,"player");Store.of(p).stage=IntegerArgumentType.getInteger(c,"stage");Network.sync(p);return 1;}))))
                .then(literal("money").then(argument("player",EntityArgument.player()).then(argument("amount",LongArgumentType.longArg(0,1_000_000_000_000L)).executes(c->{ServerPlayer p=EntityArgument.getPlayer(c,"player");Store.of(p).money=LongArgumentType.getLong(c,"amount");Network.sync(p);return 1;}))))
                .then(literal("reset_school").then(argument("player",EntityArgument.player()).executes(c->{ServerPlayer p=EntityArgument.getPlayer(c,"player");Store.of(p).school=0;Skills.attributes(p);Skills.clear(p.getUUID());Network.sync(p);return 1;})))
                .then(literal("repair").then(argument("player",EntityArgument.player()).executes(c->{Gameplay.restore(EntityArgument.getPlayer(c,"player"));return 1;})))));
    }
    static Network.Entry entry(String label,String command){return new Network.Entry(label,"dr "+command);}
    static Network.Entry card(String label,String command,String detail,String icon,boolean enabled){return new Network.Entry(label,"dr "+command,detail,icon,enabled);}
    static void show(ServerPlayer p,String title,String description,Network.Entry... es){Network.menu(p,title,description,Arrays.asList(es));}
    public static void menu(ServerPlayer p){Profile r=Store.of(p);if(r.initialized&&!r.onboardingComplete){Onboarding.menu(p);return;}show(p,"仙途首页","烟雨山岛 · 商店、拜师、交付任务与试炼请到对应楼阁，与村民交谈。"+(r.school==0?"初来此地，请先前往问道书院，拜访松玄传道师。":"继续主线、每日委托与试炼，积累突破所需材料。"),
        card("修炼功法","profile",Rules.realm(r.stage)+" · 修为 "+r.xp,"minecraft:enchanted_book",true),card("主线任务","quests","进度 "+Math.min(24,r.main+1)+" / 24 · 探索成长","minecraft:writable_book",true),
        card("每日委托","daily",r.lastSignDay==Profile.today()?"今日已签到 · 查看三项委托":"今日签到尚未领取","minecraft:clock",true),
        card("云渡出行","warp","主岛楼阁 · 浮岛 · 生存野外","minecraft:ender_pearl",true),card("道友队伍","party","邀请道友；试炼在问劫云台开启","minecraft:player_head",true),
        card("帮派洞天","guild","宗务堂创立宗门；专属建设空间","minecraft:bell",true),card("住宅领地","claim","认领进度 "+Store.get(p.server).claimCount(p.getUUID())+" / "+Rules.current.claimLimit,"minecraft:cherry_door",true),card("流派技能","skills","主动 Z / X / C · 三项被动","duskrain:artifact_1_0",true),card("御剑行空","flight","R起剑/收剑 · 云渡使处购买体验","duskrain:artifact_1_0",true),card("名下头衔","title","境界、等级与宗门身份；点击切换显示","minecraft:name_tag",true));}
    public static void service(ServerPlayer p){CityPlan.Building b=Gameplay.nearBuilding(p);if(b==null){menu(p);return;}switch(b.id()){case "alchemy"->shop(p,"alchemy");case "forge"->shop(p,"forge");case "market"->shop(p,"material");case "study","library"->profile(p);case "quests"->quests(p);case "trial"->trials(p);case "warp"->warps(p);default->menu(p);}}
    static void profile(ServerPlayer p){
        Profile r=Store.of(p);List<Network.Entry> a=new ArrayList<>();
        if(r.school==0&&Services.nearby(p,"master"))for(int i=1;i<=3;i++)a.add(card("选择"+Rules.current.schools[i],"cultivate choose "+i,new String[]{"","剑气破空 · 4.5格剑距与剑意","元素法术 · 灵力与范围","近战护体 · 移动、跳跃与韧性"}[i],"duskrain:artifact_"+i+"_0",true));
        a.add(card(r.meditating?"停止调息":"静坐调息","cultivate sit","保持静止；每"+Rules.current.meditateIntervalSeconds+"秒获得"+Rules.current.meditateXp+"修为","minecraft:amethyst_shard",r.school>0));
        String need="修为 "+r.xp+" / "+Rules.current.stageXp[r.stage];
        if(r.stage%3==2&&r.stage<17)need+="；材料 "+new String[]{"铁锭 × 8","金锭 × 8","钻石 × 4","末影之眼 × 4","下界之星 × 1"}[r.stage/3]+"；单人突破试炼";
        if(r.stage>=17)need=Ascension.requirements(r);
        if(Services.nearby(p,"master"))a.add(card("突破境界","cultivate breakthrough",need,"minecraft:nether_star",r.school>0&&r.stage<20&&r.xp>=Rules.current.stageXp[r.stage]));
        if(!Services.nearby(p,"master"))a.add(card("前往问道书院","warp study","与松玄交谈，拜师或突破","minecraft:compass",true));
        a.add(card("查看流派技能","skills","技能效果、灵力消耗和冷却","minecraft:diamond_sword",r.school>0));
        Network.menu(p,"修炼功法 · "+Rules.realm(r.stage),PlayerTitles.text(p)+"；流派："+Rules.current.schools[r.school]+"。"+need+"。"+Boosts.summary(p)+"。突破失败不掉境界；登神挑战仅成功结算，原境界挑战开始时消耗材料。",a);
    }
    static void choose(ServerPlayer p,int school){if(!Services.require(p,"master"))return;Profile r=Store.of(p);if(r.school!=0){Gameplay.say(p,"已选择流派；需要更改时请联系管理员。");return;}ItemStack gift=new ItemStack(Content.ARTIFACTS.get((school-1)*6).get());Content.bind(gift,p);if(!Gameplay.fits(p,gift)){Gameplay.say(p,"请先腾出背包空间。");return;}r.school=school;p.getInventory().add(gift);Network.sync(p);profile(p);}
    static void breakthrough(ServerPlayer p){if(!Services.require(p,"master"))return;Profile r=Store.of(p);if(r.stage>=17){if(Ascension.eligible(p,true))Trials.enter(p,4+r.stage-17,true);return;}if(r.school==0){Gameplay.say(p,"请先选择流派。");return;}if(Trials.inTrial(p.getUUID()))return;int cost=Rules.current.stageXp[r.stage];if(r.xp<cost){Gameplay.say(p,"修为不足，还需 "+(cost-r.xp)+"。");return;}if(r.stage%3==2){Trials.enter(p,Math.min(3,(r.stage+1)/6+1),true);}else{r.xp-=cost;r.stage++;Gameplay.say(p,"进境成功："+Rules.realm(r.stage));Network.sync(p);}}
    static void skills(ServerPlayer p){
        Profile r=Store.of(p);if(r.school==0){profile(p);return;}List<Network.Entry> es=new ArrayList<>();var c=CombatRules.current;
        String[][] names={{"破空","回风","追星"},{"离火灵弹","寒霜符阵","九霄雷诀"},{"撼岳","山鸣","金刚护体"}};
        String[][] effects={{"14格直线贯穿，最多3目标","半径4.5格，三道剑斩（下列为每道伤害）","8格碰撞检测突进，终点斩击"},{"18格直线火符","半径4.5格持续6秒，每秒伤害并附霜印","蓄法1.2秒落雷；引爆霜印额外30%"},{"6格冲肩，推开沿途目标","半径5格震地、短暂击飞","6秒护体；PVE减伤50%、PVP25%；每秒至多反击一次"}};
        for(int i=0;i<3;i++)es.add(card(names[r.school-1][i],"skills cast "+i,effects[r.school-1][i]+String.format(java.util.Locale.ROOT," · 伤害 %.1f",Skills.power(p)*c.damage[r.school-1][i]*(1+Boosts.damage(r))*AscensionRules.damage(r,false)*(1+(r.school==1?0:MysticEnchants.effect(p,r.school==2?1:2,false))))+" · "+Skills.cost(r,i)+"灵力 · "+c.cooldowns[r.school-1][i]+"秒冷却 · "+Rules.realm(i)+"解锁","duskrain:artifact_"+r.school+"_"+(r.stage/3),r.stage>=i&&!Skills.charging(p)));
        String passive=new String[]{"","持剑普攻4.5格、剑伤+15%；满蓄力挥剑发出6格剑气，耗2灵力。剑意 "+Skills.COMBO.getOrDefault(p.getUUID(),0)+"/5；满层下一技能+30%。","灵力上限+50%，回灵+50%，消耗-15%；霜印配合雷诀。"+(Skills.charging(p)?"正在蓄法。":""),"生命+10，移速+25%，普通跳跃约2格，抗击退60%；可空手施法。"}[r.school];
        Network.menu(p,"流派技能 · "+Rules.current.schools[r.school],passive+" 主动攻击对玩家伤害65%。 "+Boosts.summary(p),es);
    }
    static String objective(Rules.Quest q){return switch(q.type()){
        case "submit"->"提交 "+Content.label(ForgeRegistries.ITEMS.getValue(new ResourceLocation(q.target())))+" × "+q.count();
        case "kill"->"击杀 "+Map.of("hostile","敌对怪物","minecraft:zombie","僵尸","minecraft:skeleton","骷髅","minecraft:spider","蜘蛛").getOrDefault(q.target(),q.target())+" × "+q.count();
        case "talk"->"拜访 "+Services.ALL.stream().filter(x->x.id().equals(q.target())).map(Services.Service::name).findFirst().orElse(q.target());case "practice"->"命中演武庭练功木人 × "+q.count();case "puzzle"->"破解 "+Trials.NAMES[Integer.parseInt(q.target())-1]+"的机关";case "craft"->"亲手合成 "+Content.label(ForgeRegistries.ITEMS.getValue(new ResourceLocation(q.target())));case "investigate"->"向任务殿闻溪打听旧事";case "visit"->"拜访 "+CityPlan.find(q.target()).name();case "trial"->"通关 "+(q.target().equals("any")?"任意":q.target()+"阶")+"试炼";default->q.type();};}
    static void quests(ServerPlayer p){Profile r=Store.of(p);List<Network.Entry> entries=new ArrayList<>();
        for(int i=0;i<24;i++){var q=Rules.current.main.get(i);boolean current=i==r.main,done=i<r.main;int progress=current?(q.type().equals("submit")?Gameplay.count(p,ForgeRegistries.ITEMS.getValue(new ResourceLocation(q.target()))):r.mainProgress):done?q.count():0;
            boolean ready=current&&progress>=q.count()&&r.stage>=q.stage(),local=Services.nearby(p,"quests");
            String status=done?"已领取":ready?(local?"可领取":"待交付"):current?"进行中":"未开启";
            String hint=done?"奖励已入账":ready?(local?"点击领取奖励":"点击前往任务殿，向闻溪领取"):current?"目标完成后到任务殿交付":"先完成前一项主线";
            entries.add(card(status+" · "+q.title(),ready&&!local?"warp quests":"quests claim "+i,hint+"；"+objective(q)+"；进度 "+progress+"/"+q.count()+"；要求 "+Rules.realm(q.stage())+"；奖励 "+q.xp()+"修为 / "+q.money()+"灵石","minecraft:writable_book",ready));}
        Network.menu(p,"烟雨纪事 · 六章仙途","依章前行：拜师、炼制、调查与解阵。随时查看进度；完成后前往任务殿，向闻溪交付并领取奖励。",entries);
    }
    static void daily(ServerPlayer p){
        Profile r=Store.of(p);List<Network.Entry> e=new ArrayList<>();boolean signed=r.lastSignDay==Profile.today();
        e.add(card(signed?"今日已签到":"领取今日签到","daily sign",Rules.current.dailyXp+"修为 · "+Rules.current.dailyMoney+"灵石","minecraft:clock",!signed));
        for(int i:r.dailies(p.getUUID())){var q=Rules.current.daily.get(i);int progress=q.type().equals("submit")?Gameplay.count(p,ForgeRegistries.ITEMS.getValue(new ResourceLocation(q.target()))):r.dailyProgress[i];boolean done=r.dailyClaimed.contains(i);
            boolean ready=!done&&progress>=q.count()&&r.stage>=q.stage(),local=Services.nearby(p,"quests");
            e.add(card((done?"已领取":ready?(local?"可领取":"待交付"):"进行中")+" · "+q.title(),ready&&!local?"warp quests":"daily claim "+i,(done?"奖励已入账":ready?(local?"点击领取奖励":"点击前往任务殿，向闻溪领取"):"完成目标后到任务殿交付")+"；"+objective(q)+"；进度 "+progress+"/"+q.count()+"；奖励 "+q.xp()+"修为 / "+q.money()+"灵石","minecraft:writable_book",ready));}
        Network.menu(p,"每日委托与签到","北京时间每日零点刷新。每位玩家分配三项委托；完成后到任务殿交付，签到可随时领取。",e);
    }
    static void sign(ServerPlayer p){Profile r=Store.of(p);if(r.lastSignDay==Profile.today()){Gameplay.say(p,"今天已经领取过签到奖励。");return;}r.lastSignDay=Profile.today();Gameplay.reward(p,Rules.current.dailyXp,Rules.current.dailyMoney);Gameplay.say(p,"签到成功！");daily(p);}
    static void claimQuest(ServerPlayer p,int id,boolean daily){if(!Services.require(p,"quests"))return;Profile r=Store.of(p);if(daily?(!r.dailies(p.getUUID()).contains(id)||r.dailyClaimed.contains(id)):(r.main!=id)){Gameplay.say(p,"任务已领取或当前不可领取。");return;}Rules.Quest q=(daily?Rules.current.daily:Rules.current.main).get(id);if(r.stage<q.stage()){Gameplay.say(p,"境界尚未满足任务要求。");return;}
        if(q.type().equals("submit")){Item item=ForgeRegistries.ITEMS.getValue(new ResourceLocation(q.target()));if(item==null||!Gameplay.take(p,item,q.count())){Gameplay.say(p,objective(q)+"，材料不足。");return;}}
        else if((daily?r.dailyProgress[id]:r.mainProgress)<q.count()){Gameplay.say(p,objective(q)+"，目标尚未完成。");return;}
        if(daily)r.dailyClaimed.add(id);else{r.main++;r.mainProgress=0;}Gameplay.reward(p,q.xp(),q.money());Gameplay.say(p,"奖励已领取："+q.title()+" · +"+q.xp()+"修为 / +"+q.money()+"灵石");if(daily)daily(p);else quests(p);
    }
    static void shop(ServerPlayer p,String category){
        ShopCatalog.open(p,category);
    }
    static void product(ServerPlayer p,String id){
        var match=Rules.current.shops.stream().filter(o->o.id().equals(id)).findFirst();if(match.isEmpty())return;var o=match.get();if(!Services.require(p,Services.shopService(o.shop())))return;Profile r=Store.of(p);Item item=ForgeRegistries.ITEMS.getValue(new ResourceLocation(o.item()));List<Network.Entry> e=new ArrayList<>();
        for(int amount:Boosts.upgrade(id)==null?new int[]{1,8,32}:new int[]{1}){long price=(long)o.buy()*amount;e.add(card("购买 "+(o.count()*amount)+" 件","shop buy "+id+" "+amount,"花费 "+price+" 灵石"+(r.money<price?" · 余额不足":""),o.item(),r.stage>=o.stage()&&r.money>=price&&Boosts.available(o,r)));}
        if(o.sell()>0)for(int amount:new int[]{1,8,32})e.add(card("出售 "+(o.count()*amount)+" 件","shop sell "+id+" "+amount,"获得 "+(o.sell()*amount)+" 灵石",o.item(),r.stage>=o.stage()&&Gameplay.count(p,item)>=o.count()*amount&&r.sold.getOrDefault(id,0)+o.count()*amount<=Rules.current.sellDailyLimit));
        e.add(entry("返回商品列表","shop view "+o.shop()));
        Network.menu(p,"商品 · "+Boosts.label(o),Boosts.detail(o,r)+"。要求："+Rules.realm(o.stage())+"。持有 "+Gameplay.count(p,item)+" 件；余额 "+r.money+" 灵石。"+(o.sell()>0?"今日回收余量 "+Math.max(0,Rules.current.sellDailyLimit-r.sold.getOrDefault(id,0))+" 件。":"本品不回收。")+"法器购买后自动绑定。",e);
    }
    static void trade(ServerPlayer p,String id,int amount,boolean sell){if(Boosts.purchase(p,id,amount,sell))return;Profile r=Store.of(p);var opt=Rules.current.shops.stream().filter(o->o.id().equals(id)).findFirst();if(opt.isEmpty())return;var o=opt.get();if(!Services.require(p,Services.shopService(o.shop())))return;if(r.stage<o.stage()){Gameplay.say(p,"商品需要"+Rules.realm(o.stage())+"。");return;}Item item=ForgeRegistries.ITEMS.getValue(new ResourceLocation(o.item()));if(item==null||item==Items.AIR)return;int count=Math.multiplyExact(o.count(),amount);
        if(sell){if(o.sell()<=0){Gameplay.say(p,"此物品不可回收。");return;}int done=r.sold.getOrDefault(id,0);if(done+count>Rules.current.sellDailyLimit){Gameplay.say(p,"今日该物品回收额度不足。");return;}if(!Gameplay.take(p,item,count)){Gameplay.say(p,"背包物品不足。");return;}r.sold.put(id,done+count);r.money=Math.min(1_000_000_000_000L,r.money+(long)o.sell()*amount);}
        else{long price=(long)o.buy()*amount;if(r.money<price){Gameplay.say(p,"灵石不足。");return;}ItemStack stack=new ItemStack(item,count);Content.bind(stack,p);if(!Gameplay.fits(p,stack)){Gameplay.say(p,"背包空间不足，未扣款。");return;}r.money-=price;p.getInventory().add(stack);}
        Store.get(p.server).setDirty();Network.sync(p);Gameplay.say(p,(sell?"回收":"购买")+"成功；余额 "+r.money+" 灵石。");product(p,id);
    }
    static void warps(ServerPlayer p){
        List<Network.Entry> e=new ArrayList<>();
        for(var b:CityPlan.BUILDINGS)if(!b.home())e.add(card(b.name(),"warp "+b.id(),(b.z()<-40?"上层仙宫平台":b.z()<45?"中层日常事务": "下层迎仙商街")+" · 主岛","minecraft:quartz_block",true));
        for(int i=0;i<CityPlan.ISLETS.length;i++)e.add(card(new String[]{"松风浮岛","听雨浮岛","望月浮岛"}[i],"warp island"+i,"观景亭 · 登岛栈桥","minecraft:cherry_sapling",true));
        e.add(card("前往生存野外","warp survival","野外开启 PVP，注意随身物资","minecraft:iron_sword",true));
        e.add(card("烟雨小集 · 玩家商铺","warp stalls","山门南侧12间小铺；购铺、寄售与玩家交易","minecraft:barrel",true));
        Network.menu(p,"云渡出行","主城内楼阁与浮岛可直接传送。野外回城需引导5秒，移动或受伤取消，战斗后15秒内不可回城。",e);
    }
    static void warp(ServerPlayer p,String id){
        if(id.equals("survival")){Gameplay.survival(p);return;}Vec3 destination;
        if(id.equals("stalls")){var s=PlayerMarket.SITES.get(0);destination=new Vec3(s.x()+.5,s.y()+1,s.z()+6.5);}
        else if(id.matches("island[0-2]")){int[] a=CityPlan.ISLETS[Integer.parseInt(id.substring(6))];destination=new Vec3(a[0]+.5,a[3]+3,a[1]+6.5);}
        else{var found=CityPlan.BUILDINGS.stream().filter(b->b.id().equals(id)&&!b.home()).findFirst();if(found.isEmpty()){Gameplay.say(p,"未知传送点。");return;}var b=found.get();destination=new Vec3(b.x()+.5,b.y()+1,b.z()+b.d()+.5);}
        if(p.level().dimension().equals(Gameplay.CITY)&&Store.of(p).combatUntil<System.currentTimeMillis()){TeleportEffects.travel(p,p.server.getLevel(Gameplay.CITY),destination,180);Gameplay.say(p,"已抵达。右键此处的村民或柜台办理事务。");}
        else Gameplay.recall(p,Gameplay.CITY,destination);
    }
    static void claimMenu(ServerPlayer p){
        Store data=Store.get(p.server);boolean wild=p.level().dimension().equals(Level.OVERWORLD);var current=wild?data.claims.get(p.chunkPosition().toLong()):null;boolean own=current!=null&&current.owner.equals(p.getUUID());
        List<Network.Entry> entries=new ArrayList<>();
        entries.add(card("前往生存野外","warp survival","主城不能认领；野外找空地后按 F3+G 可显示原版区块边界","minecraft:compass",true));
        entries.add(card("认领脚下区块","claim add","16×16；须与已有领地相邻","minecraft:grass_block",wild&&current==null&&data.claimCount(p.getUUID())<Rules.current.claimLimit));
        entries.add(card("放弃脚下区块","claim remove","只能从边缘放弃，不能切断剩余领地","minecraft:barrier",own));
        entries.add(card("设为家园落点","home set","需要坚实地面与两格净空","minecraft:cyan_bed",own));
        entries.add(card("返回家园","home","5秒引导，移动或受伤取消","minecraft:ender_pearl",Store.of(p).hasHome));
        entries.add(entry("我的区块清单","claim list"));entries.add(entry("成员授权管理","claim members"));entries.add(card("边界 · 自动显示","claim border auto","48格内显示外轮廓；自己的青色、成员金色、他人红色","minecraft:end_rod",true));entries.add(entry("边界 · 显示","claim border on"));entries.add(entry("边界 · 隐藏","claim border off"));
        Network.menu(p,"住宅领地 · "+data.claimCount(p.getUUID())+" / "+Rules.current.claimLimit,"脚下："+(wild?(current==null?"未认领":own?"你的领地":"他人领地"):"主城或独立维度")+"。外人不能拆建、开箱或炸毁住宅；领地内仍允许 PVP。",entries);
    }
    static void claimAdd(ServerPlayer p){if(!p.level().dimension().equals(Level.OVERWORLD)){Gameplay.say(p,"仅生存主世界可认领。");return;}Store s=Store.get(p.server);ChunkPos cp=p.chunkPosition();if(s.claims.containsKey(cp.toLong())||s.claimCount(p.getUUID())>=Rules.current.claimLimit||!s.adjacent(p.getUUID(),cp)){Gameplay.say(p,"无法认领：已被占用、超过上限或不相邻。");return;}var claim=new Store.Claim(p.getUUID());claim.members.addAll(LandClaims.trusted(s,p.getUUID()));s.claims.put(cp.toLong(),claim);s.setDirty();Gameplay.say(p,"已认领当前区块；已有住宅成员授权自动继承。");claimMenu(p);}
    static void claimRemove(ServerPlayer p){Store s=Store.get(p.server);long pos=p.chunkPosition().toLong();Store.Claim c=s.claims.get(pos);if(!p.level().dimension().equals(Level.OVERWORLD)||c==null||!c.owner.equals(p.getUUID()))return;
        Set<Long> remaining=new HashSet<>();s.claims.forEach((k,v)->{if(k!=pos&&v.owner.equals(p.getUUID()))remaining.add(k);});if(!remaining.isEmpty()){Set<Long> reached=new HashSet<>();ArrayDeque<Long> q=new ArrayDeque<>();q.add(remaining.iterator().next());while(!q.isEmpty()){long k=q.pop();if(!reached.add(k))continue;ChunkPos cp=new ChunkPos(k);for(int[] a:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){long n=ChunkPos.asLong(cp.x+a[0],cp.z+a[1]);if(remaining.contains(n)&&!reached.contains(n))q.add(n);}}if(reached.size()!=remaining.size()){Gameplay.say(p,"不能让剩余领地分裂，请从边缘放弃。");return;}}
        s.claims.remove(pos);Profile profile=Store.of(p);if(profile.hasHome&&new ChunkPos(BlockPos.containing(profile.homeX,profile.homeY,profile.homeZ)).toLong()==pos)profile.hasHome=false;s.setDirty();Gameplay.say(p,"已放弃当前区块。");claimMenu(p);
    }
    static void trust(ServerPlayer p,ServerPlayer target,boolean yes){if(p==target)return;Store s=Store.get(p.server);s.claims.values().stream().filter(c->c.owner.equals(p.getUUID())).forEach(c->{if(yes)c.members.add(target.getUUID());else c.members.remove(target.getUUID());});s.setDirty();Gameplay.say(p,(yes?"已授权：":"已移除授权：")+target.getGameProfile().getName());}
    static void homeSet(ServerPlayer p){Store.Claim c=Store.get(p.server).claims.get(p.chunkPosition().toLong());if(!p.level().dimension().equals(Level.OVERWORLD)||c==null||!c.owner.equals(p.getUUID())){Gameplay.say(p,"请站在自己的领地中设置家园。");return;}if(!SafeLanding.valid(p.serverLevel(),p.blockPosition())){Gameplay.say(p,"家园必须有稳固地面、两格净空，且不在水火中。");return;}Profile r=Store.of(p);r.hasHome=true;r.homeX=p.getBlockX()+.5;r.homeY=p.getBlockY();r.homeZ=p.getBlockZ()+.5;r.homeDim=p.level().dimension().location().toString();Gameplay.say(p,"家园落点已保存。");}
    static void home(ServerPlayer p){Profile r=Store.of(p);if(!r.hasHome){Gameplay.say(p,"还未设置家园。");return;}var key=Gameplay.key(r.homeDim);var l=p.server.getLevel(key);BlockPos pos=BlockPos.containing(r.homeX,r.homeY,r.homeZ);if(l!=null)l.getChunkAt(pos);Store.Claim c=Store.get(p.server).claims.get(new ChunkPos(pos).toLong());if(!key.equals(Level.OVERWORLD)||l==null||c==null||!c.owner.equals(p.getUUID())||!SafeLanding.valid(l,pos)){Gameplay.say(p,"家园落点失效、被占用或不再属于你。");return;}Gameplay.recall(p,key,new Vec3(r.homeX,r.homeY,r.homeZ));}
    static void party(ServerPlayer p){List<Network.Entry> es=new ArrayList<>();es.add(entry("接受邀请","party accept"));es.add(entry("离开或解散队伍","party leave"));for(ServerPlayer other:p.server.getPlayerList().getPlayers())if(other!=p)es.add(entry("邀请 "+other.getGameProfile().getName(),"party invite "+other.getGameProfile().getName()));Network.menu(p,"试炼队伍 · "+Trials.members(p.getUUID()).size()+"/4","由队长发起试炼；全员须在线并位于主城。",es);}
    static void trials(ServerPlayer p){if(!Services.require(p,"trial"))return;show(p,"问劫试炼","低阶炼气、中阶金丹、高阶化神。每场三道机关、三波战斗与原创首领，最长15分钟，最多4人。",entry("青竹迷踪 · 炼气","trial enter 1"),entry("玄岩机枢 · 金丹","trial enter 2"),entry("雷台问劫 · 化神","trial enter 3"),card("登神台 · 神纹挑战","trial enter "+Math.max(5,Math.min(6,Store.of(p).stage-13)),"神境可重复挑战获取神纹晶；突破请到问道书院","minecraft:nether_star",Store.of(p).stage>=18),entry("队伍管理","party"),entry("退出当前试炼／队列","trial leave"));}
    static void sidebar(ServerPlayer p){show(p,"侧栏设置","DuskRain · 群号205255670。信息使用真实服务端数据。",entry("显示／隐藏","sidebar toggle"),entry("紧凑 0.75倍","sidebar scale 0.75"),entry("默认 1倍","sidebar scale 1"),entry("大号 1.25倍","sidebar scale 1.25"));}
    static void admin(ServerPlayer p){show(p,"DuskRain 管理","仅OP等级3及以上可用；管理操作请先备份世界。",entry("维护模式开关","admin maintenance"),entry("重新加载规则","admin reload"),entry("预生成主城","admin pregen"),entry("运行数据审计","admin audit"),entry("观看自动建造","build"));}
}

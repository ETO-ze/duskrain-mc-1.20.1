from pathlib import Path
p=Path(__file__).resolve().parents[1]/'mod/src/main/java/cn/duskrain/DRCommands.java'
s=p.read_text(encoding='utf-8')
def replace_method(name,nextname,new):
    global s
    a=s.index('    static '+name);b=s.index('    static '+nextname,a)
    s=s[:a]+new+'\n'+s[b:]
replace_method('void profile(', 'void choose(', '''    static void profile(ServerPlayer p){
        Profile r=Store.of(p);List<Network.Entry> a=new ArrayList<>();
        if(r.school==0)for(int i=1;i<=3;i++)a.add(card("选择"+Rules.current.schools[i],"cultivate choose "+i,new String[]{"","近战剑势 · 速度与连击","元素法术 · 灵力与范围","近战护体 · 生命与韧性"}[i],"duskrain:artifact_"+i+"_0",true));
        a.add(card(r.meditating?"停止调息":"静坐调息","cultivate sit","保持静止；每"+Rules.current.meditateIntervalSeconds+"秒获得"+Rules.current.meditateXp+"修为","minecraft:amethyst_shard",r.school>0));
        String need="修为 "+r.xp+" / "+Rules.current.stageXp[r.stage];
        if(r.stage%3==2&&r.stage<17)need+="；材料 "+new String[]{"铁锭 × 8","金锭 × 8","钻石 × 4","末影之眼 × 4","下界之星 × 1"}[r.stage/3]+"；单人突破试炼";
        a.add(card("突破境界","cultivate breakthrough",need,"minecraft:nether_star",r.school>0&&r.stage<17&&r.xp>=Rules.current.stageXp[r.stage]));
        a.add(card("查看流派技能","skills","技能效果、灵力消耗和冷却","minecraft:diamond_sword",r.school>0));
        Network.menu(p,"修炼功法 · "+Rules.realm(r.stage),"流派："+Rules.current.schools[r.school]+"。"+need+"。突破失败不掉境界；挑战开始时消耗材料。",a);
    }''')
replace_method('void skills(', 'String objective(', '''    static void skills(ServerPlayer p){
        Profile r=Store.of(p);if(r.school==0){profile(p);return;}List<Network.Entry> es=new ArrayList<>();
        String[][] effects={{"正前方剑气伤害","周身剑阵打击","震退周围敌人"},{"正前方灵焰灼烧","寒霜减速范围攻击","落雷震退范围攻击"},{"正前方崩山拳","短暂抗性与护体","震退周围敌人"}};
        for(int i=0;i<3;i++)es.add(card(Rules.current.skills[r.school-1][i],"skills cast "+i,effects[r.school-1][i]+" · "+Rules.current.skillCost[i]+"灵力 · "+Rules.current.skillCooldown[i]+"秒冷却","duskrain:artifact_"+r.school+"_"+(r.stage/3),true));
        String passive=new String[]{"","剑心：攻击增强；御风：移速提高；归元：击杀回复生命。","灵泉：灵力恢复；法脉：施法耗蓝降低；慧心：击杀修为增加。","铁骨：伤害减免；长生：生命上限提升；战意：低生命伤害增强。"}[r.school];
        Network.menu(p,"流派技能 · "+Rules.current.schools[r.school],"手持本命法器，按 Z / X / C 施放。"+passive,es);
    }''')
replace_method('String objective(', 'void quests(', '''    static String objective(Rules.Quest q){return switch(q.type()){
        case "submit"->"提交 "+Content.label(ForgeRegistries.ITEMS.getValue(new ResourceLocation(q.target())))+" × "+q.count();
        case "kill"->"击杀 "+Map.of("hostile","敌对怪物","minecraft:zombie","僵尸","minecraft:skeleton","骷髅","minecraft:spider","蜘蛛").getOrDefault(q.target(),q.target())+" × "+q.count();
        case "visit"->"拜访 "+CityPlan.find(q.target()).name();case "trial"->"通关 "+(q.target().equals("any")?"任意":q.target()+"阶")+"试炼";default->q.type();};}''')
replace_method('void daily(', 'void sign(', '''    static void daily(ServerPlayer p){
        Profile r=Store.of(p);List<Network.Entry> e=new ArrayList<>();boolean signed=r.lastSignDay==Profile.today();
        e.add(card(signed?"今日已签到":"领取今日签到","daily sign",Rules.current.dailyXp+"修为 · "+Rules.current.dailyMoney+"灵石","minecraft:clock",!signed));
        for(int i:r.dailies(p.getUUID())){var q=Rules.current.daily.get(i);int progress=q.type().equals("submit")?Gameplay.count(p,ForgeRegistries.ITEMS.getValue(new ResourceLocation(q.target()))):r.dailyProgress[i];boolean done=r.dailyClaimed.contains(i);
            e.add(card((done?"已完成 · ":"")+q.title(),"daily claim "+i,objective(q)+"；进度 "+progress+"/"+q.count()+"；奖励 "+q.xp()+"修为 / "+q.money()+"灵石","minecraft:writable_book",!done&&progress>=q.count()));}
        Network.menu(p,"每日委托与签到","北京时间每日零点刷新。每位玩家分配三项委托。悬停卡片查看具体目标；完成后按钮自动可领取。",e);
    }''')
replace_method('void shop(', 'void trade(', '''    static void shop(ServerPlayer p,String category){
        List<Network.Entry> e=new ArrayList<>();e.add(card("材料铺","shop view material","基础材料与物品回收","minecraft:iron_ingot",true));e.add(card("青炉丹坊","shop view alchemy","六种丹药","duskrain:pill_0",true));e.add(card("流光器阁","shop view forge","三流派 · 六境法器","duskrain:artifact_1_0",true));
        for(var o:Rules.current.shops)if(o.shop().equals(category)){Item item=ForgeRegistries.ITEMS.getValue(new ResourceLocation(o.item()));if(item!=null)e.add(card(Content.label(item),"shop inspect "+o.id(),o.buy()+"灵石 / "+o.count()+"件 · "+Rules.realm(o.stage())+"解锁",o.item(),true));}
        Network.menu(p,"灵石商店 · "+switch(category){case "alchemy"->"青炉丹坊";case "forge"->"流光器阁";default->"材料铺";},"选择商品查看详情和购买数量。余额、境界与背包空间均由服务器核验；回收只接受白名单物品。",e);
    }
    static void product(ServerPlayer p,String id){
        var match=Rules.current.shops.stream().filter(o->o.id().equals(id)).findFirst();if(match.isEmpty())return;var o=match.get();Profile r=Store.of(p);Item item=ForgeRegistries.ITEMS.getValue(new ResourceLocation(o.item()));List<Network.Entry> e=new ArrayList<>();
        for(int amount:new int[]{1,8,32}){long price=(long)o.buy()*amount;e.add(card("购买 "+(o.count()*amount)+" 件","shop buy "+id+" "+amount,"花费 "+price+" 灵石"+(r.money<price?" · 余额不足":""),o.item(),r.stage>=o.stage()&&r.money>=price));}
        if(o.sell()>0)for(int amount:new int[]{1,8,32})e.add(card("出售 "+(o.count()*amount)+" 件","shop sell "+id+" "+amount,"获得 "+(o.sell()*amount)+" 灵石",o.item(),r.stage>=o.stage()&&Gameplay.count(p,item)>=o.count()*amount&&r.sold.getOrDefault(id,0)+o.count()*amount<=Rules.current.sellDailyLimit));
        e.add(entry("返回商品列表","shop view "+o.shop()));
        Network.menu(p,"商品 · "+Content.label(item),"要求："+Rules.realm(o.stage())+"。持有 "+Gameplay.count(p,item)+" 件；余额 "+r.money+" 灵石。"+(o.sell()>0?"今日回收余量 "+Math.max(0,Rules.current.sellDailyLimit-r.sold.getOrDefault(id,0))+" 件。":"本品不回收。")+"法器购买后自动绑定。",e);
    }''')
replace_method('void warps(', 'void claimMenu(', '''    static void warps(ServerPlayer p){
        List<Network.Entry> e=new ArrayList<>();
        for(var b:CityPlan.BUILDINGS)if(!b.home())e.add(card(b.name(),"warp "+b.id(),(b.z()<-40?"上层仙宫平台":b.z()<45?"中层日常事务": "下层迎仙商街")+" · 主岛","minecraft:quartz_block",true));
        for(int i=0;i<CityPlan.ISLETS.length;i++)e.add(card(new String[]{"松风浮岛","听雨浮岛","望月浮岛"}[i],"warp island"+i,"观景亭 · 登岛栈桥","minecraft:cherry_sapling",true));
        e.add(card("前往生存野外","warp survival","野外开启 PVP，注意随身物资","minecraft:iron_sword",true));
        Network.menu(p,"云渡出行","主城内楼阁与浮岛可直接传送。野外回城需引导5秒，移动或受伤取消，战斗后15秒内不可回城。",e);
    }
    static void warp(ServerPlayer p,String id){
        if(id.equals("survival")){Gameplay.survival(p);return;}Vec3 destination;
        if(id.matches("island[0-2]")){int[] a=CityPlan.ISLETS[Integer.parseInt(id.substring(6))];destination=new Vec3(a[0]+.5,a[3]+3,a[1]+6.5);}
        else{var found=CityPlan.BUILDINGS.stream().filter(b->b.id().equals(id)&&!b.home()).findFirst();if(found.isEmpty()){Gameplay.say(p,"未知传送点。");return;}var b=found.get();destination=new Vec3(b.x()+.5,b.y()+1,b.z()+b.d()+.5);}
        if(p.level().dimension().equals(Gameplay.CITY)&&Store.of(p).combatUntil<System.currentTimeMillis()){p.teleportTo(p.server.getLevel(Gameplay.CITY),destination.x,destination.y,destination.z,180,0);p.fallDistance=0;Gameplay.say(p,"已抵达。按 Esc 收起仙途录即可游览。");}
        else Gameplay.recall(p,Gameplay.CITY,destination);
    }''')
s=s.replace('Gameplay.say(p,"签到成功！");','Gameplay.say(p,"签到成功！");daily(p);')
s=s.replace('Gameplay.say(p,(sell?"回收":"购买")+"成功；余额 "+r.money+" 灵石。");','Gameplay.say(p,(sell?"回收":"购买")+"成功；余额 "+r.money+" 灵石。");product(p,id);')
p.write_text(s,encoding='utf-8')

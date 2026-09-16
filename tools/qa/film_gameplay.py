from film_control import *

def at(x,y,z,yaw=180,pitch=0):return f'cmd execute in duskrain:city run tp @s {x} {y} {z} {yaw} {pitch}'
def school(n):
    send('close\ncmd gamemode survival\n'+at(-30.5,82,2.5)+'\ncmd dr admin reset_school DuskRainDirector\ncmd dr cultivate choose '+str(n),3)
    item=f'duskrain:artifact_{n}_5' if n!=3 else 'minecraft:air'
    send('cmd item replace entity @s weapon.mainhand with '+item+'\n'+at(84.5,71,78.5,180,6),3)

ui('12-menu','主菜单与修炼入口', 'cmd gamemode survival\n'+at(0.5,81,28.5)+'\ncmd dr menu',14)
ui('13-quests','六章主线与当前任务进度',at(39.5,80,-1.5)+'\ncmd dr quests',16)
ui('14-daily','每日委托与签到', 'cmd dr daily',14)

send(at(82.5,75,25.5)+'\ncmd dr shop page "forge:robe:-1:all" 0',3)
assert status()['screen']=='MerchantScreen',status()
begin('15-outfits');time.sleep(5)
for i in range(3):send('ui_click 下页 →',4)
send('ui_click ← 上页',4)
end('15-outfits','法衣商店真实分页与商品展示');send('close')
ui('16-elixir','灵药商与限时强化',at(-86.5,78,40.5)+'\ncmd dr shop view elixir',14)
ui('17-treasure','珍宝商与永久强化',at(-40.5,78,71.5)+'\ncmd dr shop view treasure',14)

for n,name in [(1,'18-sword'),(2,'19-mage'),(3,'20-body')]:
    school(n);send('close\nshowhud\ncmd time set 6000');time.sleep(8)
    begin(name)
    send('cmd dr skills cast 0',4)
    send('cmd dr skills cast 1',6)
    send('cmd dr skills cast 2',6)
    send('cmd dr skills cast 0',4)
    end(name,['','剑修：破空、回风与追星','术修：离火、寒霜与九霄雷诀','体修：冲肩、震地与金刚护体'][n])
    if n==3:
        send(at(244.5,240,.5,-90,0));send('motion final-body-sprint-double 80');time.sleep(6)

send('close\n'+at(0.5,81,28.5)+'\ncmd dr warp survival',12)
assert status().get('dimension')=='minecraft:overworld',status()
ui('21-claim-menu','野外住宅认领与权限菜单','cmd dr claim',12)
send('cmd dr claim add',3);send('close\ncmd dr claim border on');time.sleep(5)
pos=status()['position']
# Keep the actual claim and its surface in view.
a=orient(pos[0]+8,pos[1]+10,pos[2]+13,pos[0],pos[1],pos[2]);b=orient(pos[0]-5,pos[1]+10,pos[2]+13,pos[0],pos[1],pos[2])
scene('22-claim-border','生存野外与住宅可视边界',a,b,20,'minecraft:overworld')

send('close\ncmd gamemode survival\n'+at(0.5,81,28.5)+'\ncmd dr guild enter',10)
assert status().get('dimension')=='duskrain:guild',status()
ui('23-guild-menu','帮派管理与洞天入口','cmd dr guild',14)
pos=status()['position'];a=orient(pos[0]+18,83,pos[2]+23,pos[0],66,pos[2]);b=orient(pos[0]-10,85,pos[2]+23,pos[0],66,pos[2])
scene('24-guild','帮派专属空间与入口',a,b,22,'duskrain:guild')
send('close\ncmd gamemode survival\n'+at(172.5,139,-105.5)+'\ncmd dr trial',3)
ui('25-trial-menu','三座试炼与组队入口','cmd dr trial',14)
send('close\ncmd dr trial enter 1',6)
assert status().get('dimension')=='duskrain:trial',status()
send('cmd time set 6000\nhidehud')
begin('26-trial-hint');time.sleep(16);end('26-trial-hint','青竹迷踪：原生试炼入场与风铃光柱')
send('shot trial-hint-review\nshowhud')

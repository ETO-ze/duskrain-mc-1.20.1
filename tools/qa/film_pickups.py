"""Retake unobstructed boundaries and keep the camera inside the guild's boundary."""
from film_control import *

scene('22-claim-border','生存野外与住宅可视边界',orient(102,77,-105,80,65,-132),orient(106,79,-116,80,65,-132),22,'minecraft:overworld')
send('cinema off\ncmd gamemode survival\ncmd dr spawn',12)
send('close\ncmd dr guild enter',12)
assert status()['dimension']=='duskrain:guild',status()
scene('24-guild','帮派专属建设空间与返回阵',orient(1045,88,90,1024,65,65),orient(1011,92,86,1024,65,20),24,'duskrain:guild')
send('cinema off\ncmd gamemode survival',6)
send('cmd dr guild return',12)
send('close\ncmd tp @s 172.5 139 -105.5 90 0',5)
send('cmd dr trial enter 1',10)
assert status()['dimension']=='duskrain:trial',status()
scene('26-trial-hint','青竹迷踪：风铃阵眼与引导光柱',orient(-9,69,21,-23,68,10),orient(-16,70,18,-23,68,10),21,'duskrain:trial')
send('cinema off\ncmd dr trial leave',4)
send('showhud')

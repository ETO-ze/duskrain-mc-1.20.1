"""Author small presets using keys verified against the isolated JourneyMap 6.0.5 runtime."""
from mod_stack import ROOT,write_json

client=ROOT/'assets/v22-client-defaults'
jm=client/'journeymap/config/6.0'
minimap={"enabled":"true","active":True,"position":"TopLeft","sizePercent":"18","shape":"Circle",
    "hudRenderLayer":"BeforeEffects",
    "frameAlpha":"75","terrainAlpha":"88","backgroundAlpha":"0.5","infoSlotFontScale":"1.0",
    "info1Label":"jm.theme.labelsource.blank","info2Label":"jm.theme.labelsource.blank",
    "info3Label":"jm.theme.labelsource.location","info3LabelPosition":"Bottom",
    "info4Label":"duskrain.map.region","info4LabelPosition":"Bottom",
    "showPlayers":"false","showPlayerNames":"false","showOffScreenPlayers":"false",
    "showMobs":"false","showAnimals":"false","showVillagers":"false","showPets":"false",
    "showGrid":"false","showWaypointLabels":"false","waypointIconScale":"0.75","zoomLevel":"256",
    "configVersion":"6.0.5"}
write_json(jm/'journeymap.minimap.config',minimap)
write_json(jm/'journeymap.minimap2.config',dict(minimap,active=False,shape="Square"))
write_json(jm/'journeymap.core.config',{"themeName":"Purist","allowMiniMapBehindScreens":"false",
    "renderDistanceSurfaceMax":"4","renderDistanceCaveMax":"2","maxPlayersData":"0",
    "announceMod":"false","optionsManagerViewed":"","splashViewed":"","configVersion":"6.0.5"})
write_json(jm/'journeymap.fullmap.config',{"showPlayers":"false","showOffScreenPlayers":"false",
    "showPlayerNames":"false","showMobs":"false","showAnimals":"false","showVillagers":"false",
    "showWaypoints":"true","showWaypointLabels":"true","configVersion":"6.0.5"})
write_json(jm/'journeymap.waypoint.config',{"renderWaypointsWorld":"false","renderWaypointsMap":"true",
    "createDeathpoints":"true","configVersion":"6.0.5"})
write_json(jm/'journeymap.webmap.config',{"enabled":"false","configVersion":"6.0.5"})
write_json(client/'config/jade/jade.json',{"general":{"bossBarOverlapMode":"PUSH_DOWN","itemModNameTooltip":False,
    "hideFromDebug":True,"hideFromTabList":True},"overlay":{"alpha":0.6,"overlayScale":0.8,
    "overlayPosX":0.5,"overlayPosY":1.0,"overlayAnchorX":0.5,"overlayAnchorY":0.0,"activeTheme":"jade:dark"}})
entities=['duskrain:'+x for x in ('resident','flying_sword','wood_guardian','rock_guardian','storm_dragon','divine_sentinel')]
write_json(client/'config/entityculling.json',{"configVersion":8,"renderNametagsThroughWalls":False,
    "entityWhitelist":entities,"tickCulling":False,"tickCullingWhitelist":entities})
write_json(client/'CustomSkinLoader/CustomSkinLoader.json',{"version":"14.28","buildNumber":0,
    "loadlist":[{"name":"ServerProfile","type":"GameProfile"},{"name":"DuskRain","type":"MojangAPI",
    "apiRoot":"https://skin.duskrain.cn/account/","sessionRoot":"https://skin.duskrain.cn/session/"}],
    "enableCape":True,"enableTransparentSkin":True,"enableLogStdOut":False,"enableLocalProfileCache":False})

server=ROOT/'assets/v22-server-defaults'
sjm=server/'journeymap/server/6.0'
policy={"worldPlayerRadar":"NONE","seeUndergroundPlayers":"NONE","opsSeeHiddenPlayers":"false",
    "playerRadarEnabled":"false","playerRadarNamesEnabled":"false","radarEnabled":"NONE",
    "caveMapping":"NONE","surfaceMapping":"ALL","topoMapping":"ALL","biomeMapping":"ALL",
    "teleport":"NONE","teleportEnabled":"false","crossDimTeleport":"false","allowDeathPoints":"true",
    "minimapEnabled":"true","hideCoordinates":"false","globalWaypointsOnly":"false",
    "surfaceRenderRange":"4","caveRenderRange":"0","mobRadarEnabled":"false",
    "villagerRadarEnabled":"false","animalRadarEnabled":"false",
    "showInGameBeacons":"false","allowWaypoints":"true","viewOnlyServerProperties":"false",
    "configVersion":"6.0.5"}
write_json(sjm/'journeymap.server.global.config',policy)
for dimension in ('trial','arrival','construction'):
    write_json(sjm/f'journeymap.server.duskrain~{dimension}.config',dict(policy,enabled="true",
        minimapEnabled="false",surfaceMapping="NONE",topoMapping="NONE",biomeMapping="NONE"))
(server/'defaultconfigs').mkdir(exist_ok=True)
(server/'defaultconfigs/journeymap-server.toml').write_text('[admins]\nopAccess = true\nserverAdmins = []\n',encoding='utf8')
print('Authored client/server presets. No world IDs, map caches, account tokens or player waypoints included.')

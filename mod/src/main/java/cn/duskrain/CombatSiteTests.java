package cn.duskrain;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
@GameTestHolder(DuskRain.ID) @PrefixGameTestTemplate(false)
public final class CombatSiteTests {
 @GameTest(template="empty") public static void range_upgrade_preserves_custom_tuning(GameTestHelper h){
  CombatRules c=new CombatRules();c.ranges=new double[][]{{14,7,8},{18,4.5,16},{6,5,0}};c.normalQiRange=11;c.damage[0][0]=9;
  h.assertTrue(CombatRules.upgrade(c)&&c.ranges[0][0]==16&&c.ranges[0][1]==7&&c.normalQiRange==11&&c.damage[0][0]==9,"Upgrade replaces only old default ranges; custom balancing survives");
  h.assertTrue(!CombatRules.upgrade(c),"Migration is applied only once");c.validate();h.succeed();
 }
 @GameTest(template="empty") public static void road_decks_and_shops_above_water(GameTestHelper h){
  for(var r:CityPlan.ROUTES)if(!r.bridge()){int n=Math.max(Math.abs(r.bx()-r.ax()),Math.abs(r.bz()-r.az()));for(int i=0;i<=n;i++){int x=r.ax()+Integer.signum(r.bx()-r.ax())*i,z=r.az()+Integer.signum(r.bz()-r.az())*i;h.assertTrue(CityPlan.pathY(r,i)>Terrain.water(x,z),"Deck must stand above water at "+x+","+z);if(i>0)h.assertTrue(Math.abs(CityPlan.pathY(r,i)-CityPlan.pathY(r,i-1))<=1,"Water grading must preserve traversable stairs");}}
  for(var b:CityPlan.BUILDINGS)h.assertTrue(b.y()>Terrain.water(b.x(),b.z()),"No public building floor below water");
  for(var s:PlayerMarket.SITES)h.assertTrue(s.y()>Terrain.water(s.x(),s.z()),"Shop floors remain dry");h.succeed();
 }
 @GameTest(template="empty") public static void flooded_walkway_is_not_a_pass(GameTestHelper h){
  var l=h.getLevel();BlockPos p=h.absolutePos(new BlockPos(2,1,2));l.setBlock(p,Blocks.STONE.defaultBlockState(),2);l.setBlock(p.above(),Blocks.WATER.defaultBlockState(),2);
  h.assertTrue(Double.isNaN(WalkAudit.surface(l,p.getX(),p.getZ(),p.getY()+1)),"Water on a collision-valid floor must fail walking audit");
  l.setBlock(p.above(),Blocks.AIR.defaultBlockState(),2);h.assertTrue(!Double.isNaN(WalkAudit.surface(l,p.getX(),p.getZ(),p.getY()+1)),"Dry floor is walkable");h.succeed();
 }
 @GameTest(template="combat_space") public static void area_respects_radius_and_wall(GameTestHelper h){
  var l=h.getLevel();var p=IntegrationTests.player(h,"DRRangeBoundary");Vec3 at=Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(12,3,12)));p.setPos(at.x,at.y,at.z);
  Zombie inside=new Zombie(EntityType.ZOMBIE,l),outside=new Zombie(EntityType.ZOMBIE,l),wall=new Zombie(EntityType.ZOMBIE,l);
  inside.setPos(at.x+4,at.y,at.z);outside.setPos(at.x+8,at.y,at.z);wall.setPos(at.x-4,at.y,at.z);
  for(var z:new Zombie[]{inside,outside,wall}){z.setNoAi(true);l.addFreshEntity(z);}
  BlockPos b=BlockPos.containing(at.add(-2,0,0));for(int y=0;y<3;y++)l.setBlock(b.above(y),Blocks.STONE.defaultBlockState(),2);
  try{Skills.area(p,at,5.5,8,0);h.assertTrue(inside.getHealth()<20,"Visible target inside true radius takes damage");h.assertTrue(outside.getHealth()==20,"Outside target does not take cosmetic-ring damage");h.assertTrue(wall.getHealth()==20,"Wall blocks area attack");}finally{for(var z:new Zombie[]{inside,outside,wall})z.discard();for(int y=0;y<3;y++)l.setBlock(b.above(y),Blocks.AIR.defaultBlockState(),2);}h.succeed();
 }
}

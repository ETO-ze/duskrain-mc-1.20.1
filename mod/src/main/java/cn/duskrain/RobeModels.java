package cn.duskrain;
import java.util.*;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
/** Articulated robe panels use the player's existing pose and never replace their skin. */
public final class RobeModels {
 static final Map<Integer,HumanoidModel<LivingEntity>> CACHE=new HashMap<>();
 public static HumanoidModel<?> model(LivingEntity wearer,EquipmentSlot slot,int stage,HumanoidModel<?> original){int school=1;var mc=net.minecraft.client.Minecraft.getInstance();if(wearer==mc.player&&ClientUI.snapshot!=null)school=ClientUI.snapshot.school();else{var title=FlightClient.titles.get(wearer.getUUID());if(title!=null)school=title.school();else if(wearer.getMainHandItem().getItem() instanceof Content.Artifact a)school=a.school;}
  int realm=stage/3;var m=CACHE.computeIfAbsent(realm*4+school,k->create(k/4,k%4));((HumanoidModel)original).copyPropertiesTo(m);m.setAllVisible(false);switch(slot){case HEAD->{m.head.visible=true;m.hat.visible=true;}case CHEST->{m.body.visible=true;m.rightArm.visible=true;m.leftArm.visible=true;}case LEGS->{m.body.visible=true;m.rightLeg.visible=true;m.leftLeg.visible=true;}case FEET->{m.rightLeg.visible=true;m.leftLeg.visible=true;}default->{}}
  for(String part:List.of("lapel_left","lapel_right","cape","stole_left","stole_right","shoulder_mantle"))m.body.getChild(part).visible=slot==EquipmentSlot.CHEST;
  m.body.getChild("skirt_back").visible=slot==EquipmentSlot.LEGS;m.body.getChild("pendant").visible=slot==EquipmentSlot.CHEST;
  for(var leg:List.of(m.rightLeg,m.leftLeg)){leg.getChild("panel").visible=slot==EquipmentSlot.LEGS;leg.getChild("cloth").visible=slot==EquipmentSlot.LEGS;leg.getChild("boot_tip").visible=slot==EquipmentSlot.FEET;}
  float age=wearer.tickCount; m.body.getChild("pendant").zRot=(float)Math.sin(age*.08)*.035f;m.body.getChild("cape").xRot=.08f+(float)Math.sin(age*.06)*.025f;
  return m;
 }
 static HumanoidModel<LivingEntity> create(int realm,int school){var mesh=HumanoidModel.createMesh(new CubeDeformation(.45f),0);var r=mesh.getRoot();var head=r.addOrReplaceChild("head",CubeListBuilder.create(),PartPose.ZERO);r.addOrReplaceChild("hat",CubeListBuilder.create(),PartPose.ZERO);
  head.addOrReplaceChild("band",CubeListBuilder.create().texOffs(64,16).addBox(-4.4f,-7.4f,-4.4f,8.8f,.8f,8.8f),PartPose.ZERO);
  head.addOrReplaceChild("crown",CubeListBuilder.create().texOffs(64,0).addBox(-1.4f,-10-realm*.45f,-1.5f,2.8f,2.6f+realm*.45f,3),PartPose.ZERO);
  if(realm>0)for(int side:new int[]{-1,1})head.addOrReplaceChild("wing"+side,CubeListBuilder.create().texOffs(64,16).addBox(side<0?-3.7f:1.5f,-9.3f-realm*.25f,-.5f,2.2f,1.1f+realm*.2f,1.2f),PartPose.rotation(0,0,side*.18f));
  head.addOrReplaceChild("ribbon",CubeListBuilder.create().texOffs(96,0).addBox(-.7f,-4,4.2f,1.4f,7+realm*.6f,.35f),PartPose.ZERO);
  var body=r.getChild("body");body.addOrReplaceChild("lapel_left",CubeListBuilder.create().texOffs(64,32).addBox(-.8f,0,-2.7f,1.6f,8,.5f),PartPose.offsetAndRotation(-3,0,0,0,0,-.70f));body.addOrReplaceChild("lapel_right",CubeListBuilder.create().texOffs(64,32).addBox(-.6f,0,-2.8f,1.2f,8,.4f),PartPose.offsetAndRotation(3,0,0,0,0,.70f));
  body.addOrReplaceChild("belt",CubeListBuilder.create().texOffs(64,48).addBox(-4.65f,9.2f,-2.7f,9.3f,1.6f,5.4f),PartPose.ZERO);
  body.addOrReplaceChild("pendant",CubeListBuilder.create().texOffs(96,16).addBox(-.6f,0,-.4f,1.2f,2.8f,.8f).texOffs(96,0).addBox(-.3f,2.7f,-.3f,.6f,2.8f,.6f),PartPose.offset(3.8f,10.5f,-2.9f));
  body.addOrReplaceChild("skirt_back",CubeListBuilder.create().texOffs(80,68).addBox(-4.5f,10,2.5f,9,school==2?9:school==3?3.5f:7,.6f),PartPose.ZERO);
  body.addOrReplaceChild("cape",realm>=4?CubeListBuilder.create().texOffs(80,88).addBox(-4.8f,1,2.9f,9.6f,realm==4?10:17,.4f):CubeListBuilder.create(),PartPose.ZERO);
  for(int side:new int[]{-1,1})body.addOrReplaceChild(side<0?"stole_left":"stole_right",realm>=2?CubeListBuilder.create().texOffs(80,68).addBox(side<0?-4.8f:2.9f,1,-3.1f,1.9f,realm==2?12:realm==3?17:14,.3f):CubeListBuilder.create(),PartPose.ZERO);
  body.addOrReplaceChild("shoulder_mantle",realm>=4?CubeListBuilder.create().texOffs(64,32).addBox(-5.5f,-.7f,-3.1f,11,realm==4?2:3,6.2f):CubeListBuilder.create(),PartPose.ZERO);
  if(realm==6)for(int i=0;i<8;i++){float a=i*(float)Math.PI/4;head.addOrReplaceChild("solar"+i,CubeListBuilder.create().texOffs(64,16).addBox(-.4f,-.9f,0,.8f,1.8f,.4f),PartPose.offsetAndRotation((float)Math.cos(a)*5,-7+(float)Math.sin(a)*5,4.9f,0,0,a));}
  for(int side:new int[]{-1,1}){var arm=r.getChild(side<0?"right_arm":"left_arm");float start=side<0?-3.5f:-1.5f;float width=school==2?7:school==3?5.3f:5;arm.addOrReplaceChild("cuff",CubeListBuilder.create().texOffs(64,32).addBox(start-(width-5)/2,5.3f,-2.5f,width,school==2?5:3,5.2f),PartPose.ZERO);if(school==3)arm.addOrReplaceChild("pauldron",CubeListBuilder.create().texOffs(64,0).addBox(start-1,-2.4f,-3.2f,7,3,6.4f),PartPose.ZERO);if(school==1&&realm>=2)arm.addOrReplaceChild("knot",CubeListBuilder.create().texOffs(96,16).addBox(start,1,-2.8f,1,4,.8f),PartPose.ZERO);
   var leg=r.addOrReplaceChild(side<0?"right_leg":"left_leg",CubeListBuilder.create(),PartPose.offset(side*1.9f,12,0));leg.addOrReplaceChild("cloth",CubeListBuilder.create().texOffs(0,16).addBox(-2.3f,0,-2.3f,4.6f,12,4.6f),PartPose.ZERO);leg.addOrReplaceChild("panel",CubeListBuilder.create().texOffs(80,68).addBox(-2.4f,0,-2.8f,4.8f,school==3?5:realm==0?6:realm==3?10:8.7f,.5f),PartPose.rotation(-.025f,0,side*.02f));leg.addOrReplaceChild("boot_tip",CubeListBuilder.create().texOffs(0,23).addBox(-2.5f,7.5f,-3.2f,5,4.2f,5.7f),PartPose.ZERO);
  }
  return new HumanoidModel<>(LayerDefinition.create(mesh,128,128).bakeRoot());
 }
}

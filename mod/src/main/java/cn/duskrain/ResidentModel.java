package cn.duskrain;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;

/** Original accessories share the entity's authored atlas; no dependency on player-skin mods. */
public final class ResidentModel extends PlayerModel<Resident> {
    final ModelPart bun,longHair,pendant,book;
    ResidentModel(ModelPart root){super(root,false);bun=head.getChild("bun");longHair=head.getChild("long_hair");pendant=body.getChild("pendant");book=leftArm.getChild("book");}
    public static LayerDefinition layer(){MeshDefinition mesh=PlayerModel.createMesh(CubeDeformation.NONE,false);var root=mesh.getRoot();var head=root.getChild("head");var body=root.getChild("body");
        head.addOrReplaceChild("bun",CubeListBuilder.create().texOffs(64,0).addBox(-2,-11,-.8f,4,3,3.8f),PartPose.ZERO);
        head.addOrReplaceChild("pin",CubeListBuilder.create().texOffs(64,16).addBox(-3.6f,-9.5f,.1f,7.2f,.5f,.5f).addBox(3.5f,-9.8f,-.15f,.9f,1.1f,1),PartPose.ZERO);
        head.addOrReplaceChild("long_hair",CubeListBuilder.create().texOffs(64,0).addBox(-4.15f,-6,1,1,7,3).addBox(3.15f,-6,1,1,7,3).addBox(-3.5f,-5,3,7,8,1.4f),PartPose.ZERO);
        body.addOrReplaceChild("collar_left",CubeListBuilder.create().texOffs(64,24).addBox(-.7f,0,-2.35f,1.4f,8,.45f),PartPose.offsetAndRotation(-2.9f,0,0,0,0,-.68f));
        body.addOrReplaceChild("collar_right",CubeListBuilder.create().texOffs(64,24).addBox(-.55f,0,-2.45f,1.1f,7.5f,.35f),PartPose.offsetAndRotation(2.9f,0,0,0,0,.70f));
        body.addOrReplaceChild("belt",CubeListBuilder.create().texOffs(64,36).addBox(-4.2f,9,-2.2f,8.4f,1.8f,4.4f),PartPose.ZERO);
        body.addOrReplaceChild("pendant",CubeListBuilder.create().texOffs(64,16).addBox(-.6f,0,-.3f,1.2f,2,.6f).texOffs(80,24).addBox(-.2f,1.9f,-.2f,.4f,2,.4f),PartPose.offset(3.1f,10,-2.6f));
        body.addOrReplaceChild("hem",CubeListBuilder.create().texOffs(84,36).addBox(-4.3f,10,-2.25f,8.6f,6,4.5f),PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("cuff",CubeListBuilder.create().texOffs(64,24).addBox(-3.2f,6.5f,-2.2f,4.4f,2,4.4f),PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("cuff",CubeListBuilder.create().texOffs(64,24).addBox(-1.2f,6.5f,-2.2f,4.4f,2,4.4f),PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("book",CubeListBuilder.create().texOffs(96,64).addBox(-1.7f,7,-4.5f,5.4f,3.4f,1.1f),PartPose.ZERO);
        return LayerDefinition.create(mesh,128,128);
    }
    @Override public void setupAnim(Resident e,float limb,float amount,float age,float yaw,float pitch){super.setupAnim(e,limb,amount,age,yaw,pitch);var s=Services.byEntity(e);String id=s==null?"materials":s.id();book.visible=java.util.Set.of("master","quests","enchanter","guild").contains(id);if(book.visible){leftArm.xRot=-.75f;rightArm.xRot=-.7f+(float)Math.sin(age*.035)*.13f;rightArm.zRot=-.15f;}
        longHair.visible=java.util.Set.of("pills","elixirs","quests","treasures","master").contains(id);bun.visible=!id.equals("artifacts");body.zRot=(float)Math.sin(age*.025)*.007f;pendant.zRot=(float)Math.sin(age*.05)*.05f;rightArm.xRot+=(float)Math.sin(age*.03)*.022f;leftArm.xRot-=(float)Math.sin(age*.03)*.022f;}
}

package arena.ai;

import arc.math.geom.Vec2;
import arc.util.Tmp;
import mindustry.entities.units.AIController;

import static mindustry.Vars.controlPath;

public class CrawlerAI extends AIController {

    public int pathID = controlPath.nextTargetId();
    public Vec2 out = new Vec2();

    @Override
    public void updateUnit() {
        if (retarget() || invalid(target))
            target = findMainTarget(unit.x, unit.y, 999999f, unit.type.targetAir, unit.type.targetGround);

        boolean shouldShoot = target != null && unit.within(target, unit.range() * 1.25f);
        if (shouldShoot) unit.aimLook(target);
        unit.controlWeapons(shouldShoot);

        if (target != null) {
            controlPath.getPathPosition(unit, pathID, Tmp.v1.set(target), out);
            moveTo(out, unit.range() * 0.75f, 0f);
        }

        faceTarget();
    }
}
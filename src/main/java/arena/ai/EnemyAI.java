package arena.ai;

import mindustry.entities.Units;
import mindustry.entities.units.AIController;

public class EnemyAI extends AIController {

    @Override
    public void updateUnit() {
        if (retarget() || Units.invalidateTarget(target, unit, targetRange()))
            target = findMainTarget(unit.x, unit.y, targetRange(), unit.type.targetAir, unit.type.targetGround);

        boolean shouldShoot = target != null && unit.within(target, targetRange());
        if (shouldShoot) unit.aimLook(target);
        unit.controlWeapons(shouldShoot);

        var realTarget = findMainTarget(unit.x, unit.y, 999999f, true, true);
        moveTo(realTarget != null ? realTarget : target, unit.range() * 0.5f);

        faceTarget();
    }

    public float targetRange() {
        return unit.range() * (isSuicide() ? 0.75f : 1.25f);
    }

    public boolean isSuicide() {
        return unit.type.weapons.contains(weapon -> weapon.bullet.killShooter);
    }
}
package arena.ai;

import mindustry.entities.Units;
import mindustry.entities.units.AIController;
import mindustry.gen.Teamc;

public class EnemyAI extends AIController {

    public Teamc bestTarget;

    @Override
    public void updateUnit() {
        if (retarget() || invalid(target, unit.range() * 1.25f))
            target = findMainTarget(unit.x, unit.y, unit.range() * 1.25f, unit.type.targetAir, unit.type.targetGround);

        if (timer.get(timerTarget2, 120) || invalid(bestTarget))
            bestTarget = findMainTarget(unit.x, unit.y, Float.MAX_VALUE, true, true);

        boolean shoot = !invalid(target, suicide() ? unit.range() * 0.5f : unit.range());
        if (shoot) unit.aimLook(target);
        unit.controlWeapons(shoot);

        moveTo(bestTarget, suicide() ? 0f : unit.range() * 0.5f);
        faceTarget();
    }

    public boolean suicide() {
        return unit.type.weapons.contains(weapon -> weapon.bullet.killShooter);
    }

    public boolean invalid(Teamc target, float range) {
        return Units.invalidateTarget(target, unit, range);
    }
}
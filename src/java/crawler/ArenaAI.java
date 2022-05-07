package crawler;

import mindustry.ai.types.GroundAI;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.gen.Hitboxc;
import mindustry.gen.Teamc;

import static mindustry.Vars.tilesize;

public class ArenaAI extends GroundAI {

    // TODO улучшить

    @Override
    public void updateUnit() {
        if (Units.invalidateTarget(target, unit.team, unit.x, unit.y, Float.MAX_VALUE)) {
            target = null;
        }

        if (retarget()) {
            target = target(unit.x, unit.y, unit.range(), true, true);
        }

        boolean rotate = false, shoot = false;

        if (!Units.invalidateTarget(target, unit, unit.range()) && unit.hasWeapons()) {
            rotate = true;
            shoot = unit.within(target, unit.type.weapons.first().bullet.range() + (target instanceof Building b ? b.block.size * tilesize / 2f : ((Hitboxc) target).hitSize() / 2f));

            unit.movePref(vec.set(target).sub(unit).limit(unit.speed()));
        }

        unit.controlWeapons(rotate, shoot);
        faceTarget();
    }

    @Override
    public Teamc target(float x, float y, float range, boolean air, boolean ground) {
        return Units.closestTarget(unit.team, x, y, range, u -> u.checkTarget(air, ground), t -> ground);
    }
}

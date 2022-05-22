package crawler;

import mindustry.ai.types.GroundAI;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.gen.Hitboxc;

import static mindustry.Vars.tilesize;

public class ArenaAI extends GroundAI {

    public static final float range = 80000f;

    @Override
    public void updateUnit() {
        if (Units.invalidateTarget(target, unit.team, unit.x, unit.y, Float.MAX_VALUE)) {
            target = null;
        }

        if (retarget()) {
            target = target(unit.x, unit.y, range, true, true);
        }

        boolean rotate, shoot = false;

        if (rotate = !Units.invalidateTarget(target, unit, range)) {
            shoot = unit.within(target, unit.type.weapons.first().bullet.range() + (target instanceof Building b ? b.block.size * tilesize : ((Hitboxc) target).hitSize()) / 2f);
            unit.movePref(vec.set(target).sub(unit).limit(unit.speed()));
        }

        unit.controlWeapons(rotate, shoot);
        faceTarget();
    }
}

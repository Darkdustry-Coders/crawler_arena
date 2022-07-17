package crawler.ai;

import mindustry.ai.types.GroundAI;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.gen.Hitboxc;

import static crawler.CrawlerVars.AIRange;
import static mindustry.Vars.tilesize;

public class CrawlerAI extends GroundAI {

    @Override
    public void updateUnit() {
        if (Units.invalidateTarget(target, unit.team, unit.x, unit.y, Float.MAX_VALUE)) {
            target = null;
        }

        if (retarget()) {
            target = target(unit.x, unit.y, AIRange, true, true);
        }

        boolean rotate = !Units.invalidateTarget(target, unit, AIRange), shoot = false;

        if (rotate) {
            shoot = unit.within(target, unit.type.weapons.first().bullet.range + (target instanceof Building b ? b.block.size * tilesize : ((Hitboxc) target).hitSize()) / 2f);
            unit.movePref(vec.set(target).sub(unit).limit(unit.speed()));
        }

        unit.controlWeapons(rotate, shoot);
        faceTarget();
    }
}

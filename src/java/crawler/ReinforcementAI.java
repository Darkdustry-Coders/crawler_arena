package crawler;

import arc.math.Mathf;
import arc.math.geom.Vec2;
import mindustry.entities.units.AIController;
import mindustry.gen.Call;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

public class ReinforcementAI extends AIController {

    @Override
    public void updateUnit() {
        unit.moveAt(new Vec2().trns(Mathf.atan2(world.width() * 4 - unit.x, world.height() * 4 - unit.y), unit.speed()));

        if (world.width() * tilesize / 2f - unit.x < 120f) {
            Call.payloadDropped(unit, unit.x, unit.y);
        }

        if (unit.x > world.width() * 7) {
            Call.unitDespawn(unit);
        }

        if (unit.moving()) {
            unit.lookAt(unit.vel().angle());
        }
    }
}

package crawler.ai;

import mindustry.entities.units.AIController;
import mindustry.gen.Call;

import static mindustry.Vars.world;

public class ReinforcementAI extends AIController {

    @Override
    public void updateUnit() {
        unit.move(unit.speed(), 0f);
        unit.lookAt(unit.vel().angle());

        if (world.unitWidth() / 2f - unit.x < 120f)
            Call.payloadDropped(unit, unit.x, unit.y);

        if (unit.x >= world.unitWidth())
            Call.unitDespawn(unit);
    }
}
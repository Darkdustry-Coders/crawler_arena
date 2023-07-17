package arena.ai;

import mindustry.entities.units.AIController;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class ReinforcementAI extends AIController {

    @Override
    public void updateUnit() {
        if (!(unit instanceof Payloadc payloadc)) return;

        unit.move(unit.speed(), 0f);
        unit.lookAt(unit.vel().angle());

        if (unit.x > world.unitWidth() / 2f - 144f && payloadc.dropLastPayload())
            Call.payloadDropped(unit, unit.x, unit.y);

        if (payloadc.payloads().isEmpty())
            Call.unitDespawn(unit);
    }
}
package arena.ai;

import mindustry.entities.units.AIController;
import mindustry.gen.Call;
import mindustry.gen.Payloadc;

import static mindustry.Vars.*;

public class ReinforcementAI extends AIController {

    public final int timerPayload = 2;
    public final float timePayload = 60f;

    @Override
    public void updateUnit() {
        unit.move(unit.speed(), 0f);
        unit.lookAt(unit.vel().angle());

        if (timer.get(timerPayload, timePayload) && world.width() * tilesize / 2f < unit.x + 144f)
            Call.payloadDropped(unit, unit.x, unit.y);

        if (unit instanceof Payloadc payloadc && payloadc.payloads().isEmpty())
            Call.unitDespawn(unit);
    }
}
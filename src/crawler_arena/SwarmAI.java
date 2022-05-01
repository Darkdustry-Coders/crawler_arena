package crawler_arena;

import mindustry.ai.types.FlyingAI;
import mindustry.entities.Units;
import mindustry.entities.units.UnitCommand;
import mindustry.gen.Unit;
import mindustry.world.meta.BlockFlag;

public class SwarmAI extends FlyingAI {

    public float swarmRange = 320f;
    public float innerSwarmRange = 120f;
    public float avoidRange = 480f;
    public float kiteRange = 0.8f;
    public int swarmCount = 10;

    // TODO слишком имбовое, понерфить

    @Override
    public void updateMovement() {
        if (target != null && unit.hasWeapons() && command() == UnitCommand.attack) {
            if (!unit.type.circleTarget) {
                if (Units.count(unit.x, unit.y, swarmRange, u -> u.type == unit.type && u.team == unit.team) > swarmCount) {
                    moveTo(target, unit.type.range * kiteRange);
                } else {
                    if (unit.dst(target) > avoidRange) {
                        Unit targetSameType = Units.closest(unit.team, unit.x, unit.y, u -> u.type == unit.type && unit.dst(u) > innerSwarmRange);
                        moveTo(targetSameType != null ? targetSameType : target, unit.hitSize * 2f);
                    } else {
                        moveTo(target, avoidRange * 1.1f);
                    }
                }
                unit.lookAt(target);
            } else {
                attack(120f);
            }
        }

        if (command() == UnitCommand.rally) {
            moveTo(targetFlag(unit.x, unit.y, BlockFlag.rally, false), 60f);
        }
    }
}

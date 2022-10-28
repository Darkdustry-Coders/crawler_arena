package crawler.ai;

import mindustry.entities.units.AIController;

import static mindustry.ai.Pathfinder.fieldCore;

public class CrawlerAI extends AIController {

    @Override
    public void updateUnit() {
        if (retarget() || invalid(target))
            target = findMainTarget(unit.x, unit.y, unit.range() * 2f, unit.type.targetAir, unit.type.targetGround);

        if (target != null && unit.within(target, unit.range() * 1.25f))
            for (var mount : unit.mounts)
                mount.target = target;

        pathfind(fieldCore);
        faceTarget();
    }
}
package crawler.ai;

import crawler.CrawlerLogic;
import mindustry.entities.units.AIController;

import static mindustry.Vars.tilesize;
import static mindustry.ai.Pathfinder.fieldCore;

public class CrawlerAI extends AIController {

    @Override
    public void updateUnit() {
        if (retarget() || invalid(target))
            target = findMainTarget(unit.x, unit.y, unit.range() * 2f, unit.type.targetAir, unit.type.targetGround);

        boolean shouldShoot = target != null && unit.within(target, unit.range() * 1.25f);
        if (shouldShoot) unit.aimLook(target);
        unit.controlWeapons(shouldShoot);

        if (unit.dst(CrawlerLogic.worldCenter()) < 36 * tilesize)
            moveTo(target, unit.range() / 1.25f);
        else pathfind(fieldCore);

        faceTarget();
    }
}
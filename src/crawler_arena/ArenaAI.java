package crawler_arena;

import arc.math.geom.Geometry;
import arc.math.geom.Point2;
import mindustry.ai.types.GroundAI;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.gen.Hitboxc;
import mindustry.gen.Teamc;
import mindustry.world.Tile;
import mindustry.world.blocks.distribution.Conveyor;
import mindustry.world.blocks.liquid.Conduit;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.meta.BlockGroup;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

public class ArenaAI extends GroundAI {

    public boolean blockedByBlocks;

    @Override
    public void updateUnit() {
        if (Units.invalidateTarget(target, unit.team, unit.x, unit.y, Float.MAX_VALUE)) {
            target = null;
        }

        if (retarget()) {
            target = target(unit.x, unit.y, unit.range(), unit.type.targetAir, unit.type.targetGround);
        }

        boolean rotate = false, shoot = false;

        if (!Units.invalidateTarget(target, unit, unit.range()) && unit.hasWeapons()) {
            rotate = true;
            shoot = unit.within(target, unit.type.weapons.first().bullet.range() + (target instanceof Building b ? b.block.size * tilesize / 2f : ((Hitboxc) target).hitSize() / 2f));

            if (!(target instanceof Building build && !(build.block instanceof CoreBlock) && (build.block.group == BlockGroup.walls || build.block.group == BlockGroup.liquids || build.block.group == BlockGroup.transportation))) {
                blockedByBlocks = false;

                boolean blocked = world.raycast(unit.tileX(), unit.tileY(), target.tileX(), target.tileY(), (x, y) -> {
                    for (Point2 p : Geometry.d4c) {
                        Tile tile = world.tile(x + p.x, y + p.y);
                        if (tile != null && tile.build == target) return false;
                        if (tile != null && tile.build != null && tile.build.team != unit.team()) {
                            blockedByBlocks = true;
                            return true;
                        } else {
                            return tile == null || tile.solid();
                        }
                    }
                    return false;
                });

                if (blockedByBlocks) {
                    shoot = true;
                }

                if (!blocked) {
                    unit.movePref(vec.set(target).sub(unit).limit(unit.speed()));
                }
            }
        }

        unit.controlWeapons(rotate, shoot);
        faceTarget();
    }

    @Override
    public Teamc target(float x, float y, float range, boolean air, boolean ground) {
        return Units.closestTarget(unit.team, x, y, range, u -> u.checkTarget(air, ground), t -> ground && !(t.block instanceof Conveyor || t.block instanceof Conduit));
    }
}

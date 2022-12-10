package arena.ai;

import mindustry.ai.types.FlyingAI;
import mindustry.gen.Teamc;

public class BossAI extends FlyingAI {

    @Override
    public Teamc findTarget(float x, float y, float range, boolean air, boolean ground) {
        return super.findTarget(x, y, Float.POSITIVE_INFINITY, air, ground);
    }

    @Override
    public Teamc findMainTarget(float x, float y, float range, boolean air, boolean ground) {
        return super.findMainTarget(x, y, Float.POSITIVE_INFINITY, air, ground);
    }
}
package arena.ai;

import mindustry.ai.types.FlyingAI;
import mindustry.gen.Teamc;

public class BossAI extends FlyingAI {

    @Override
    public Teamc findTarget(float x, float y, float range, boolean air, boolean ground) {
        return super.target(x, y, Float.MAX_VALUE, air, ground);
    }

    @Override
    public Teamc findMainTarget(float x, float y, float range, boolean air, boolean ground) {
        return super.target(x, y, Float.MAX_VALUE, air, ground);
    }
}
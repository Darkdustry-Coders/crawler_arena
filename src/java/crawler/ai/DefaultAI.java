package crawler.ai;

import mindustry.ai.types.FlyingAI;
import mindustry.gen.Teamc;

import static crawler.CrawlerVars.AIRange;

public class DefaultAI extends FlyingAI {

    @Override
    public Teamc findTarget(float x, float y, float range, boolean air, boolean ground) {
        return super.findTarget(x, y, AIRange, air, ground);
    }

    @Override
    public Teamc findMainTarget(float x, float y, float range, boolean air, boolean ground) {
        return super.findMainTarget(x, y, AIRange, air, ground);
    }
}

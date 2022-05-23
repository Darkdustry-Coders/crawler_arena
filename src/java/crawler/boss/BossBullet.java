package crawler.boss;

import arc.math.geom.Vec2;

public class BossBullet extends Vec2 {

    public int lifetime;

    public BossBullet(float x, float y, int lifetime) {
        super(x, y);
        
        this.lifetime = lifetime;
        BossBullets.bullets.add(this);
    }

    public void update() {
        lifetime--;
    }

    public boolean alive() {
        return lifetime != 0;
    }
}

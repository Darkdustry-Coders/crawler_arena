package arena.boss;

import arc.math.geom.Vec2;

import static arena.boss.BossBullets.*;

public class BossBullet extends Vec2 {
    public int lifetime;

    public BossBullet(float x, float y, int lifetime) {
        super(x, y);
        this.lifetime = lifetime;

        bullets.add(this);
    }

    public void update() {
        lifetime--;
    }

    public boolean alive() {
        return lifetime > 0;
    }
}
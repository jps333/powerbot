package iDzn.OgreCannon.Tasks;

import iDzn.OgreCannon.Task;
import org.powerbot.bot.rt4.client.Client;
import org.powerbot.script.Area;
import org.powerbot.script.Condition;
import org.powerbot.script.Tile;
import org.powerbot.script.rt4.*;

import java.util.concurrent.Callable;


public class leaveRails  extends Task<org.powerbot.script.ClientContext<Client>> {
    private final Area lootArea = new Area(new Tile(2523, 3377, 0), new Tile(2533,3373, 0));
    private final int[] fenceBounds = {118, 177, -134, 0, -32, 100};
    GameObject cannonToFire = ctx.objects.select().id(6).poll();
    public leaveRails(ClientContext ctx) {
        super(ctx);
    }


    @Override
    public boolean activate() {
        return lootArea.contains (ctx.players.local()) && ctx.groundItems.select().id(5300, 5304, 5295).isEmpty();

    }

    @Override
    public void execute() {
        GameObject rail = ctx.objects.select().id(19171).poll();
        rail.bounds(fenceBounds);

        if (rail.valid())
            if (!rail.inViewport()) {
                ctx.camera.turnTo(rail);
                ctx.camera.pitch(67);
            }
        rail.interact("Squeeze-through", "Loose Railing");
        Condition.wait(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return !lootArea.contains(ctx.players.local());
            }
        }, 500, 30);
        ctx.movement.step(cannonToFire);
        cannonToFire.interact("Fire");
    }
}
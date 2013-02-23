package ardust.client;

import ardust.entities.Entity;
import ardust.packets.DwarfRequestPacket;
import ardust.shared.*;

import java.awt.*;

public class Character {

    double modeProgress;
    AnimatedSprite sprite = new AnimatedSprite();

    public Point3 location = new Point3();
    public Point3 targetLocation = new Point3();

    private final Entity entity;
    Entity.Mode prevMode = Entity.Mode.IDLE;

    public boolean isHalting;

    public Character(Entity entity) {
        this.entity = entity;
    }

    public void animateWalk() {
        modeProgress = 1d - (double) entity.countdown / (double) Constants.WALKING_COUNTDOWN;
        switch (entity.orientation) {
            case NORTH:
                sprite.animate(28, 4, Constants.DWARF_ANIMATION_SPEED);
                break;
            default:
                sprite.animate(24, 4, Constants.DWARF_ANIMATION_SPEED);
                break;
        }
    }

    public void animateMining() {
        switch (entity.orientation) {
            case NORTH:
                sprite.animate(36, 4, Constants.DWARF_ANIMATION_SPEED / 2);
                break;
            default:
                sprite.animate(32, 4, Constants.DWARF_ANIMATION_SPEED / 2);
                break;
        }
    }

    public void halt()
    {
        if (entity.mode == Entity.Mode.WALKING)
        {
            isHalting = true;
        }
    }

    public void showStationarySprite() {
        switch (entity.orientation) {
            case NORTH:
                sprite.currentFrame = 29;
                break;
            default:
                sprite.currentFrame = 25;
                break;
        }
    }

    public void tick(int deltaT, ClientWorld world, NetworkConnection network) {
        entity.countdown -= deltaT;

        if (entity.countdown < 0)
            entity.countdown = 0;

        boolean setCountdown = (prevMode != entity.mode) || (!location.equals(entity.position));
        prevMode = entity.mode;

        // detect if just started moving
        location.set(entity.position);
        targetLocation.set(location);
        switch (entity.mode) {
            case WALKING:
                if (setCountdown)
                    entity.countdown = Constants.WALKING_COUNTDOWN;
                animateWalk();
                switch (entity.orientation) {
                    case NORTH:
                        targetLocation.y -= 1;
                        break;
                    case EAST:
                        targetLocation.x += 1;
                        break;
                    case SOUTH:
                        targetLocation.y += 1;
                        break;
                    case WEST:
                        targetLocation.x -= 1;
                        break;
                }
                break;
            case MINING:
                animateMining();
                break;
            default:
                showStationarySprite();
        }

        if (modeProgress >= 1 && entity.mode == Entity.Mode.WALKING)
        {
            if (!isHalting)
            {
                network.send(new DwarfRequestPacket(id(), DwarfRequest.Walk, entity.orientation));
            }  else {
                isHalting = false;
                entity.mode = Entity.Mode.IDLE;
            }
        }
    }

    public Point getLocalDrawPoint(Point viewportLocation) {
        Point localPoint = new Point(0, 0);
        World.globalTileToLocalCoord(location.x, location.y, location.z, viewportLocation, localPoint);

        if (entity.mode == Entity.Mode.WALKING) {
            switch (entity.orientation) {
                case NORTH:
                    localPoint.y -= (int) (Constants.TILE_BASE_HEIGHT * modeProgress);
                    break;
                case EAST:
                    localPoint.x += (int) (Constants.TILE_BASE_WIDTH * modeProgress);
                    break;
                case WEST:
                    localPoint.x -= (int) (Constants.TILE_BASE_WIDTH * modeProgress);
                    break;
                case SOUTH:
                    localPoint.y += (int) (Constants.TILE_BASE_HEIGHT * modeProgress);
                    break;
            }
        }
        return localPoint;
    }

    public void draw(Painter p, Point viewportLocation, boolean selectedDwarf) {
        Point localPoint = getLocalDrawPoint(viewportLocation);
        boolean flipAnimation = (entity.orientation == Orientation.EAST);
        //if (selectedDwarf)
       //    p.draw(localPoint.x, localPoint.y - (Constants.TILE_DRAW_HEIGHT - Constants.TILE_BASE_HEIGHT), 96, 40, 43, 10, false);//sorry
        sprite.draw(p, localPoint.x, localPoint.y - Constants.TILE_BASE_HEIGHT - Constants.DWARF_OFFSET_ON_TILE, flipAnimation);
    }


    public Integer id() {
        return entity.id;
    }
}

package ardust.client;

import ardust.entities.Entity;
import ardust.packets.DwarfRequestPacket;
import ardust.shared.*;

import java.awt.*;
import java.util.Random;


public class Character {

    double modeProgress;
    AnimatedSprite sprite = new AnimatedSprite();

    public Point2 location = new Point2();
    public Point2 targetLocation = new Point2();

    private final Entity entity;
    Entity.Mode prevMode = Entity.Mode.IDLE;

    CharacterAIMode aiMode = CharacterAIMode.IDLE;
    Point2 pathingTarget = new Point2();

    Random random = new Random();
    private int pathingFailStrike;
    private int miningFailStrike;

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
        int currentFrame = sprite.currentFrame;
        switch (entity.orientation) {
            case NORTH:
                sprite.animate(36, 4, Constants.DWARF_ANIMATION_SPEED / 2);
                break;
            default:
                sprite.animate(32, 4, Constants.DWARF_ANIMATION_SPEED / 2);
                break;
        }
        if (sprite.currentFrame != currentFrame && sprite.currentFrame % 8 % 3 == 0) {
            GameLoop.soundBank.playSound(SoundBank.pickaxeSound, true);
        }
    }

    public void animateFighting() {
        int currentFrame = sprite.currentFrame;
        switch (entity.orientation) {
            case NORTH:
                sprite.animate(68, 4, Constants.DWARF_ANIMATION_SPEED / 2);
                break;
            default:
                sprite.animate(64, 4, Constants.DWARF_ANIMATION_SPEED / 2);
                break;
        }
        if (sprite.currentFrame != currentFrame && sprite.currentFrame % 8 % 3 == 0) {
            GameLoop.soundBank.playSound(SoundBank.fightSound, true);
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

    public void tick(int deltaT, World world, NetworkConnection network, GameCore core) {
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
                targetLocation.move(entity.orientation);
                break;
            case MINING:
                animateMining();
                break;
            case ATTACK:
                animateFighting();
                break;
            default:
                showStationarySprite();
        }

        switch (aiMode) {
            case WALK:
                if (!pathTowards(world, network, true))
                    aiMode = CharacterAIMode.IDLE;
                break;
            case MINE:
                if (!pathTowards(world, network, false)) {
                    if (location.equals(pathingTarget) || (targetLocation.equals(pathingTarget) && entity.mode == Entity.Mode.WALKING)) {
                        aiMode = CharacterAIMode.IDLE;
                    } else {
                        Orientation orientation = orientationToward(world, false);
                        tempPoint.set(location);
                        tempPoint.move(orientation);
                        if (world.isTileMineable(tempPoint)) {
                            clearFailStrikes();
                            network.send(new DwarfRequestPacket(entity.id, DwarfRequest.Mine, orientation));
                        } else {
                            if (world.isTileOccupied(tempPoint.x, tempPoint.y, entity)) { // don't complain about walkables, dunno why the pathing is failing on it however
                                miningFailStrike -= 1;
                                if (miningFailStrike <= 0) {
                                    aiMode = CharacterAIMode.IDLE;
                                }
                            } else
                                clearFailStrikes();
                        }
                    }
                }
                break;
            case USE:
                if (!pathTowards(world, network, true)) {

                    // if reached, use
                    aiMode = CharacterAIMode.IDLE;
                }
                break;
            case IDLE:
                break;
        }
    }

    Point2 tempPoint = new Point2();

    private Orientation orientationToward(World world, boolean goAround) {
        Orientation ew;
        if (pathingTarget.x == targetLocation.x) {
            if (goAround)
                ew = random.nextBoolean() ? Orientation.EAST : Orientation.WEST;
            else
                ew = Orientation.NONE;
        } else
            ew = (pathingTarget.x > targetLocation.x) ? Orientation.EAST : Orientation.WEST;

        Orientation ns;
        if (pathingTarget.y == targetLocation.y) {
            if (goAround)
                ns = random.nextBoolean() ? Orientation.NORTH : Orientation.SOUTH;
            else
                ns = Orientation.NONE;
        } else
            ns = (pathingTarget.y > targetLocation.y) ? Orientation.SOUTH : Orientation.NORTH;

        double eww = Math.abs(pathingTarget.x - targetLocation.x);
        double nsw = Math.abs(pathingTarget.y - targetLocation.y);

        Orientation orientation;
        Orientation origOrientation;
        Orientation otherOrientation;
        double w = eww + nsw;
        if (w == 0)
            w += 1;
        eww /= w;
        if (random.nextFloat() < eww) {
            orientation = ew;
            otherOrientation = ns;
        } else {
            orientation = ns;
            otherOrientation = ew;
        }
        origOrientation = orientation;
        if (orientation == Orientation.NONE)
            return otherOrientation;

        if (!goAround)
            return orientation;

        tempPoint.set(targetLocation);
        tempPoint.move(orientation);
        if (world.isTileOccupied(tempPoint, entity))
            orientation = otherOrientation;

        tempPoint.set(targetLocation);
        tempPoint.move(orientation);
        if (world.isTileOccupied(tempPoint, entity))
            return origOrientation;
        return orientation;
    }

    private boolean pathTowards(World world, NetworkConnection network, boolean goAround) {
        if (targetLocation.equals(pathingTarget))
            return false;

        Orientation orientation = orientationToward(world, goAround);
        if (orientation == Orientation.NONE)
            return false;

        tempPoint.set(targetLocation);
        tempPoint.move(orientation);
        if (world.isTileOccupied(tempPoint, entity)) {
            pathingFailStrike -= 1;
            if (pathingFailStrike <= 0) {
                return false;
            }
            return true;
        }

        if (location.equals(tempPoint)) {
            pathingFailStrike -= 1;
            if (pathingFailStrike <= 0)
                return false;
        } else
            clearFailStrikes();

        network.send(new DwarfRequestPacket(entity.id, DwarfRequest.Walk, orientation));
        return true;
    }

    public void getLocalDrawPoint(Point viewportLocation, Point result) {
        World.globalTileToLocalCoord(location.x, location.y, viewportLocation, result);

        if (entity.mode == Entity.Mode.WALKING) {
            switch (entity.orientation) {
                case NORTH:
                    result.y -= (int) (Constants.TILE_BASE_HEIGHT * modeProgress);
                    break;
                case EAST:
                    result.x += (int) (Constants.TILE_BASE_WIDTH * modeProgress);
                    break;
                case WEST:
                    result.x -= (int) (Constants.TILE_BASE_WIDTH * modeProgress);
                    break;
                case SOUTH:
                    result.y += (int) (Constants.TILE_BASE_HEIGHT * modeProgress);
                    break;
                default:
                    break;
            }
        }
    }

    Point localPoint = new Point();

    public void draw(Painter p, Point viewportLocation, boolean selectedDwarf) {
        getLocalDrawPoint(viewportLocation, localPoint);
        boolean flipAnimation = (entity.orientation == Orientation.EAST);
        if (selectedDwarf)
            p.draw(localPoint.x + Constants.DWARF_HEART_CENTER_OFFSET, localPoint.y - (Constants.TILE_DRAW_HEIGHT - Constants.TILE_BASE_HEIGHT), 96, 40, 9, 9, false);
        sprite.draw(p, localPoint.x, localPoint.y - Constants.TILE_BASE_HEIGHT - Constants.DWARF_OFFSET_ON_TILE, flipAnimation);
    }

    public Integer id() {
        return entity.id;
    }

    public void halt() {
        aiMode = CharacterAIMode.IDLE;
    }

    public void walkTo(Point2 target) {
        aiMode = CharacterAIMode.WALK;
        pathingTarget.set(target);
        clearFailStrikes();
    }

    public void mineTo(Point2 target) {
        aiMode = CharacterAIMode.MINE;
        pathingTarget.set(target);
        clearFailStrikes();
    }

    public void use(Point2 target) {
        aiMode = CharacterAIMode.USE;
        pathingTarget.set(target);
        clearFailStrikes();
    }

    private void clearFailStrikes() {
        pathingFailStrike = Constants.WALK_LOOP_LIMIT;
        miningFailStrike = Constants.MINE_FAIL_LIMIT;
    }

    public int playerId() {
        return entity.playerId.intValue();
    }
}

package ardust.client;

import ardust.entities.Entities;
import ardust.entities.Entity;
import ardust.shared.Constants;
import ardust.shared.NetworkConnection;
import ardust.shared.Point3;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class World {
    public static final int tilesBeyondViewportToRender = 3;

    ClientWorld clientWorld;
    Entities entities;
    Characters characters;


    public World() {
        clientWorld = new ClientWorld();
        entities = new Entities();
        characters = new Characters(entities);
    }

    public static void globalTileToLocalCoord(int tileX, int tileY, int tileZ, Point viewportLocation, Point result) {
        result.setLocation(tileX * Constants.TILE_BASE_WIDTH - viewportLocation.x, tileY * Constants.TILE_BASE_HEIGHT - viewportLocation.y);
    }

    public void tick(int deltaT, NetworkConnection network, GameCore core) {
        characters.tick(deltaT, this, network, core);
    }

    Point toDrawCoord = new Point();
    Rectangle tileSheetFloorRect = new Rectangle();
    Rectangle tileSheetRect = new Rectangle();
    Point3 tilePoint = new Point3();

    public void draw(Painter p, Point viewportLocation, int zLayer, int screenWidth, int screenHeight, Character selectedDwarf, int cursorX, int cursorY, int cursorZ) {

        int tileRectX = viewportLocation.x / Constants.TILE_BASE_WIDTH - tilesBeyondViewportToRender;
        int tileRectY = viewportLocation.y / Constants.TILE_BASE_HEIGHT - tilesBeyondViewportToRender;
        int tileRectWidth = screenWidth / Constants.TILE_BASE_WIDTH + 2 * tilesBeyondViewportToRender;
        int tileRectHeight = screenHeight / Constants.TILE_BASE_HEIGHT + 2 * tilesBeyondViewportToRender;

        HashMap<Point3, Character> charactersByPosition = characters.charactersByPosition();

        double t = (System.currentTimeMillis() / 1000d * 2d * 3.14d) / 5;
        float r = (float) Math.abs(Math.sin(t));
        float g = (float) Math.abs(Math.sin(t + 2));
        float b = (float) Math.abs(Math.sin(t + 4));
        float a = 0.5f + 0.5f * (float) Math.abs(Math.sin(t * 3));

        p.start();
        int z = zLayer;
        for (int y = tileRectY; y < tileRectY + tileRectHeight; y++) {
            for (int x = tileRectX; x < tileRectX + tileRectWidth; x++) {

                globalTileToLocalCoord(x, y, z, viewportLocation, toDrawCoord);

                //Draw Floor
                p.getSourceRectFromTileSheetIndex(0, tileSheetFloorRect);
                p.draw(toDrawCoord.x, toDrawCoord.y - (Constants.TILE_DRAW_HEIGHT - Constants.TILE_BASE_HEIGHT) + Constants.FLOOR_TILE_THICKNESS,
                        tileSheetFloorRect.x, tileSheetFloorRect.y, tileSheetFloorRect.width, tileSheetFloorRect.height, false);

                //Draw Terrain Item
                byte whatItem = clientWorld.read(x, y, z);
                int baseBlock = Constants.convertIndexToBaseBlockIndex(whatItem);
                int blockMod = Constants.getBlockModIndex(whatItem);
                if (whatItem != 0) {
                    if ((x == cursorX) && (y == cursorY) && (z == cursorZ))
                        GL11.glColor4f(r, g, b, 1);
                    p.getSourceRectFromTileSheetIndex(baseBlock, tileSheetRect);
                    p.draw(toDrawCoord.x, toDrawCoord.y - (Constants.TILE_DRAW_HEIGHT - Constants.TILE_BASE_HEIGHT),
                            tileSheetRect.x, tileSheetRect.y, tileSheetRect.width, tileSheetRect.height, false);
                    if ((x == cursorX) && (y == cursorY) && (z == cursorZ))
                        GL11.glColor4f(1, 1, 1, 1);

                    if (blockMod != -1) {
                        p.getSourceRectFromTileSheetIndex(blockMod, tileSheetRect);
                        p.draw(toDrawCoord.x, toDrawCoord.y - (Constants.TILE_DRAW_HEIGHT - Constants.TILE_BASE_HEIGHT),
                                tileSheetRect.x, tileSheetRect.y, tileSheetRect.width, tileSheetRect.height, false);
                    }
                }
                //Draw Character
                tilePoint.set(x, y, z);
                Character character = charactersByPosition.get(tilePoint);
                if (character != null) {
                    character.draw(p, viewportLocation, character.equals(selectedDwarf));
                }
            }
        }

        //cursor
        GL11.glColor4f(r, g, b, a);
        globalTileToLocalCoord(cursorX, cursorY, cursorZ, viewportLocation, toDrawCoord);

        p.getSourceRectFromTileSheetIndex(Constants.CURSOR_TILE_NORMAL, tileSheetRect);
        p.draw(toDrawCoord.x, toDrawCoord.y - (Constants.TILE_DRAW_HEIGHT - Constants.TILE_BASE_HEIGHT),
                tileSheetRect.x, tileSheetRect.y, tileSheetRect.width, tileSheetRect.height, false);

        p.flush();
    }

    public static void screenCoordToWorldCoord(Point viewportLocation, Point result) {
        int x = viewportLocation.x / Constants.TILE_BASE_WIDTH;
        int y = viewportLocation.y / Constants.TILE_BASE_HEIGHT;
        result.setLocation(x, y);
    }

    public static void worldCoordToScreenCoord(Point worldLocation, Point result) {
        int x = worldLocation.x * Constants.TILE_BASE_WIDTH;
        int y = worldLocation.y * Constants.TILE_BASE_HEIGHT;
        result.setLocation(x, y);
    }

    public static void localCoordToGlobalTile(int x, int y, Point viewportLocation, Point result) {
        result.setLocation((viewportLocation.x + x / Constants.PIXEL_SCALE) / Constants.TILE_BASE_WIDTH, (viewportLocation.y + y / Constants.PIXEL_SCALE + Constants.MOUSE_TO_TILE_YSHIFT) / Constants.TILE_BASE_HEIGHT);
    }

    public void writeTiles(int[] locations, byte[] tiles) {
        clientWorld.writeTiles(locations, tiles);
    }

    public boolean isTileOccupied(int x, int y, int z, Entity entity) {
        if (!Constants.isWalkable(clientWorld.read(x, y, z)))
            return true;
        Character at = getCharacterAtTile(x, y, z);
        if ((at != null) && (at.id().equals(entity.id)))
            return true;
        return false;
    }

    public Character getCharacterAtTile(int x, int y, int z) {
        return characters.getCharacterAtTile(x, y, z);
    }

    public void updateEntities(ByteBuffer data, boolean checkpoint) {
        entities.read(data, checkpoint);
    }

    public Character nextCharacter(Character selectedDwarf) {
        return characters.nextCharacter(selectedDwarf);
    }

    public boolean isTileOccupied(Point3 point, Entity entity) {
        return isTileOccupied(point.x, point.y, point.z, entity);
    }

    public boolean isTileMineable(Point3 point) {
        return Constants.isMinable(clientWorld.read(point.x, point.y, point.z));
    }
}

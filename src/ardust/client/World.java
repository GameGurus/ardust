package ardust.client;

import ardust.shared.Constants;
import com.sun.org.apache.bcel.internal.classfile.StackMapEntry;

import java.awt.*;
import java.util.*;

/* I chose to store the "terrainItems" (any stationary object that exists in the world... like stones, structures, etc.)  in a byte array to save on memory (the bytes would presumably
correspond to an index in the tile sheet).
This might be a bad choice, but I figured there wouldn't be too many possible types in a game of this size, and they wouldn't really need to store any data about themselves.
I'm also not sure how complex the interactions will be with these structures, but I figured that we might able to do something like Minecraft,
so each dwarf can really only affect one block at a time (if you are mining a block and then switch to another block
your "damage" to the initial block just goes away). So we could just have each dwarf keep track of the terrainItem he is currently
working with, and pull info (health, effect on adjacent dwarves) from a couple of switch statements somewhere.
I dunno.. it's something to discuss.


*/
public class World {
    public static final int tilesBeyondViewportToRender = 1;

    ClientWorld clientWorld;
    ArrayList<Character> characters = new ArrayList<Character>();

    public World() {
        clientWorld = new ClientWorld();
        characters.add(new Character(4, 4, Constants.DUMMY_Z, Constants.DWARF_DEFAULT_SPEED));
        characters.add(new Character(5, 4, Constants.DUMMY_Z, Constants.DWARF_DEFAULT_SPEED));
        characters.add(new Character(3, 3, Constants.DUMMY_Z, Constants.DWARF_DEFAULT_SPEED));
        characters.add(new Character(4, 6, Constants.DUMMY_Z, Constants.DWARF_DEFAULT_SPEED));
    }

    public static void globalTileToLocalCoord(int tileX, int tileY, Point viewportLocation, Point result) {
        result.setLocation(tileX * Constants.TILE_BASE_WIDTH - viewportLocation.x, tileY * Constants.TILE_BASE_HEIGHT - viewportLocation.y);
    }

    public void tick()
    {
        for (Character c : characters) c.tick(this);
    }

    public void draw(Painter p, Point viewportLocation, int screenWidth, int screenHeight, Character selectedDwarf) {

        int tileRectX = viewportLocation.x / Constants.TILE_BASE_WIDTH - tilesBeyondViewportToRender;
        int tileRectY = viewportLocation.y / Constants.TILE_BASE_HEIGHT - tilesBeyondViewportToRender;
        int tileRectWidth = screenWidth / Constants.TILE_BASE_WIDTH + 2 * tilesBeyondViewportToRender;
        int tileRectHeight = screenHeight / Constants.TILE_BASE_HEIGHT + 2 * tilesBeyondViewportToRender;

        HashMap<Point3, Character> charactersByPosition = new HashMap<Point3, Character>();
        for (Character c : characters)
        {
            charactersByPosition.put(c.getLocation(), c);
        }


        Point toDrawCoord = new Point();
        Rectangle tileSheetFloorRect = new Rectangle();
        Rectangle tileSheetRect = new Rectangle();
        p.start();
        int z = Constants.DUMMY_Z;
        for (int x = tileRectX; x < tileRectX + tileRectWidth; x++) {
            for (int y = tileRectY; y < tileRectY + tileRectHeight; y++) {

                globalTileToLocalCoord(x, y, viewportLocation, toDrawCoord);

                //Draw Floor
                p.getSourceRectFromTileSheetIndex(0, tileSheetFloorRect);
                p.draw(toDrawCoord.x, toDrawCoord.y - (Constants.TILE_DRAW_HEIGHT - Constants.TILE_BASE_HEIGHT) + Constants.FLOOR_TILE_THICKNESS,
                        tileSheetFloorRect.x, tileSheetFloorRect.y, tileSheetFloorRect.width, tileSheetFloorRect.height, false);

                //Draw Character
                Point3 tile = new Point3(x - 1, y, z);
                if (charactersByPosition.containsKey(tile)){
                    charactersByPosition.get(tile).draw(p, viewportLocation, charactersByPosition.get(tile).equals(selectedDwarf));
                }

                //Draw Terrain Item
                byte whatItem = clientWorld.readDirect(x, y, z);
                if (whatItem != 0) {
                    p.getSourceRectFromTileSheetIndex(whatItem, tileSheetRect);
                    p.draw(toDrawCoord.x, toDrawCoord.y - (Constants.TILE_DRAW_HEIGHT - Constants.TILE_BASE_HEIGHT),
                            tileSheetRect.x, tileSheetRect.y, tileSheetRect.width, tileSheetRect.height, false);
                }
            }
        }
        p.flush();
    }

    public static void screenCoordToWorldCoord(Point viewportLocation, Point result) {
        int x = viewportLocation.x / Constants.TILE_BASE_WIDTH;
        int y = viewportLocation.y / Constants.TILE_BASE_HEIGHT;
        result.setLocation(x, y);
    }

    public static void localCoordToGlobalTile(int x, int y, Point viewportLocation, Point result)
    {
        result.setLocation((viewportLocation.x + x/ Constants.PIXEL_SCALE) / Constants.TILE_BASE_WIDTH, (viewportLocation.y + y/ Constants.PIXEL_SCALE) / Constants.TILE_BASE_HEIGHT);
    }

    public void writeTiles(int[] locations, byte[] tiles) {
        clientWorld.writeTiles(locations, tiles);
    }

    public boolean isTileOccupied(int x, int y, int z)
    {
       return (clientWorld.readDirect(x,y,z) != 0);
    }

    public Character getCharacterAtTile(int x, int y, int z)
    {
        Point3 p = new Point3(x, y, z);
        for (Character c : characters)
        {
           if (c.location.equals(p)) return c;
        }
        return null;
    }
}

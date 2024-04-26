import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import com.google.gson.JsonObject;

public class WaypointProfile {
    String name;
    double x, y, z;
    int zoomNeeded;
    CustomColor color;
    WaypointType type;
    WaypointType group = null;

    boolean showBeaconBeam;

    public WaypointProfile(String name, double x, double y, double z, CustomColor color, WaypointType type, int zoomNeeded, boolean showBeaconBeam) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
        this.type = type;
        this.zoomNeeded = zoomNeeded;
        this.showBeaconBeam = showBeaconBeam;
    }

    public int getZoomNeeded() {
        return zoomNeeded;
    }

    public static final byte currentFormat = 2;

    public void decode(byte format, ByteBuffer buf) throws IllegalArgumentException, BufferUnderflowException {
        assert 0 <= format && format <= currentFormat;

        int nameSize = 0;
        switch (format) {
            case 0: nameSize = buf.getInt(); break;
            case 1:
            case 2: nameSize = EncodingUtils.decodeInt(buf); break;
        }
        if (nameSize < 0) {
            throw new IllegalArgumentException(String.format("Invalid waypoint (format %d)\nName size is negative", format));
        }
        if (nameSize > 1024) {
            throw new IllegalArgumentException(String.format("Invalid waypoint (format %d)\nName size is too large", format));
        }
        byte[] name = new byte[nameSize];
        buf.get(name);
        this.name = new String(name, StandardCharsets.UTF_8);

        float r = -1; float g = -1; float b = -1; float a = -1;

        switch (format) {
            case 0:
                this.x = buf.getDouble(); this.y = buf.getDouble(); this.z = buf.getDouble();
                this.zoomNeeded = buf.getInt();
                r = buf.getFloat(); g = buf.getFloat(); b = buf.getFloat(); a = buf.getFloat();
                break;
            case 1:
            case 2:
                this.x = EncodingUtils.decodeDouble(buf); this.y = EncodingUtils.decodeDouble(buf); this.z = EncodingUtils.decodeDouble(buf);
                byte zoomNeeded = buf.get();
                switch (zoomNeeded) {
                    case 0: this.zoomNeeded = 0; break;
                    case 1: this.zoomNeeded = -1000; break;
                    case 2: this.zoomNeeded = -1; break;
                    case -1: this.zoomNeeded = buf.getInt(); break;
                    default: throw new IllegalArgumentException(String.format("Invalid waypoint (format %s)\nIllegal waypoint zoomNeeded", format));
                }
                r = buf.getFloat(); g = buf.getFloat(); b = buf.getFloat(); a = buf.getFloat();
                break;
        }

        if (!(0 <= r && r <= 1 && 0 <= g && g <= 1 && 0 <= b && b <= 1 && 0 <= a && a <= 1)) {
            throw new IllegalArgumentException(String.format("Invalid waypoint (format %d)\nColour out of range", format));
        }
        this.color = new CustomColor(r, g, b, a);

        int type = Byte.toUnsignedInt(buf.get());

        if (type >= WaypointType.values().length) {
            throw new IllegalArgumentException(String.format("Invalid waypoint (format %s)\nWaypoint type out of range", format));
        }
        this.type = WaypointType.values()[type];

        int group = Byte.toUnsignedInt(buf.get());
        if (group == 0xFF) {
            this.group = null;
        } else if (group >= WaypointType.values().length) {
            throw new IllegalArgumentException(String.format("Invalid waypoint (format %s)\nWaypoint group out of range", format));
        } else {
            this.group = WaypointType.values()[group];
        }

        if (format == 2) {
            this.showBeaconBeam = buf.get() == 1;
        }
    }

    public static List<WaypointProfile> decode(String base64) throws IllegalArgumentException {
        if (base64 == null) throw new IllegalArgumentException("Invalid waypoint list\nWas null");
        return decode(Base64.getDecoder().decode(base64));
    }

    public static List<WaypointProfile> decode(byte[] data) throws IllegalArgumentException {
        if (data == null) throw new IllegalArgumentException("Invalid waypoint list\nWas null");
        ByteBuffer buf = ByteBuffer.wrap(data);
        List<WaypointProfile> result;
        try {
            result = decode(buf);
        } catch (BufferUnderflowException e) {
            throw new IllegalArgumentException("Invalid waypoint list\nNot enough bytes");
        }
        if (buf.position() != data.length) {
            throw new IllegalArgumentException(String.format("Invalid waypoint list\nFound extra %s bytes", data.length - buf.position()));
        }
        return result;
    }

    public static List<WaypointProfile> decode(ByteBuffer buf) throws IllegalArgumentException, BufferUnderflowException {
        byte format = buf.get();
        int uformat = Byte.toUnsignedInt(format);
        if (!(0 <= uformat && uformat <= (int) currentFormat)) {
            throw new IllegalArgumentException(String.format("Invalid waypoint format (Found: %s)", format));
        }

        if (uformat == 0) {
            // First format was an int instead of a byte
            buf.position(buf.position() + 3);
        }


        int size = -1;
        switch (format) {
            case 0: size = buf.getInt(); break;
            case 1:
            case 2: size = EncodingUtils.decodeInt(buf); break;
        }
        if (size < 0 || size > 8192) {
            throw new IllegalArgumentException("Invalid waypoint list size");
        }
        List<WaypointProfile> result = new ArrayList<>(size);
        while (size-- > 0) {
            WaypointProfile wp = new WaypointProfile(null, 0, 0, 0, null, null, 0, false);
            wp.decode(format, buf);
            result.add(wp);
        }
        return result;
    }

    public JsonObject toArtemisObject() {
        JsonObject json = new JsonObject();

        json.addProperty("name", name);
        json.addProperty("color", color.toHexString());

        String zoomNeededString = "default";
        int zoomNeeded = getZoomNeeded();
        if (zoomNeeded == -1000) {
            zoomNeededString = "always";
        } else if (zoomNeeded == -1) {
            zoomNeededString = "hidden";
        }

        json.addProperty("visibility", zoomNeededString);

        JsonObject location = new JsonObject();
        location.addProperty("x", (int)x);
        location.addProperty("y", (int)y);
        location.addProperty("z", (int)z);

        json.add("location", location);

        json.addProperty("icon", type.artemisName);

        return json;
    }

    public enum WaypointType {

        FLAG("Flag", "flag"),
        DIAMOND("Diamond", "diamond"),
        SIGN("Sign", "sign"),
        STAR("Star", "star"),
        // Artemis doesn't have a waypoint type for this, but has wall :)
        TURRET("Turret", "wall"),
        LOOTCHEST_T4("Chest (T4)", "chestT4"),
        LOOTCHEST_T3("Chest (T3)", "chestT3"),
        LOOTCHEST_T2("Chest (T2)", "chestT2"),
        LOOTCHEST_T1("Chest (T1)", "chestT1"),
        FARMING("Farming", "farming"),
        FISHING("Fishing", "fishing"),
        MINING("Mining", "mining"),
        WOODCUTTING("Woodcutting", "woodcutting");

        private String displayName;
        private String artemisName;

        WaypointType(String displayName, String artemisName) {
            this.displayName = displayName;
            this.artemisName = artemisName;
        }
    }
}

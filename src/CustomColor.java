
public class CustomColor {
    public float
            r,  // The RED   value of the color(0.0f -> 1.0f)
            g,  // The GREEN value of the color(0.0f -> 1.0f)
            b,  // The BLUE  value of the color(0.0f -> 1.0f)
            a;  // The ALPHA value of the color(0.0f -> 1.0f)

    public CustomColor(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }


    public int toInt() {
        int r = (int) (Math.min(this.r, 1f) * 255);
        int g = (int) (Math.min(this.g, 1f) * 255);
        int b = (int) (Math.min(this.b, 1f) * 255);
        int a = (int) (Math.min(this.a, 1f) * 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public String toHexString() {
        String hex = Integer.toHexString(this.toInt());
        // whether alpha portion is 1 digit or 2
        hex = (hex.length() > 7) ? hex.substring(2) : hex.substring(1);
        hex = "#" + hex;

        return hex;
    }
}

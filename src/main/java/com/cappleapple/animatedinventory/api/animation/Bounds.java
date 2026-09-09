package com.cappleapple.animatedinventory.api.animation;

/** Rectangle in logical GUI pixels, before any visual animation transform. */
public record Bounds(double x, double y, double width, double height) {
    public Bounds {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(width)
                || !Double.isFinite(height) || width < 0 || height < 0) {
            throw new IllegalArgumentException("Bounds must be finite with nonnegative size");
        }
    }
    public static Bounds item(double x, double y) { return new Bounds(x, y, 16, 16); }
    public double centerX() { return x + width / 2; }
    public double centerY() { return y + height / 2; }
    public boolean contains(double px, double py) { return px >= x && py >= y && px < x + width && py < y + height; }
    public boolean intersects(Bounds other) {
        return x < other.x + other.width && x + width > other.x && y < other.y + other.height && y + height > other.y;
    }
    public Bounds intersect(Bounds other) {
        double nx = Math.max(x, other.x), ny = Math.max(y, other.y);
        return new Bounds(nx, ny, Math.max(0, Math.min(x + width, other.x + other.width) - nx),
                Math.max(0, Math.min(y + height, other.y + other.height) - ny));
    }
    public Bounds interpolate(Bounds to, double t) {
        return new Bounds(x + (to.x - x) * t, y + (to.y - y) * t,
                Math.max(0, width + (to.width - width) * t), Math.max(0, height + (to.height - height) * t));
    }
    public Bounds offset(double dx, double dy) { return new Bounds(x + dx, y + dy, width, height); }
}

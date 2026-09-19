package com.cappleapple.animatedinventory.client.render;

import com.cappleapple.animatedinventory.mixin.GuiGraphicsAccess;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.*;
import net.minecraft.client.renderer.state.gui.pip.OversizedItemRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

import java.lang.reflect.*;
import java.util.*;

/** Replays immutable GUI extraction output through Minecraft's current render backend. */
public final class TextureCompositor {
    private static final Map<PictureInPictureRenderState, Effect> PICTURES = Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Map<String, Field> TEXT_FIELDS = fields(GuiTextRenderState.class,
            "font", "text", "x", "y", "color", "backgroundColor", "dropShadow", "includeEmpty");
    private static final Field FIRST_BLUR = field(GuiRenderState.class, "firstStratumAfterBlur");
    private static final Field MOUSE_X = field(GuiGraphicsExtractor.class, "mouseX");
    private static final Field MOUSE_Y = field(GuiGraphicsExtractor.class, "mouseY");
    private static final Field CURSOR = field(GuiGraphicsExtractor.class, "pendingCursor");
    private static final Field SCISSOR_STACK = field(GuiGraphicsExtractor.class, "scissorStack");
    private static final Method SCISSOR_PEEK = method(SCISSOR_STACK.getType(), "peek");

    public static GuiRenderState state(GuiGraphicsExtractor graphics) {
        return ((GuiGraphicsAccess)graphics).animatedinventory$renderState();
    }

    public static GuiGraphicsExtractor isolate(GuiGraphicsExtractor graphics, CaptureState state) {
        var isolated = new GuiGraphicsExtractor(Minecraft.getInstance(), state, integer(MOUSE_X, graphics), integer(MOUSE_Y, graphics));
        ScreenRectangle clip = clip(graphics);
        if (clip != null) isolated.enableScissor(clip.left(), clip.top(), clip.right(), clip.bottom());
        isolated.pose().set(graphics.pose());
        return isolated;
    }

    public static void draw(GuiGraphicsExtractor graphics, Snapshot snapshot, Matrix3x2fc transform, float alpha) {
        float opacity = Math.clamp(alpha, 0, 1);
        if (opacity <= 0) return;
        GuiRenderState target = state(graphics);
        ScreenRectangle outerClip = clip(graphics);
        for (Object command : snapshot.commands()) {
            if (command == Boundary.STRATUM) { target.nextStratum(); continue; }
            if (command == Boundary.UP) { target.up(); continue; }
            if (command == Boundary.BLUR) {
                if (target instanceof CaptureState || integer(FIRST_BLUR, target) == Integer.MAX_VALUE) target.blurBeforeThisStratum();
                continue;
            }
            if (command instanceof GuiItemRenderState item) {
                var copy = new GuiItemRenderState(new Matrix3x2f(transform).mul(item.pose()),
                        item.itemStackRenderState(), item.x(), item.y(), transformedClip(item.scissorArea(), transform, outerClip));
                ItemOpacityLayer.opacity(copy, opacity * ItemOpacityLayer.opacity(item));
                target.addItem(copy);
            } else if (command instanceof GuiTextRenderState text) {
                target.addText(copyText(text, transform, opacity, outerClip));
            } else if (command instanceof PictureInPictureRenderState picture) {
                var copy = copyPicture(picture, transform, outerClip);
                PICTURES.put(copy, new Effect(new Matrix3x2f(transform), opacity, outerClip));
                target.addPicturesInPictureState(copy);
            } else if (command instanceof GuiElementRenderState element) {
                target.addGuiElement(element(element, transform, opacity, outerClip));
            }
        }
    }

    /** Called at vanilla's PIP texture submission, including oversized item models. */
    public static BlitRenderState pictureBlit(BlitRenderState blit, PictureInPictureRenderState picture) {
        Effect effect = PICTURES.get(picture);
        float opacity = picture instanceof OversizedItemRenderState oversized
                ? ItemOpacityLayer.opacity(oversized.guiItemRenderState()) : 1;
        if (effect == null) return tint(blit, opacity);
        return new BlitRenderState(blit.pipeline(), blit.textureSetup(), new Matrix3x2f(effect.transform()).mul(blit.pose()),
                blit.x0(), blit.y0(), blit.x1(), blit.y1(), blit.u0(), blit.u1(), blit.v0(), blit.v1(),
                color(blit.color(), opacity * effect.alpha(), true),
                transformedClip(blit.scissorArea(), effect.transform(), effect.clip()), null);
    }

    static BlitRenderState tint(BlitRenderState blit, float alpha) {
        if (alpha >= .999f) return blit;
        return new BlitRenderState(blit.pipeline(), blit.textureSetup(), blit.pose(), blit.x0(), blit.y0(), blit.x1(), blit.y1(),
                blit.u0(), blit.u1(), blit.v0(), blit.v1(), color(blit.color(), alpha, true), blit.scissorArea(), blit.bounds());
    }

    private static PictureInPictureRenderState copyPicture(PictureInPictureRenderState picture, Matrix3x2fc transform, ScreenRectangle outerClip) {
        // Keep the native renderer's exact record class and original texture size; only placement bounds change.
        if (!picture.getClass().isRecord()) return picture;
        try {
            RecordComponent[] components = picture.getClass().getRecordComponents();
            Class<?>[] types = new Class<?>[components.length];
            Object[] values = new Object[components.length];
            for (int i = 0; i < components.length; i++) {
                types[i] = components[i].getType();
                values[i] = components[i].getAccessor().invoke(picture);
                if (components[i].getName().equals("bounds") && values[i] instanceof ScreenRectangle bounds) {
                    ScreenRectangle transformed = bounds.transformMaxBounds(transform);
                    values[i] = outerClip == null ? transformed : transformed.intersection(outerClip);
                }
            }
            return (PictureInPictureRenderState)picture.getClass().getDeclaredConstructor(types).newInstance(values);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot snapshot picture-in-picture state " + picture.getClass().getName(), error);
        }
    }
    private static GuiTextRenderState copyText(GuiTextRenderState text, Matrix3x2fc transform, float alpha, ScreenRectangle outerClip) {
        return new GuiTextRenderState(value(text, "font"), value(text, "text"), new Matrix3x2f(transform).mul(text.pose),
                value(text, "x"), value(text, "y"), color(value(text, "color"), alpha, false),
                color(value(text, "backgroundColor"), alpha, false), value(text, "dropShadow"), value(text, "includeEmpty"),
                transformedClip(text.scissor, transform, outerClip));
    }

    /* A proxy keeps the same source compatible with 26.2 and 26.3's different GPU pipeline packages. */
    private static GuiElementRenderState element(GuiElementRenderState original, Matrix3x2fc transform, float alpha, ScreenRectangle outerClip) {
        Matrix3x2f matrix = new Matrix3x2f(transform);
        ScreenRectangle clip = transformedClip(original.scissorArea(), matrix, outerClip);
        ScreenRectangle bounds = original.bounds() == null ? null : original.bounds().transformMaxBounds(matrix);
        ScreenRectangle clippedBounds = bounds == null || clip == null ? bounds : bounds.intersection(clip);
        boolean premultiplied = original.pipeline() == RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA;
        return (GuiElementRenderState)Proxy.newProxyInstance(TextureCompositor.class.getClassLoader(), new Class<?>[]{GuiElementRenderState.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "buildVertices" -> { original.buildVertices(vertices((VertexConsumer)args[0], matrix, alpha, premultiplied)); yield null; }
                    case "scissorArea" -> clip;
                    case "bounds" -> clippedBounds;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "AnimatedInventoryGuiElement[" + original + "]";
                    default -> invoke(method, original, args);
                });
    }

    static VertexConsumer vertices(VertexConsumer delegate, Matrix3x2fc pose, float alpha, boolean premultiplied) {
        return (VertexConsumer)Proxy.newProxyInstance(TextureCompositor.class.getClassLoader(), new Class<?>[]{VertexConsumer.class}, (proxy, method, args) -> {
            if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
            if (method.getName().equals("addVertex") && args.length == 3) {
                float x = (float)args[0], y = (float)args[1];
                delegate.addVertex(pose.m00() * x + pose.m10() * y + pose.m20(), pose.m01() * x + pose.m11() * y + pose.m21(), (float)args[2]);
                return proxy;
            }
            if (method.getName().equals("setColor")) {
                if (args.length == 1) delegate.setColor(color((int)args[0], alpha, premultiplied));
                else {
                    float rgb = premultiplied ? alpha : 1;
                    delegate.setColor(Math.round((int)args[0] * rgb), Math.round((int)args[1] * rgb),
                            Math.round((int)args[2] * rgb), Math.round((int)args[3] * alpha));
                }
                return proxy;
            }
            Object result = invoke(method, delegate, args);
            return result instanceof VertexConsumer ? proxy : result;
        });
    }

    static int color(int argb, float alpha, boolean premultiplied) {
        float rgb = premultiplied ? alpha : 1;
        return Math.round((argb >>> 24) * alpha) << 24 | Math.round((argb >> 16 & 255) * rgb) << 16
                | Math.round((argb >> 8 & 255) * rgb) << 8 | Math.round((argb & 255) * rgb);
    }

    static ScreenRectangle transformedClip(ScreenRectangle clip, Matrix3x2fc matrix, ScreenRectangle outer) {
        ScreenRectangle transformed = clip == null ? null : clip.transformMaxBounds(matrix);
        if (outer == null) return transformed;
        if (transformed == null) return outer;
        ScreenRectangle intersection = transformed.intersection(outer);
        return intersection == null ? new ScreenRectangle(0, 0, 0, 0) : intersection;
    }

    static ScreenRectangle clip(GuiGraphicsExtractor graphics) {
        try { return (ScreenRectangle)SCISSOR_PEEK.invoke(SCISSOR_STACK.get(graphics)); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException("Cannot inspect GUI clipping", error); }
    }
    private static Map<String, Field> fields(Class<?> type, String... names) {
        Map<String, Field> fields = new HashMap<>();
        for (String name : names) fields.put(name, field(type, name));
        return Map.copyOf(fields);
    }
    private static Field field(Class<?> type, String name) {
        try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field; }
        catch (ReflectiveOperationException error) { throw new ExceptionInInitializerError(error); }
    }
    private static Method method(Class<?> type, String name) {
        try { Method method = type.getDeclaredMethod(name); method.setAccessible(true); return method; }
        catch (ReflectiveOperationException error) { throw new ExceptionInInitializerError(error); }
    }
    @SuppressWarnings("unchecked") private static <T> T value(GuiTextRenderState text, String name) {
        try { return (T)TEXT_FIELDS.get(name).get(text); }
        catch (IllegalAccessException error) { throw new IllegalStateException(error); }
    }
    private static Object invoke(Method method, Object receiver, Object[] args) throws Throwable {
        try { return method.invoke(receiver, args); }
        catch (InvocationTargetException error) { throw error.getCause(); }
    }
    static void copyCursor(GuiGraphicsExtractor from, GuiGraphicsExtractor to) {
        try { CURSOR.set(to, CURSOR.get(from)); }
        catch (IllegalAccessException error) { throw new IllegalStateException("Cannot preserve requested cursor", error); }
    }
    private static int integer(Field field, Object receiver) {
        try { return field.getInt(receiver); }
        catch (IllegalAccessException error) { throw new IllegalStateException(error); }
    }    public static void clear() { PICTURES.clear(); }

    public record Snapshot(List<Object> commands) { public Snapshot { commands = List.copyOf(commands); } }
    private record Effect(Matrix3x2f transform, float alpha, ScreenRectangle clip) { }
    private enum Boundary { STRATUM, UP, BLUR }

    /** Records extraction commands, preserving the original ordering and stratum boundaries. */
    public static final class CaptureState extends GuiRenderState {
        private final List<Object> commands = new ArrayList<>();
        private GuiRenderState forward;
        @Override public void nextStratum() {
            if (forward != null) forward.nextStratum();
            else if (commands != null) commands.add(Boundary.STRATUM);
        }
        @Override public void up() { if (forward != null) forward.up(); else commands.add(Boundary.UP); }
        @Override public void blurBeforeThisStratum() { if (forward != null) forward.blurBeforeThisStratum(); else commands.add(Boundary.BLUR); }
        @Override public void addItem(GuiItemRenderState item) { if (forward != null) forward.addItem(item); else commands.add(item); }
        @Override public void addText(GuiTextRenderState text) { if (forward != null) forward.addText(text); else commands.add(text); }
        @Override public void addPicturesInPictureState(PictureInPictureRenderState picture) {
            if (forward != null) forward.addPicturesInPictureState(picture); else commands.add(picture);
        }
        @Override public void addGuiElement(GuiElementRenderState element) { if (forward != null) forward.addGuiElement(element); else commands.add(element); }
        @Override public void addBlitToCurrentLayer(BlitRenderState element) { if (forward != null) forward.addBlitToCurrentLayer(element); else commands.add(element); }
        @Override public void addGlyphToCurrentLayer(GuiElementRenderState element) { if (forward != null) forward.addGlyphToCurrentLayer(element); else commands.add(element); }
        public Snapshot snapshot() { return new Snapshot(commands); }
        public void forwardTo(GuiRenderState destination) { forward = destination; commands.clear(); }
    }
    private TextureCompositor() { }
}

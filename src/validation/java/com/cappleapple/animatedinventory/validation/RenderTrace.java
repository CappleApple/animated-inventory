package com.cappleapple.animatedinventory.validation;

import java.util.ArrayList;
import java.util.List;

public final class RenderTrace {
    public static final List<String> calls = new ArrayList<>();
    public static double selectorLogical, selectorVisual;
    public static long selectorFrames;
    private RenderTrace() { }
}

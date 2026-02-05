package ru.smcsystem.smcmodules.module;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class ShapeElementXml {
    private String type;
    private String name;
    private int width;
    private int height;
    private String innerHtml;
    private List<ShapeElementXml> childs;
    private String htmlAttrs;
    private String innerHtmlDirect;

    public ShapeElementXml(Shape shape, List<Shape> shapes) {
        this.width = shape.getWidth();
        this.height = shape.getHeight();
        this.name = shape.getName();
        String[] arrLines = shape.getDescription().split("\n");
        if (arrLines.length > 0)
            this.type = arrLines[0].trim().toLowerCase();
        if ((this.type == null || this.type.isBlank())) {
            if (shape.getType() == ShapeType.text) {
                this.type = "label";
            } else {
                this.type = "panel";
            }
        }

        Map<String, String> htmlElementAttrs = new HashMap<>();
        htmlElementAttrs.putIfAbsent("id", name);
        htmlElementAttrs.putIfAbsent("preferredSize", width + "," + height);
        if (shape.getText() != null && !shape.getText().isBlank())
            htmlElementAttrs.putIfAbsent("text", shape.getText());
        if (shape.getType() == ShapeType.text && shape.getFontSize() != null) {
            Font font = Font.decode(null);
            htmlElementAttrs.putIfAbsent("font", String.format("%s-%s-%d", font.getFamily(), font.getStyle(), shape.getFontSize()));
        }
        if (shape.getColor() != null && shape.getColor() > 1) {
            String colorStr = intToRgbText(shape.getColor());
            htmlElementAttrs.putIfAbsent("foreground", colorStr);
            if (shape.getFilled() != null && shape.getFilled())
                htmlElementAttrs.putIfAbsent("background", colorStr);
        }
        if (Objects.equals(type, "panel") && shape.getStrokeWidth() != null && shape.getStrokeWidth() != 1 && shape.getColor() != null && shape.getColor() > 0) {
            htmlElementAttrs.putIfAbsent("border", String.format("MatteBorder(%d,%d,%d,%d,%s)",
                    shape.getStrokeWidth().intValue(), shape.getStrokeWidth().intValue(), shape.getStrokeWidth().intValue(), shape.getStrokeWidth().intValue(), intToRgbText(shape.getColor())));
        }

        if (arrLines.length > 1) {
            String params = arrLines[1].trim();
            String[] arr1 = params.split("=");
            for (int i = 0; i + 1 < arr1.length; i++) {
                String name = arr1[i].trim();
                String val = arr1[i + 1].trim();

                int lastIndexOf = name.lastIndexOf(" ");
                if (lastIndexOf > 0 && name.length() > lastIndexOf + 1)
                    name = name.substring(lastIndexOf).trim();
                if (arr1.length > i + 2) {
                    int lastIndexOf2 = val.lastIndexOf(" ");
                    if (lastIndexOf2 > 0)
                        val = val.substring(0, lastIndexOf2).trim();
                }

                if (val.startsWith("\""))
                    val = val.substring(1);
                if (val.endsWith("\""))
                    val = val.substring(0, val.length() - 1);
                htmlElementAttrs.put(name, val);
            }
        }
        childs = null;
        innerHtml = null;
        if (arrLines.length > 2)
            innerHtml = String.join("\n", Arrays.asList(arrLines).subList(2, arrLines.length));

        htmlAttrs = htmlElementAttrs.entrySet().stream().map(e -> e.getKey() + "=\"" + e.getValue() + "\"").collect(Collectors.joining(" "));

        List<Shape> childShapes = shapes.stream()
                .filter(s -> s.getParentName() != null && Objects.equals(s.getParentName(), name))
                .collect(Collectors.toList());
        if (childs == null && !childShapes.isEmpty()) {
            childs = childShapes.stream()
                    .sorted(Comparator.comparing(s -> s.getX() * s.getY()))
                    .map(s -> new ShapeElementXml(s, shapes))
                    .collect(Collectors.toList());
            if (childs.isEmpty())
                childs = null;
        }

        innerHtmlDirect = null;
        if (Objects.equals(type, "panel") && htmlElementAttrs.containsKey("layout")) {
            String layout = htmlElementAttrs.get("layout");
            if (!layout.isBlank())
                innerHtmlDirect = String.format("<layout type=\"%s\" />", layout);
        }
    }

    public String genXml(String padding) {
        String innerHtmlResult = innerHtml;
        if (childs != null && innerHtmlResult == null)
            innerHtmlResult = childs.stream().map(s -> s.genXml(padding + "\t")).collect(Collectors.joining("\n"));

        if (innerHtmlDirect != null)
            innerHtmlResult = padding + "\t" + innerHtmlDirect + "\n" + innerHtmlResult;

        return innerHtmlResult == null || innerHtmlResult.isBlank() ?
                String.format("%s<%s %s/>", padding, type, htmlAttrs) :
                String.format("%s<%s %s>\n%s\n%s</%s>", padding, type, htmlAttrs, innerHtmlResult, padding, type);
    }

    public String getType() {
        return type;
    }

    public static String intToRgbText(int colorInt) {
        // if (colorInt == null)
        //     return "#000000";
        int red = (colorInt >> 16) & 0xFF;
        int green = (colorInt >> 8) & 0xFF;
        int blue = colorInt & 0xFF;
        return String.format("%02x%02x%02x", red, green, blue);
    }
}

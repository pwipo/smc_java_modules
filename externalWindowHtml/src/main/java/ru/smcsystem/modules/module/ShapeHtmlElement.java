package ru.smcsystem.modules.module;

import java.util.*;
import java.util.stream.Collectors;

public class ShapeHtmlElement {
    // private Shape shape;
    private String type;
    private Map<String, String> htmlElementAttrs;
    private String innerHtml;
    private List<ShapeHtmlElement> childs;
    private String htmlAttrs;
    private int x;
    private int y;
    private String decorationElement;

    public ShapeHtmlElement(String type, Map<String, String> htmlElementAttrs, List<Shape> childShapes, List<Shape> shapes) {
        this.type = type;
        childs = childShapes != null ?
                childShapes.stream()
                        .map(s -> new ShapeHtmlElement(s, shapes))
                        .sorted(Comparator.comparing(s -> s.x * s.y))
                        .collect(Collectors.toList()) :
                null;
        if (childs != null && childs.isEmpty())
            childs = null;
        this.htmlElementAttrs = htmlElementAttrs;
        htmlAttrs = htmlElementAttrs.entrySet().stream().map(e -> e.getKey() + "=\"" + e.getValue() + "\"").collect(Collectors.joining(" "));
        innerHtml = null;
        x = 0;
        y = 0;
        decorationElement = null;
    }

    public ShapeHtmlElement(Shape shape, List<Shape> shapes) {
        // this.shape = shape;
        this.x = shape.getX();
        this.y = shape.getY();
        String[] arrLines = shape.getDescription().split("\n");
        if (arrLines.length > 0)
            this.type = arrLines[0].trim().toLowerCase();

        htmlElementAttrs = new HashMap<>();
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
        List<Shape> childShapes = shapes.stream()
                .filter(s -> s.getParentName() != null && Objects.equals(s.getParentName(), shape.getName()))
                .collect(Collectors.toList());
        htmlElementAttrs.putIfAbsent("id", shape.getName());
        innerHtml = null;
        if (arrLines.length > 2)
            innerHtml = String.join("\n", Arrays.asList(arrLines).subList(2, arrLines.length));
        decorationElement = null;

        // htmlElementAttrs.putIfAbsent("width", shape.getWidth().toString());
        // htmlElementAttrs.putIfAbsent("height", shape.getHeight().toString());
        // htmlElementAttrs.putIfAbsent("border", shape.getStrokeWidth().toString());
        int workX = shape.getX();
        int workY = shape.getY();
        int maxPrevX = 0;
        int maxPrevY = 0;
        for (Shape s : shapes) {
            if (Objects.equals(s.getParentName(), shape.getParentName())) {
                int vX = s.getX() + s.getWidth();
                if (vX < workX && maxPrevX < vX)
                    maxPrevX = vX;
                int vY = s.getY() + s.getHeight();
                if (vY < workY && maxPrevY < vY)
                    maxPrevY = vY;
            }
        }
        workX -= maxPrevX;
        workY -= maxPrevY;
        decorationElement = String.format("<table cellspacing=\"0\" cellpadding=\"0\" border=\"%d\"><tbody>" +
                        "<tr><td width=\"%d\" height=\"%d\"></td><td></td></tr>" +
                        "<tr><td width=\"%d\"></td><td width=\"%d\" height=\"%d\">%%s</td></tr>" +
                        "</tbody></table>",
                0, workX, workY, workX, shape.getWidth(), shape.getHeight());
        switch (this.type) {
            case "input":
                htmlElementAttrs.putIfAbsent("name", shape.getName());
                if (!shape.getText().isBlank())
                    htmlElementAttrs.putIfAbsent("value", shape.getText());
                break;
            case "textarea":
                htmlElementAttrs.putIfAbsent("name", shape.getName());
                innerHtml = shape.getText();
                break;
            case "button":
                type = "input";
                htmlElementAttrs.putIfAbsent("name", shape.getName());
                if (!shape.getText().isBlank())
                    htmlElementAttrs.putIfAbsent("value", shape.getText());
                htmlElementAttrs.putIfAbsent("type", "submit");
                break;
            case "form":
                htmlElementAttrs.putIfAbsent("action", "");
                htmlElementAttrs.putIfAbsent("method", "post");
                // decorationElement = null;
                break;
            case "a":
            case "p":
            case "td":
            case "li":
            case "strong":
            case "span":
            case "small":
            case "code":
            case "dd":
            case "dt":
            case "label":
                innerHtml = String.format("<font size=\"%d\" color=\"%s\">\n%s\n</font>",
                        Math.max(7, shape.getFontSize() / 7), intToRgbText(shape.getColor()), shape.getText());
                // decorationElement = String.format("<font size=\"%d\" color=\"%s\">%%s</font>", shape.getFontSize(), intToRgbText(shape.getColor()));
                break;
            case "pre":
                innerHtml = shape.getText();
                break;
            case "table":
                if (childShapes.stream().anyMatch(s -> s.getDescription() != null && s.getDescription().startsWith("tr"))) {
                    childs = List.of(new ShapeHtmlElement("tbody", Map.of(), childShapes, shapes));
                    childShapes = List.of();
                } else if (childShapes.size() == 1) {
                    Shape shape1 = childShapes.get(0);
                    if (shape1.getDescription() == null || shape1.getDescription().isBlank())
                        shape1.setDescription("tbody");
                } else if (childShapes.size() == 2) {
                    Shape shape1 = childShapes.get(0);
                    if (shape1.getDescription() == null || shape1.getDescription().isBlank())
                        shape1.setDescription("thead");
                    shape1 = childShapes.get(1);
                    if (shape1.getDescription() == null || shape1.getDescription().isBlank())
                        shape1.setDescription("tbody");
                }
                htmlElementAttrs.putIfAbsent("width", shape.getWidth().toString());
                htmlElementAttrs.putIfAbsent("border", shape.getStrokeWidth().toString());
                decorationElement = null;
                break;
            case "tbody":
            case "thead":
                childShapes.stream().filter(s -> s.getDescription() == null || s.getDescription().isBlank()).forEach(s -> s.setDescription("tr"));
                decorationElement = null;
                break;
            case "tr": {
                long width = childShapes.stream().mapToLong(s -> s.getX() + s.getWidth()).sum();
                long height = childShapes.stream().mapToLong(s -> s.getY() + s.getHeight()).sum();
                childs = List.of(new ShapeHtmlElement("td", Map.of("width", String.valueOf(width), "height", String.valueOf(height)), childShapes, shapes));
                childShapes = List.of();
                decorationElement = null;
                break;
            }
            case "div":
                decorationElement = String.format(decorationElement,
                        String.format("<table width=\"%d\" border=\"%d\"><tbody><tr><td width=\"%d\" height=\"%d\">%%s</td></tr></tbody></table>",
                                shape.getWidth(), shape.getStrokeWidth().intValue(), shape.getWidth(), shape.getHeight()));
                break;
            case "img":
                htmlElementAttrs.putIfAbsent("width", shape.getWidth().toString());
                htmlElementAttrs.putIfAbsent("height", shape.getHeight().toString());
                htmlElementAttrs.putIfAbsent("border", shape.getStrokeWidth().toString());
                break;
        }

        htmlAttrs = htmlElementAttrs.entrySet().stream().map(e -> e.getKey() + "=\"" + e.getValue() + "\"").collect(Collectors.joining(" "));
        if (childs == null && !childShapes.isEmpty()) {
            childs = childShapes.stream()
                    .map(s -> new ShapeHtmlElement(s, shapes))
                    .sorted(Comparator.comparing(s -> s.x * s.y))
                    .collect(Collectors.toList());
            if (childs.isEmpty())
                childs = null;
        }
    }

    public static String intToRgbText(int colorInt) {
        int red = (colorInt >> 16) & 0xFF;
        int green = (colorInt >> 8) & 0xFF;
        int blue = colorInt & 0xFF;
        return String.format("#%02x%02x%02x", red, green, blue);
    }

    public String genHtml() {
        String innerHtmlResult = innerHtml;
        if (childs != null && innerHtmlResult == null)
            innerHtmlResult = childs.stream().map(ShapeHtmlElement::genHtml).collect(Collectors.joining("\n"));

        String resultHtml = innerHtmlResult == null || innerHtmlResult.isBlank() ?
                String.format("<%s %s/>", type, htmlAttrs) :
                String.format("<%s %s>\n%s\n</%s>", type, htmlAttrs, innerHtmlResult, type);
        if (decorationElement != null)
            resultHtml = String.format(decorationElement, resultHtml);
        return resultHtml;
    }

}

package ru.smcsystem.modules.module;

import ru.smcsystem.smc.utils.SmcField;
import ru.smcsystem.smc.utils.converter.SmcConverterEnumName;

public class Shape {
    @SmcField(required = true, converter = SmcConverterEnumName.class)
    private ShapeType type;
    @SmcField(required = true)
    private String name;
    @SmcField
    private String parentName;
    @SmcField(required = true)
    private Integer x;
    @SmcField(required = true)
    private Integer y;
    @SmcField(required = true)
    private Integer width;
    @SmcField(required = true)
    private Integer height;
    @SmcField(name = "color", required = true)
    private Integer color;
    @SmcField(required = true)
    private Double strokeWidth;
    @SmcField(required = true)
    private String description;
    @SmcField
    private String text;
    @SmcField
    private Boolean filled;
    @SmcField
    private Integer point2X;
    @SmcField
    private Integer point2Y;
    @SmcField
    private Boolean showArrow1;
    @SmcField
    private Boolean showArrow2;
    @SmcField
    private byte[] imageBytes;
    @SmcField
    private Integer fontSize;
    @SmcField
    private String nameObj;
    @SmcField
    private String nameFile;
    @SmcField
    private byte[] data;

    public Shape(ShapeType type, String name, String parentName, Integer x, Integer y, Integer width, Integer height, Integer color, Double strokeWidth,
                 String description, String text, Boolean filled, Integer point2X, Integer point2Y, Boolean showArrow1, Boolean showArrow2, byte[] imageBytes,
                 Integer fontSize, String nameObj, String nameFile, byte[] data) {
        this.type = type != null ? type : ShapeType.shape;
        this.name = name;
        this.parentName = parentName;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
        this.strokeWidth = strokeWidth;
        this.description = description;
        this.text = text;
        this.filled = filled;
        this.point2X = point2X;
        this.point2Y = point2Y;
        this.showArrow1 = showArrow1;
        this.showArrow2 = showArrow2;
        this.imageBytes = imageBytes;
        this.fontSize = fontSize;
        this.nameObj = nameObj;
        this.nameFile = nameFile;
        this.data = data;
    }

    public Shape() {
        this(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public ShapeType getType() {
        return type;
    }

    public void setType(ShapeType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getColor() {
        return color;
    }

    public void setColor(Integer color) {
        this.color = color;
    }

    public Double getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(Double strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Boolean getFilled() {
        return filled;
    }

    public void setFilled(Boolean filled) {
        this.filled = filled;
    }

    public Integer getPoint2X() {
        return point2X;
    }

    public void setPoint2X(Integer point2X) {
        this.point2X = point2X;
    }

    public Integer getPoint2Y() {
        return point2Y;
    }

    public void setPoint2Y(Integer point2Y) {
        this.point2Y = point2Y;
    }

    public Boolean getShowArrow1() {
        return showArrow1;
    }

    public void setShowArrow1(Boolean showArrow1) {
        this.showArrow1 = showArrow1;
    }

    public Boolean getShowArrow2() {
        return showArrow2;
    }

    public void setShowArrow2(Boolean showArrow2) {
        this.showArrow2 = showArrow2;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public void setImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }

    public Integer getFontSize() {
        return fontSize;
    }

    public void setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
    }

    public String getNameObj() {
        return nameObj;
    }

    public void setNameObj(String nameObj) {
        this.nameObj = nameObj;
    }

    public String getNameFile() {
        return nameFile;
    }

    public void setNameFile(String nameFile) {
        this.nameFile = nameFile;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

}

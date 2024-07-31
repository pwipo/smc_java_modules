package ru.smcsystem.modules.module;

import javax.swing.*;
import javax.swing.text.Element;
import java.util.HashMap;
import java.util.Map;

public class FormElement {
    public final String id;
    public final JComponent component;
    public final MainForm.ElementType type;
    public final Element element;
    public final Map<Object, Object> cache;

    public FormElement(String id, JComponent component, MainForm.ElementType type, Element element) {
        this.id = id;
        this.component = component;
        this.type = type;
        this.element = element;
        this.cache = new HashMap<>();
    }

}

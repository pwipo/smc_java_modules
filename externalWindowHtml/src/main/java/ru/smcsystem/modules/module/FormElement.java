package ru.smcsystem.modules.module;

import javax.swing.*;
import javax.swing.text.Element;
import javax.swing.text.View;
import java.util.HashMap;
import java.util.Map;

public class FormElement {
    public final String id;
    public final JComponent component;
    public final MainForm.ElementType type;
    public final Element element;
    public final Map<Object, Object> cache;
    public final View view;

    public FormElement(String id, JComponent component, MainForm.ElementType type, Element element, View view) {
        this.id = id;
        this.component = component;
        this.type = type;
        this.element = element;
        this.cache = new HashMap<>();
        this.view = view;
    }

    public FormElement(String id, JComponent component, MainForm.ElementType type, Element element) {
        this(id, component, type, element, null);
    }

}

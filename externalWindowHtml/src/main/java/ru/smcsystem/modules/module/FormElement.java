package ru.smcsystem.modules.module;

import javax.swing.*;
import javax.swing.text.Element;

public class FormElement {
    final public String id;
    final public JComponent component;
    final public MainForm.ElementType type;
    final public Element element;

    public FormElement(String id, JComponent component, MainForm.ElementType type, Element element) {
        this.id = id;
        this.component = component;
        this.type = type;
        this.element = element;
    }

}

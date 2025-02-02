package ru.smcsystem.modules.module;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.FormView;
import javax.swing.text.html.HTML;
import java.awt.*;

class CompView extends FormView {
    private final MainForm mainForm;

    public CompView(MainForm mainForm, Element element) {
        super(element);
        this.mainForm = mainForm;
    }

    @Override
    protected Component createComponent() {
        JComponent component = (JComponent) super.createComponent();  // COMPONENT IS CREATED HERE

        Element element = getElement();
        AttributeSet attrs = element.getAttributes();
        HTML.Tag t = (HTML.Tag) attrs.getAttribute(StyleConstants.NameAttribute);
        // SimpleAttributeSet simpleAttributeSet = new SimpleAttributeSet(attrs);
        // attrs.getAttribute(StyleConstants.NameAttribute);
        // Object type = simpleAttributeSet.getAttribute(HTML.Attribute.TYPE);
        // if ("text".equals(type)) {
        //     System.out.println("create input attrs:" + simpleAttributeSet);
        // } else if ("submit".equals(type)) {
        //     System.out.println("create button attrs:" + simpleAttributeSet);
        // }

        Object idObj = attrs.getAttribute(HTML.Attribute.ID);
        String id = idObj != null ? idObj.toString() : null;
        if (id == null && (t == HTML.Tag.INPUT) ||
                (t == HTML.Tag.SELECT) ||
                (t == HTML.Tag.TEXTAREA)) {
            String name = (String) attrs.getAttribute(HTML.Attribute.NAME);
            if (name != null)
                id = name;
        }
        if (id != null) {
            if (mainForm.elements.containsKey(id)) {
                Integer ecId = mainForm.elementsToEcId.get(id);
                mainForm.elements.put(id, new FormElement(id, component, mainForm.elements.get(id).type, element));
                if (ecId != null) {
                    String idTmp = id;
                    SwingUtilities.invokeLater(() -> mainForm.addEC(idTmp, ecId));
                }
            } else {
                MainForm.ElementType elementType;
                if (t == HTML.Tag.SELECT) {
                    elementType = MainForm.ElementType.SELECT;
                } else if (t == HTML.Tag.TEXTAREA) {
                    elementType = MainForm.ElementType.TEXTAREA;
                } else {
                    try {
                        elementType = MainForm.ElementType.valueOf(((String) attrs.getAttribute(HTML.Attribute.TYPE)).toUpperCase());
                    } catch (Exception e) {
                        elementType = MainForm.ElementType.OTHER;
                    }
                }
                mainForm.elements.put(id, new FormElement(id, component, elementType, element));
            }
        }
        return component;
    }
    /*
    @Override
    protected Component createComponent() {
        Component component = super.createComponent();  // COMPONENT IS CREATED HERE
        System.out.println(component);
        JLabel layeredPane = (JLabel) component;
        layeredPane.setIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("images/9.gif"))));
        return component;
    }
    */

    @Override
    protected void submitData(String data) {
        // super.submitData(data);
    }

}

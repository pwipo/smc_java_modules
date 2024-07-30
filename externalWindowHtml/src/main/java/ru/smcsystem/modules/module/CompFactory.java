package ru.smcsystem.modules.module;

import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

class CompFactory extends HTMLEditorKit.HTMLFactory {
    private final MainForm mainForm;

    public CompFactory(MainForm mainForm) {
        super();
        this.mainForm = mainForm;
    }

    @Override
    public View create(Element element) {
        AttributeSet attrs = element.getAttributes();
        Object elementName = attrs.getAttribute(AbstractDocument.ElementNameAttribute);
        Object o = (elementName != null) ? null : attrs.getAttribute(StyleConstants.NameAttribute);
        if (o instanceof HTML.Tag) {
            HTML.Tag kind = (HTML.Tag) o;
            if ((kind == HTML.Tag.INPUT) ||
                    (kind == HTML.Tag.SELECT) ||
                    (kind == HTML.Tag.TEXTAREA)) {
                CompView compView = new CompView(mainForm, element);
                mainForm.updateLastCreatedElement();
                return compView;
            } else {
                Object idObj = attrs.getAttribute(HTML.Attribute.ID);
                String id = idObj != null ? idObj.toString() : null;
                if (id != null && !mainForm.elements.containsKey(id)) {
                    mainForm.elements.put(id, new FormElement(id, null, MainForm.ElementType.OTHER, element));
                    mainForm.updateLastCreatedElement();
                }
            }
        }
        return super.create(element);
    }
}

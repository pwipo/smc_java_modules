package ru.smcsystem.modules.module;

import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import java.util.Map;

class CompFactory extends HTMLEditorKit.HTMLFactory {
    private final MainForm mainForm;
    private final Map<String, byte[]> mapImages;

    public CompFactory(MainForm mainForm, Map<String, byte[]> mapImages) {
        super();
        this.mainForm = mainForm;
        this.mapImages = mapImages;
    }

    @Override
    public View create(Element element) {
        AttributeSet attrs = element.getAttributes();
        Object elementName = attrs.getAttribute(AbstractDocument.ElementNameAttribute);
        Object o = (elementName != null) ? null : attrs.getAttribute(StyleConstants.NameAttribute);
        if (o instanceof HTML.Tag) {
            HTML.Tag kind = (HTML.Tag) o;
            Object idObj = attrs.getAttribute(HTML.Attribute.ID);
            String id = idObj != null ? idObj.toString() : null;
            if ((kind == HTML.Tag.INPUT) ||
                    (kind == HTML.Tag.SELECT) ||
                    (kind == HTML.Tag.TEXTAREA)) {
                CompView compView = new CompView(mainForm, element);
                mainForm.updateLastCreatedElement();
                return compView;
            } else if (kind == HTML.Tag.IMG) {
                MyImageView compView = new MyImageView(element, id != null ? mapImages.get(id) : null);
                if (id != null && !mainForm.elements.containsKey(id)) {
                    mainForm.elements.put(id, new FormElement(id, null, MainForm.ElementType.IMG, element, compView));
                    mainForm.updateLastCreatedElement();
                }
                return compView;
            } else {
                if (id != null && !mainForm.elements.containsKey(id)) {
                    mainForm.elements.put(id, new FormElement(id, null, MainForm.ElementType.OTHER, element));
                    mainForm.updateLastCreatedElement();
                }
            }
        }
        return super.create(element);
    }
}

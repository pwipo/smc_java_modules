package ru.smcsystem.modules.module;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Objects;

public class HTMLElementWriter extends HTMLWriter {
    private final Element elementFirst;
    private final Element elementLast;
    private boolean find;

    public HTMLElementWriter(Writer w, HTMLDocument doc, Element elementFirst, Element elementLast) {
        super(w, doc, elementFirst.getStartOffset(), elementLast.getEndOffset() - elementFirst.getStartOffset());
        this.elementFirst = elementFirst;
        this.elementLast = elementLast;
        find = false;
    }

    public HTMLElementWriter(Writer w, HTMLDocument doc, Element element) {
        this(w, doc, element, element);
    }

    @Override
    public void write() throws IOException, BadLocationException {
        find = false;
        super.write();
        find = false;
    }

    @Override
    protected boolean inRange(Element next) {
        int startOffset = getStartOffset();
        int endOffset = getEndOffset();
        if (next.getStartOffset() >= startOffset &&
                next.getStartOffset() < endOffset) {
            if (!find) {
                if (!Objects.equals(next, elementFirst))
                    return false;
                find = true;
            }
            return true;
        }
        return false;
    }
}

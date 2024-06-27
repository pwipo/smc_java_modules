package ru.smcsystem.modules.module;

import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

public class ExtendedHTMLDocument extends HTMLDocument {
    public ExtendedHTMLDocument(StyleSheet styles) {
        super(styles);
    }

    public void hackWriteLock() {
        writeLock();
    }

    public void hackWriteUnlock() {
        writeUnlock();
    }

    public Content getContentMy() {
        return getContent();
    }

}

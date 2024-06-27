package ru.smcsystem.modules.module;

import javax.swing.text.Document;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

class CompEditorKit extends HTMLEditorKit {
    private final MainForm mainForm;

    public CompEditorKit(MainForm mainForm) {
        this.mainForm = mainForm;
    }

    @Override
    public ViewFactory getViewFactory() {
        return new CompFactory(mainForm);
    }

    @Override
    public Document createDefaultDocument() {
        StyleSheet styles = getStyleSheet();
        StyleSheet ss = new StyleSheet();

        ss.addStyleSheet(styles);

        ExtendedHTMLDocument doc = new ExtendedHTMLDocument(ss);
        doc.setParser(getParser());
        doc.setAsynchronousLoadPriority(4);
        doc.setTokenThreshold(100);
        return doc;
    }
}

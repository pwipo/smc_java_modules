package ru.smcsystem.modules.module;

import javax.swing.text.Document;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.util.Map;

class CompEditorKit extends HTMLEditorKit {
    private final MainForm mainForm;
    private final Map<String, byte[]> mapImages;

    public CompEditorKit(MainForm mainForm, Map<String, byte[]> mapImages) {
        this.mainForm = mainForm;
        this.mapImages = mapImages;
    }

    @Override
    public ViewFactory getViewFactory() {
        return new CompFactory(mainForm, mapImages);
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

package ru.smcsystem.modules.module;

import ru.smcsystem.smc.utils.SmcField;

public class OptionElement {
    @SmcField(name = "id")
    private Integer id;
    @SmcField(name = "label", required = true)
    private String label;
    @SmcField(name = "value", required = true)
    private Object value;
    @SmcField(name = "selected")
    private Boolean selected;

    public OptionElement(Integer id, String label, Object value, Boolean selected) {
        this.id = id;
        this.label = label;
        this.value = value;
        this.selected = selected;
    }

    public OptionElement() {
        this(null, null, null, false);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    public String toHtml() {
        return String.format("<option value=\"%s\" %s>%s</option>", getValue() instanceof String ? getValue() : getId(), getSelected() ? "selected" : "", getLabel());
    }
}

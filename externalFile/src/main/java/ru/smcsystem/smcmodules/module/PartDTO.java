package ru.smcsystem.smcmodules.module;

import ru.smcsystem.smc.utils.SmcField;

public class PartDTO {
    @SmcField(name = "name")
    private String name;
    @SmcField(name = "data")
    private byte[] data;
    @SmcField(name = "size")
    private Long size;

    public PartDTO(String name, byte[] data, Long size) {
        this.name = name;
        this.data = data;
        this.size = size;
    }

    public PartDTO() {
        this(null, null, null);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}

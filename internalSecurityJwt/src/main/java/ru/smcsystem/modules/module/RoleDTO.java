package ru.smcsystem.modules.module;

import ru.smcsystem.smc.utils.SmcField;

public class RoleDTO {
    @SmcField(name = "id", required = true)
    private Long id;
    @SmcField(name = "name", required = true)
    private String name;

    public RoleDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public RoleDTO() {
        this(null, null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

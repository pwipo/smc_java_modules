package ru.smcsystem.modules.module;

import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.smc.utils.SmcField;
import ru.smcsystem.smc.utils.converter.SmcConverterDate;
import ru.smcsystem.smc.utils.converter.SmcConverterEnumOrdinal;

import java.util.Date;

public class UserDTO {
    @SmcField(name = "id", required = true)
    private Long id;
    @SmcField(name = "login", required = true)
    private String login;
    @SmcField(name = "password", required = true)
    private Object password;
    @SmcField(name = "disabled", converter = SmcConverterDate.class)
    private Date disabled;
    @SmcField(name = "blocking", converter = SmcConverterDate.class)
    private Date blocking;
    @SmcField(name = "auth_type", converter = SmcConverterEnumOrdinal.class)
    private UserAuthType authType;
    private ObjectElement objectElement;

    public UserDTO(Long id, String login, Object password, Date disabled, Date blocking, UserAuthType authType) {
        this.id = id;
        this.login = login;
        this.password = password;
        this.disabled = disabled;
        this.blocking = blocking;
        this.authType = authType;
        this.objectElement = null;
    }

    public UserDTO() {
        this(null, null, null, null, null, UserAuthType.LOGIN_PASS);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public Object getPassword() {
        return password;
    }

    public void setPassword(Object password) {
        this.password = password;
    }

    public Date getDisabled() {
        return disabled;
    }

    public void setDisabled(Date disabled) {
        this.disabled = disabled;
    }

    public Date getBlocking() {
        return blocking;
    }

    public void setBlocking(Date blocking) {
        this.blocking = blocking;
    }

    public UserAuthType getAuthType() {
        return authType;
    }

    public void setAuthType(UserAuthType authType) {
        this.authType = authType;
    }

    public ObjectElement getObjectElement() {
        return objectElement;
    }

    public void setObjectElement(ObjectElement objectElement) {
        this.objectElement = objectElement;
    }
}

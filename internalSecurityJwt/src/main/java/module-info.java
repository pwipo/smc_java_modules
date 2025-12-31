module ru.smcsystem.modules.internalSecurityJwt {
    requires ru.smcsystem.api;
    requires ru.smcsystem.utils;
    // requires smallrye.jwt.build;
    // requires smallrye.jwt;
    requires bcrypt;
    requires org.apache.commons.lang3;
    requires com.google.common;
    // requires microprofile.jwt.auth.api;
    requires jjwt.api;
    requires jjwt.impl;
    exports ru.smcsystem.modules.module;
}
module ru.smcsystem.modules.externalHttpServer {
    requires ru.smcsystem.api;
    requires jdk.httpserver;
    requires org.apache.commons.lang3;
    requires org.apache.commons.collections4;
    requires commons.io;
    requires tomcat.embed.core;
    requires java.management;
    requires ru.smcsystem.utils;
}
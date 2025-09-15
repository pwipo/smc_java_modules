package ru.smcsystem.modules.module;

import ru.smcsystem.api.dto.IValue;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.dto.ObjectField;
import ru.smcsystem.test.emulate.ConfigurationToolImpl;
import ru.smcsystem.test.emulate.Value;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class MyConfigurationToolImpl extends ConfigurationToolImpl {
    public MyConfigurationToolImpl(String name, String description, Map<String, IValue> settings, String homeFolder, String workDirectory) {
        super(name, description, settings, homeFolder, workDirectory);
    }

    @Override
    public Optional<IValue> getInfo(String s) {
        if (Objects.equals(s, "decorationShapes")) {
            return Optional.of(new Value(
                    new ObjectArray(
                            new ObjectElement(
                                    new ObjectField("type", "rectangle"),
                                    new ObjectField("name", "root"),
                                    new ObjectField("description", ""),
                                    new ObjectField("x", 1),
                                    new ObjectField("y", 1),
                                    new ObjectField("width", 100),
                                    new ObjectField("height", 100),
                                    new ObjectField("color", 1),
                                    new ObjectField("strokeWidth", 1)
                            ),
                            new ObjectElement(
                                    new ObjectField("type", "rectangle"),
                                    new ObjectField("name", "form1"),
                                    new ObjectField("parentName", "root"),
                                    new ObjectField("description", "form\nattr1=\"as 1\" attr2=\"as 2\""),
                                    new ObjectField("x", 20),
                                    new ObjectField("y", 20),
                                    new ObjectField("width", 80),
                                    new ObjectField("height", 85),
                                    new ObjectField("color", 1),
                                    new ObjectField("strokeWidth", 1)
                            ),
                            new ObjectElement(
                                    new ObjectField("type", "rectangle"),
                                    new ObjectField("name", "input1"),
                                    new ObjectField("parentName", "form1"),
                                    new ObjectField("description", "textarea\n\ninput 1"),
                                    new ObjectField("x", 10),
                                    new ObjectField("y", 10),
                                    new ObjectField("width", 70),
                                    new ObjectField("height", 50),
                                    new ObjectField("color", 1),
                                    new ObjectField("strokeWidth", 1)
                            ),
                            new ObjectElement(
                                    new ObjectField("type", "rectangle"),
                                    new ObjectField("name", "input2"),
                                    new ObjectField("parentName", "form1"),
                                    new ObjectField("description", "input\ntype=\"submit\""),
                                    new ObjectField("x", 10),
                                    new ObjectField("y", 60),
                                    new ObjectField("width", 70),
                                    new ObjectField("height", 80),
                                    new ObjectField("color", 1),
                                    new ObjectField("text", "ok"),
                                    new ObjectField("strokeWidth", 1),
                                    new ObjectField("filled", true)
                            ),
                            new ObjectElement(
                                    new ObjectField("type", "rectangle"),
                                    new ObjectField("name", "div1"),
                                    new ObjectField("parentName", "root"),
                                    new ObjectField("description", "div"),
                                    new ObjectField("x", 20),
                                    new ObjectField("y", 100),
                                    new ObjectField("width", 80),
                                    new ObjectField("height", 150),
                                    new ObjectField("color", 1),
                                    new ObjectField("strokeWidth", 1),
                                    new ObjectField("filled", true)
                            ),
                            new ObjectElement(
                                    new ObjectField("type", "rectangle"),
                                    new ObjectField("name", "image1"),
                                    new ObjectField("parentName", "root"),
                                    new ObjectField("description", "img"),
                                    new ObjectField("x", 20),
                                    new ObjectField("y", 200),
                                    new ObjectField("width", 80),
                                    new ObjectField("height", 100),
                                    new ObjectField("color", 1),
                                    new ObjectField("strokeWidth", 1),
                                    new ObjectField("filled", true),
                                    new ObjectField("imageBytes", "23556".getBytes())
                            ),
                            new ObjectElement(
                                    new ObjectField("type", "text"),
                                    new ObjectField("name", "text1"),
                                    new ObjectField("parentName", "root"),
                                    new ObjectField("description", "p"),
                                    new ObjectField("x", 20),
                                    new ObjectField("y", 250),
                                    new ObjectField("width", 80),
                                    new ObjectField("height", 30),
                                    new ObjectField("color", -16777216),
                                    new ObjectField("strokeWidth", 1),
                                    new ObjectField("filled", true),
                                    new ObjectField("text", "hello world"),
                                    new ObjectField("fontSize", 10)
                            ),
                            new ObjectElement(
                                    new ObjectField("type", "text"),
                                    new ObjectField("name", "link1"),
                                    new ObjectField("parentName", "root"),
                                    new ObjectField("description", "a\nhref=\"google.com/sd\""),
                                    new ObjectField("x", 20),
                                    new ObjectField("y", 300),
                                    new ObjectField("width", 80),
                                    new ObjectField("height", 30),
                                    new ObjectField("color", 1),
                                    new ObjectField("strokeWidth", 1),
                                    new ObjectField("filled", true),
                                    new ObjectField("text", "link"),
                                    new ObjectField("fontSize", 10)
                            )
                    )
            ));
        }
        return super.getInfo(s);
    }
}

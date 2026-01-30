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
                                    new ObjectField("type", "rhombus"),
                                    new ObjectField("name", "if1"),
                                    new ObjectField("description", "$src0.-1\neq\n1"),
                                    new ObjectField("x", 100),
                                    new ObjectField("y", 1),
                                    new ObjectField("width", 100),
                                    new ObjectField("height", 100),
                                    new ObjectField("color", 1),
                                    new ObjectField("strokeWidth", 1)
                            ),
                            new ObjectElement(
                                    new ObjectField("type", "rectangle"),
                                    new ObjectField("name", "exec1"),
                                    new ObjectField("description", "0\n\"test\""),
                                    new ObjectField("x", 1),
                                    new ObjectField("y", 150),
                                    new ObjectField("width", 100),
                                    new ObjectField("height", 100),
                                    new ObjectField("color", 1),
                                    new ObjectField("strokeWidth", 1)
                            ),
                            new ObjectElement(
                                    new ObjectField("type", "circle"),
                                    new ObjectField("name", "output1"),
                                    new ObjectField("description", "\"success\"\n$ec0"),
                                    new ObjectField("x", 10),
                                    new ObjectField("y", 300),
                                    new ObjectField("width", 50),
                                    new ObjectField("height", 50),
                                    new ObjectField("color", 1),
                                    new ObjectField("strokeWidth", 1)
                            ),
                            new ObjectElement(
                                    new ObjectField("type", "circle"),
                                    new ObjectField("name", "output2"),
                                    new ObjectField("description", "\"fail\"\n$src0"),
                                    new ObjectField("x", 200),
                                    new ObjectField("y", 150),
                                    new ObjectField("width", 50),
                                    new ObjectField("height", 50),
                                    new ObjectField("color", 1),
                                    new ObjectField("strokeWidth", 1)
                            ),
                            new ObjectElement(
                                    new ObjectField("type", "line"),
                                    new ObjectField("name", "line1"),
                                    new ObjectField("description", "1"),
                                    new ObjectField("x", 150),
                                    new ObjectField("y", 100),
                                    new ObjectField("width", 100),
                                    new ObjectField("height", 55),
                                    new ObjectField("point2X", 50),
                                    new ObjectField("point2Y", 155),
                                    new ObjectField("color", 1),
                                    new ObjectField("strokeWidth", 1)
                            ),
                            new ObjectElement(
                                    new ObjectField("type", "line"),
                                    new ObjectField("name", "line2"),
                                    new ObjectField("description", "1"),
                                    new ObjectField("x", 50),
                                    new ObjectField("y", 250),
                                    new ObjectField("width", 15),
                                    new ObjectField("height", 55),
                                    new ObjectField("point2X", 35),
                                    new ObjectField("point2Y", 305),
                                    new ObjectField("color", 1),
                                    new ObjectField("strokeWidth", 1)
                            ),
                            new ObjectElement(
                                    new ObjectField("type", "line"),
                                    new ObjectField("name", "line3"),
                                    new ObjectField("description", "1"),
                                    new ObjectField("x", 150),
                                    new ObjectField("y", 100),
                                    new ObjectField("width", 75),
                                    new ObjectField("height", 55),
                                    new ObjectField("point2X", 225),
                                    new ObjectField("point2Y", 155),
                                    new ObjectField("color", 1),
                                    new ObjectField("strokeWidth", 1)
                            )
                    )
            ));
        }
        return super.getInfo(s);
    }
}

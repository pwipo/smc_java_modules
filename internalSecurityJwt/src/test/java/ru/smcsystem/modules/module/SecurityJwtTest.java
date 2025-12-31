package ru.smcsystem.modules.module;

import org.junit.Test;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.dto.ObjectField;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.smc.utils.ModuleUtils;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.List;
import java.util.Map;

public class SecurityJwtTest {

    @Test
    public void process() {
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        Map.of(
                                "issuer", new Value("test.com"),
                                "accessTokenExpires", new Value(180),
                                "refreshTokenExpires", new Value(60 * 60 * 2),
                                "publicKey", new Value("public.key.txt"),
                                "privateKey", new Value("private.key.txt"),
                                "bcryptCost", new Value(11),
                                "authSleep", new Value(1000),
                                "fieldNames", new Value("email, tel")
                        ),
                        null,
                        "C:\\tmp\\5"
                ),
                new SecurityJwt()
        );
        process.start();

        ExecutionContextToolImpl executionContextTool;

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(new Value("pass"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null, null, null, "ec", "gen_hash");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        String passHash = ModuleUtils.getString(executionContextTool.getOutput().get(0));
        executionContextTool.getOutput().clear();


        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(new Value("user")),
                                                new Message(new Value("pass"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                List.of(
                        new Action(
                                List.of(
                                        new Message(new Value(new ObjectArray(new ObjectElement(
                                                new ObjectField("id", 3L),
                                                new ObjectField("login", "user"),
                                                new ObjectField("password", passHash),
                                                new ObjectField("email", "test@test.com"),
                                                new ObjectField("tel", "78038564986")
                                        ))))
                                ),
                                ActionType.EXECUTE
                        ),
                        new Action(
                                List.of(
                                        new Message(new Value(new ObjectArray(new ObjectElement(
                                                new ObjectField("id", 1L),
                                                new ObjectField("name", "users")
                                        ))))
                                ),
                                ActionType.EXECUTE
                        ),
                        new Action(
                                List.of(
                                        new Message(new Value(true))
                                ),
                                ActionType.EXECUTE
                        )), null, "ec", "login");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        ObjectElement objectElement = (ObjectElement) ModuleUtils.getObjectArray(executionContextTool.getOutput().get(3)).get(0);
        String accessToken = objectElement.findField("accessToken").map(ModuleUtils::toString).get();
        String refreshToken = objectElement.findField("refreshToken").map(ModuleUtils::toString).get();
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(new Value(accessToken))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null, null, null, "ec", "parse");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        ObjectElement objectElementToken = (ObjectElement) ModuleUtils.getObjectArray(executionContextTool.getOutput().get(0)).get(0);
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(new Value(new ObjectArray(objectElementToken))),
                                                new Message(new Value("users"))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null, null, null, "ec", "has_role");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        executionContextTool.getOutput().clear();

        executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(new Value(refreshToken))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                null,
                List.of(
                        (l) -> {
                            System.out.println(l);
                            return new Action(List.of(
                                    new Message(new Value(true))
                            ));
                        }
                ), "ec", "refresh_tokens");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        objectElement = (ObjectElement) ModuleUtils.getObjectArray(executionContextTool.getOutput().get(1)).get(0);
        accessToken = objectElement.findField("accessToken").map(ModuleUtils::toString).get();
        refreshToken = objectElement.findField("refreshToken").map(ModuleUtils::toString).get();
        executionContextTool.getOutput().clear();

        process.stop();
    }
}

package ru.smcsystem.modules.module;

import org.junit.Test;
import ru.smcsystem.api.dto.IValue;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.dto.ObjectField;
import ru.smcsystem.api.enumeration.ActionType;
import ru.smcsystem.smc.utils.ModuleUtils;
import ru.smcsystem.test.Process;
import ru.smcsystem.test.emulate.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SecurityJwtTest {

    @Test
    public void process() {
        Map<String, IValue> settings = new HashMap<>(Map.of(
                "issuer", new Value("test.com"),
                "accessTokenExpires", new Value(180),
                "refreshTokenExpires", new Value(60 * 60 * 2),
                "publicKey", new Value("public.key.txt"),
                "privateKey", new Value("private.key.txt"),
                "bcryptCost", new Value(11),
                "authSleep", new Value(1000),
                "fieldNames", new Value("email, tel"),
                "checkRemoteAddr", new Value(false),
                "countLoginFailBeforeBlocking", new Value(5)
        ));
        settings.put("blockingTime", new Value(600));
        settings.put("plainPass", new Value("{noop}"));
        settings.put("tfaCheckFieldName", new Value("tfa_login_check"));
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        settings,
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

        ObjectElement objectElement = executeLogin(process, passHash, "user", "pass", true);
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

    @Test
    public void testBlocking() throws InterruptedException {
        Map<String, IValue> settings = new HashMap<>(Map.of(
                "issuer", new Value("test.com"),
                "accessTokenExpires", new Value(180),
                "refreshTokenExpires", new Value(60 * 60 * 2),
                "publicKey", new Value("public.key.txt"),
                "privateKey", new Value("private.key.txt"),
                "bcryptCost", new Value(11),
                "authSleep", new Value(1000),
                "fieldNames", new Value("email, tel"),
                "checkRemoteAddr", new Value(false),
                "countLoginFailBeforeBlocking", new Value(2)
        ));
        settings.put("blockingTime", new Value(10));
        settings.put("plainPass", new Value("{noop}"));
        settings.put("tfaCheckFieldName", new Value("tfa_login_check"));
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        settings,
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


        ObjectElement objectElement = executeLogin(process, passHash, "user", "pass1", true);
        objectElement = executeLogin(process, passHash, "user", "pass2", true);
        objectElement = executeLogin(process, passHash, "user", "pass3", true);
        objectElement = executeLogin(process, passHash, "user", "pass4", true);

        Thread.sleep(20 * 1000);

        objectElement = executeLogin(process, passHash, "user", "pass5", true);
        objectElement = executeLogin(process, passHash, "user", "pass", true);

        String accessToken = objectElement.findField("accessToken").map(ModuleUtils::toString).get();
        String refreshToken = objectElement.findField("refreshToken").map(ModuleUtils::toString).get();
        executionContextTool.getOutput().clear();

        process.stop();
    }

    private ObjectElement executeLogin(Process process, String passHash, String login, String pass, boolean isLogin) {
        ExecutionContextToolImpl executionContextTool = new ExecutionContextToolImpl(
                List.of(
                        List.of(
                                new Action(
                                        List.of(
                                                new Message(new Value(login)),
                                                new Message(new Value(pass))
                                        ),
                                        ActionType.EXECUTE
                                ))),
                null,
                List.of(
                        new Action(
                                List.of(
                                        new Message(new Value(new ObjectArray(new ObjectElement(
                                                new ObjectField("id", 3L),
                                                new ObjectField("login", login),
                                                new ObjectField("password", passHash),
                                                new ObjectField("email", "test@test.com"),
                                                new ObjectField("tel", "78038564986"),
                                                new ObjectField("auth_type", 2)
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
                        )), null, "ec", isLogin ? "login" : "login_field");
        System.out.println("call login");
        process.execute(executionContextTool);
        executionContextTool.getOutput().forEach(m -> System.out.println(m.getMessageType() + " " + m.getValue()));
        ObjectArray objectArray = executionContextTool.getOutput().size() > 2 ? ModuleUtils.getObjectArray(executionContextTool.getOutput().get(2)) : null;
        return ModuleUtils.isArrayContainObjectElements(objectArray) ? (ObjectElement) objectArray.get(0) : null;
    }

    @Test
    public void testLoginField() {
        Map<String, IValue> settings = new HashMap<>(Map.of(
                "issuer", new Value("test.com"),
                "accessTokenExpires", new Value(180),
                "refreshTokenExpires", new Value(60 * 60 * 2),
                "publicKey", new Value("public.key.txt"),
                "privateKey", new Value("private.key.txt"),
                "bcryptCost", new Value(11),
                "authSleep", new Value(1000),
                "fieldNames", new Value("email, tel"),
                "checkRemoteAddr", new Value(false),
                "countLoginFailBeforeBlocking", new Value(5)
        ));
        settings.put("blockingTime", new Value(600));
        settings.put("plainPass", new Value("{noop}"));
        settings.put("tfaCheckFieldName", new Value("tfa_login_check"));
        Process process = new Process(
                new ConfigurationToolImpl(
                        "test",
                        null,
                        settings,
                        null,
                        "C:\\tmp\\5"
                ),
                new SecurityJwt()
        );
        process.start();

        executeLogin(process, "{noop}pass", "user", "pass", false);

        process.stop();
    }

}

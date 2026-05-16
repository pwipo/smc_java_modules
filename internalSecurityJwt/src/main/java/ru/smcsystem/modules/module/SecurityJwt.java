package ru.smcsystem.modules.module;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.DefaultClaims;
import ru.smcsystem.api.dto.IMessage;
import ru.smcsystem.api.dto.ObjectArray;
import ru.smcsystem.api.dto.ObjectElement;
import ru.smcsystem.api.dto.ObjectField;
import ru.smcsystem.api.enumeration.ObjectType;
import ru.smcsystem.api.exceptions.ModuleException;
import ru.smcsystem.api.module.Module;
import ru.smcsystem.api.tools.ConfigurationTool;
import ru.smcsystem.api.tools.execution.ExecutionContextTool;
import ru.smcsystem.smc.utils.ModuleUtils;

import java.io.File;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class SecurityJwt implements Module {
    private static final String CLAIM_NAME_GROUPS = "groups";
    private static final String CLAIM_NAME_IS_AT = "isAT";
    private static final String CLAIM_NAME_RADDR = "raddr";
    private String issuer;
    private Integer accessTokenExpires;
    private Integer refreshTokenExpires;
    private Integer bcryptCost;
    private PrivateKey privateKey;
    // private PublicKey publicKey;
    // private String publicKeyRaw;
    // private String privateKeyRaw;
    // private JWTParser parser;
    private Integer authSleep;
    private List<String> fieldNames;
    // private Cache<String, Map.Entry<UserDTO, List<RoleDTO>>> cacheRefreshToken;
    private JwtParser parser;
    private Boolean checkRemoteAddr;
    private Map<String/*login*/, Integer> loginFailsMap;
    private Map<String/*login*/, Instant> loginBlockingMap;
    private Map<String/*login*/, Lock> loginLock;
    private Map<String/*login*/, Integer> authFieldFailsMap;
    private Map<String/*login*/, Instant> authFieldBlockingMap;
    private Map<String/*login*/, Lock> authFieldLock;
    private Integer countLoginFailBeforeBlocking;
    private Integer blockingTime;
    private String plainPass;
    private String tfaCheckFieldName;

    @Override
    public void start(ConfigurationTool configurationTool) throws ModuleException {
        issuer = configurationTool.getSetting("issuer").map(ModuleUtils::toString).orElseThrow(() -> new ModuleException("issuer setting not found"));
        accessTokenExpires = configurationTool.getSetting("accessTokenExpires").map(ModuleUtils::toNumber).map(Number::intValue)
                .orElseThrow(() -> new ModuleException("accessTokenExpires setting not found"));
        refreshTokenExpires = configurationTool.getSetting("refreshTokenExpires").map(ModuleUtils::toNumber).map(Number::intValue)
                .orElseThrow(() -> new ModuleException("refreshTokenExpires setting not found"));
        String publicKeyStr = configurationTool.getSetting("publicKey").map(ModuleUtils::toString).orElseThrow(() -> new ModuleException("publicKey setting not found"));
        String privateKeyStr = configurationTool.getSetting("privateKey").map(ModuleUtils::toString).orElseThrow(() -> new ModuleException("privateKey setting not found"));
        bcryptCost = configurationTool.getSetting("bcryptCost").map(ModuleUtils::toNumber).map(Number::intValue)
                .orElseThrow(() -> new ModuleException("bcryptCost setting not found")); /*12*/
        authSleep = configurationTool.getSetting("authSleep").map(ModuleUtils::toNumber).map(Number::intValue).orElseThrow(() -> new ModuleException("authSleep setting not found"));
        fieldNames = configurationTool.getSetting("fieldNames").map(ModuleUtils::toString).filter(s -> !s.isBlank()).stream()
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .collect(Collectors.toList());
        checkRemoteAddr = configurationTool.getSetting("checkRemoteAddr").map(ModuleUtils::toBoolean)
                .orElseThrow(() -> new ModuleException("checkRemoteAddr setting not found"));

        File publicKeyFile = new File(configurationTool.getWorkDirectory(), publicKeyStr);
        File privateKeyFile = new File(configurationTool.getWorkDirectory(), privateKeyStr);
        countLoginFailBeforeBlocking = configurationTool.getSetting("countLoginFailBeforeBlocking").map(ModuleUtils::toNumber).map(Number::intValue)
                .orElseThrow(() -> new ModuleException("countLoginFailBeforeBlocking setting not found"));
        blockingTime = configurationTool.getSetting("blockingTime").map(ModuleUtils::toNumber).map(Number::intValue)
                .orElseThrow(() -> new ModuleException("blockingTime setting not found"));
        plainPass = configurationTool.getSetting("plainPass").map(ModuleUtils::toString).orElseThrow(() -> new ModuleException("plainPass setting not found"));
        tfaCheckFieldName = configurationTool.getSetting("tfaCheckFieldName").map(ModuleUtils::toString).orElseThrow(() -> new ModuleException("tfaCheckFieldName setting not found"));

        try {
            String privateKeyRaw = new String(Files.readAllBytes(privateKeyFile.toPath()));
            // Pulizia per Java (Java vuole solo il Base64 puro, senza
            // header/footer/newlines)
            String realPem = privateKeyRaw.contains("\\n") ? privateKeyRaw.replace("\\n", "\n") : privateKeyRaw;
            String privateKeyPEM = realPem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("[^A-Za-z0-9+/=]", ""); // Rimuove QUALSIASI carattere non valido per Base64
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyPEM));
            privateKey = keyFactory.generatePrivate(keySpec);

            String publicKeyRaw = new String(Files.readAllBytes(publicKeyFile.toPath()));
            String realPemPub = publicKeyRaw.contains("\\n") ? publicKeyRaw.replace("\\n", "\n") : publicKeyRaw;
            String publicKeyPEM = realPemPub
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("[^A-Za-z0-9+/=]", "");
            EncodedKeySpec keySpecPub = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyPEM));
            PublicKey publicKey = keyFactory.generatePublic(keySpecPub);

            parser = Jwts.parserBuilder()
                    .setSigningKey(privateKey)
                    .build();

            // JWTAuthContextInfo contextInfo = new JWTAuthContextInfo(publicKey, issuer);
            // Instantiate the parser
            // parser = new DefaultJWTParser(contextInfo);
        } catch (Exception e) {
            throw new ModuleException("Failed to read keys", e);
        }

        // cacheRefreshToken = CacheBuilder.newBuilder()
        //         .expireAfterAccess(refreshTokenExpires, TimeUnit.SECONDS)
        //         .build();
        loginFailsMap = new ConcurrentHashMap<>();
        loginBlockingMap = new ConcurrentHashMap<>();
        loginLock = new ConcurrentHashMap<>();
        authFieldFailsMap = new ConcurrentHashMap<>();
        authFieldBlockingMap = new ConcurrentHashMap<>();
        authFieldLock = new ConcurrentHashMap<>();
    }

    @Override
    public void update(ConfigurationTool configurationTool) throws ModuleException {
        stop(configurationTool);
        start(configurationTool);
    }

    @Override
    public void process(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool) throws ModuleException {
        ModuleUtils.processMessagesAll(configurationTool, executionContextTool, (unused, messagesList) -> {
            Type type = Type.valueOf(executionContextTool.getType().toUpperCase());
            switch (type) {
                case LOGIN:
                    login(configurationTool, executionContextTool, messagesList);
                    break;
                // case LOGOUT:
                //     logout(configurationTool, executionContextTool, messagesList);
                //     break;
                case PARSE:
                    parse(configurationTool, executionContextTool, messagesList);
                    break;
                case REFRESH_TOKENS:
                    refreshToken(configurationTool, executionContextTool, messagesList);
                    break;
                case HAS_ROLE:
                    hasRole(configurationTool, executionContextTool, messagesList);
                    break;
                case GEN_HASH:
                    genHash(configurationTool, executionContextTool, messagesList);
                    break;
                case LOGIN_FIELD:
                    loginField(configurationTool, executionContextTool, messagesList);
                    break;
            }
        });
    }

    @Override
    public void stop(ConfigurationTool configurationTool) throws ModuleException {
        issuer = null;
        accessTokenExpires = null;
        refreshTokenExpires = null;
        bcryptCost = null;
        privateKey = null;
        // publicKey = null;
        // publicKeyRaw = null;
        // privateKeyRaw = null;
        parser = null;
        authSleep = null;
        fieldNames = null;
        // if (cacheRefreshToken != null) {
        //     cacheRefreshToken.invalidateAll();
        //     cacheRefreshToken = null;
        // }
        checkRemoteAddr = null;
        countLoginFailBeforeBlocking = null;
        blockingTime = null;
        plainPass = null;
        tfaCheckFieldName = null;

        loginBlockingMap = null;
        loginFailsMap = null;
        loginLock = null;
        authFieldBlockingMap = null;
        authFieldFailsMap = null;
        authFieldLock = null;
    }

    private void genHash(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, List<LinkedList<IMessage>> messagesList) {
        LinkedList<IMessage> messages = messagesList.get(0);
        String password = ModuleUtils.toString(messages.poll());
        executionContextTool.addMessage(genPasswordHash(password));
    }

    private void hasRole(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, List<LinkedList<IMessage>> messagesList) {
        LinkedList<IMessage> messages = messagesList.get(0);
        ObjectArray objectArray = ModuleUtils.getObjectArray(messages.poll());
        String role = ModuleUtils.toString(messages.poll());
        boolean result = false;
        if (ModuleUtils.isArrayContainObjectElements(objectArray)) {
            ObjectArray array = ((ObjectElement) objectArray.get(0)).findField(CLAIM_NAME_GROUPS).map(ModuleUtils::getObjectArray).orElse(null);
            if (array != null && array.isSimple()) {
                for (int i = 0; i < array.size(); i++) {
                    if (Objects.equals(role, array.get(i).toString())) {
                        result = true;
                        break;
                    }
                }
            }
        }
        executionContextTool.addMessage(result);
    }

    private void refreshToken(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, List<LinkedList<IMessage>> messagesList) {
        LinkedList<IMessage> messages = messagesList.get(0);
        String token = ModuleUtils.toString(messages.poll());
        Claims claims = parseToken(configurationTool, token);
        if (claims == null)
            return;

        Object isATV = claims.get(CLAIM_NAME_IS_AT);
        boolean isAT = isATV instanceof Boolean ? (Boolean) isATV : false;
        if (isAT) {
            executionContextTool.addError("requires refresh token");
            return;
        }

        Map.Entry<String, Boolean> remoteAddrEntry = getOrCheckRemoteAddr(executionContextTool, messagesList, 1, claims);
        if (remoteAddrEntry != null && !remoteAddrEntry.getValue())
            return;

        if (executionContextTool.getFlowControlTool().countManagedExecutionContexts() > 0) {
            configurationTool.loggerTrace(String.format("additional check for %s", claims.getSubject()));
            Boolean additionalCheck = ModuleUtils.executeAndGetMessages(executionContextTool, 0, List.of(Long.parseLong(claims.getSubject()), token))
                    .map(l -> ModuleUtils.toBoolean(l.get(0))).orElse(false);
            if (!additionalCheck) {
                executionContextTool.addError("additional check fail");
                return;
            }
        }

        String accessToken = regenerateToken(claims, accessTokenExpires, true);
        String refreshToken = regenerateToken(claims, refreshTokenExpires, false);

        executionContextTool.addMessage(new ObjectArray(new ObjectElement(
                new ObjectField("accessToken", accessToken),
                new ObjectField("refreshToken", refreshToken))));
    }

    private void parse(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, List<LinkedList<IMessage>> messagesList) throws Exception {
        LinkedList<IMessage> messages = messagesList.get(0);
        String token = ModuleUtils.toString(messages.poll());
        Claims claims = parseToken(configurationTool, token);
        if (claims == null)
            return;

        Object isATV = claims.get(CLAIM_NAME_IS_AT);
        boolean isAT = isATV instanceof Boolean ? (Boolean) isATV : false;
        if (!isAT) {
            executionContextTool.addError("requires access token");
            return;
        }

        Map.Entry<String, Boolean> remoteAddrEntry = getOrCheckRemoteAddr(executionContextTool, messagesList, 1, claims);
        if (remoteAddrEntry != null && !remoteAddrEntry.getValue())
            return;

        ObjectElement objectElement = new ObjectElement(
                new ObjectField("id", Long.parseLong(claims.getSubject())),
                new ObjectField("sub", claims.getSubject()),
                new ObjectField("exp", claims.getExpiration().getTime()));
        Object groupsO = claims.get(CLAIM_NAME_GROUPS);
        if (groupsO != null/* && !groupsO.toString().isBlank()*/)
            objectElement.getFields().add(
                    new ObjectField(CLAIM_NAME_GROUPS, new ObjectArray(
                            new ArrayList<>((Collection<String>) groupsO)/*Arrays.stream(groupsO.toString().split(",")).collect(Collectors.toList())*/,
                            ObjectType.STRING)));
        fieldNames.forEach(fieldName -> {
            Object v = claims.get(fieldName);
            if (v != null)
                objectElement.getFields().add(new ObjectField(fieldName, v.toString()));
        });
        executionContextTool.addMessage(new ObjectArray(objectElement));
    }

    private Map.Entry<String, Boolean> getOrCheckRemoteAddr(ExecutionContextTool executionContextTool, List<LinkedList<IMessage>> messagesList, int sourceId, Claims claims) {
        String remoteAddr = null;
        if (checkRemoteAddr && messagesList.size() > sourceId) {
            LinkedList<IMessage> messagesReq = messagesList.get(sourceId);
            ObjectArray objectArray = ModuleUtils.getObjectArray(messagesReq.poll());
            if (ModuleUtils.isArrayContainObjectElements(objectArray)) {
                ObjectElement objectElement = (ObjectElement) objectArray.get(0);
                remoteAddr = objectElement.findField(CLAIM_NAME_RADDR).map(ModuleUtils::toString).orElse(null);

                if (claims != null) {
                    String remoteAddrT = claims.get(CLAIM_NAME_RADDR).toString();
                    if (remoteAddr != null && !Objects.equals(remoteAddr, remoteAddrT)) {
                        executionContextTool.addError("remoteAddr not valid");
                        return Map.entry(remoteAddr, false);
                    }
                }
            }
        }
        return remoteAddr != null ? Map.entry(remoteAddr, true) : null;
    }

    private void login(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, List<LinkedList<IMessage>> messagesList) {
        LinkedList<IMessage> messages = messagesList.get(0);
        String login = ModuleUtils.toString(messages.poll());
        String password = ModuleUtils.toString(messages.poll());
        Map.Entry<String, Boolean> remoteAddrEntry = getOrCheckRemoteAddr(executionContextTool, messagesList, 1, null);
        String remoteAddr = remoteAddrEntry != null ? remoteAddrEntry.getKey() : null;
        Instant blockingTo = loginBlockingMap.get(login);
        if (blockingTo != null) {
            if (Instant.now().isBefore(blockingTo)) {
                executionContextTool.addError("user blocking");
                return;
            } else {
                loginFailsMap.remove(login);
                loginBlockingMap.remove(login);
            }
        }

        Lock lock = loginLock.computeIfAbsent(login, k -> new ReentrantLock());
        try {
            lock.lock();
            try {
                Thread.sleep(authSleep);
            } catch (Exception ignore) {
            }

            UserDTO userDTO = ModuleUtils.executeAndGetElement(executionContextTool, 0, List.of(login))
                    .map((e) -> {
                        UserDTO userDTOTmp = ModuleUtils.convertFromObjectElement(e, UserDTO.class, true, true);
                        userDTOTmp.setObjectElement(e);
                        if (userDTOTmp.getBlocking() != null) {
                            Instant instant = Instant.ofEpochMilli(userDTOTmp.getBlocking().getTime());
                            if (instant.isAfter(Instant.now()))
                                loginBlockingMap.put(login, instant);
                        }
                        return userDTOTmp;
                    })
                    .orElse(null);
            if (userDTO == null) {
                executionContextTool.addError("user not exists");
                return;
            }
            if (userDTO.getDisabled() != null) {
                executionContextTool.addError("user is disabled");
                return;
            }
            if (loginBlockingMap.containsKey(login)) {
                executionContextTool.addError("user blocking");
                return;
            }
            if (!UserAuthType.LOGIN_PASS.equals(userDTO.getAuthType()) && !UserAuthType.ALL.equals(userDTO.getAuthType())) {
                executionContextTool.addError("auth method not allowed");
                return;
            }
            if (!verifyPassword(password, userDTO.getPassword())) {
                executionContextTool.addError("password incorrect");
                saveFail(configurationTool, executionContextTool, userDTO, userDTO.getLogin(), true);
                return;
            }
            loginEnd(configurationTool, executionContextTool, userDTO, remoteAddr, userDTO.getLogin(), false);
        } finally {
            lock.unlock();
        }
    }

    private void loginField(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, List<LinkedList<IMessage>> messagesList) {
        LinkedList<IMessage> messages = messagesList.get(0);
        String authFieldValue = ModuleUtils.toString(messages.poll());
        // String password = ModuleUtils.toString(messages.poll());
        Map.Entry<String, Boolean> remoteAddrEntry = getOrCheckRemoteAddr(executionContextTool, messagesList, 1, null);
        String remoteAddr = remoteAddrEntry != null ? remoteAddrEntry.getKey() : null;

        Instant blockingTo = authFieldBlockingMap.get(authFieldValue);
        if (blockingTo != null) {
            if (Instant.now().isBefore(blockingTo)) {
                executionContextTool.addError("user blocking");
                return;
            } else {
                authFieldFailsMap.remove(authFieldValue);
                authFieldBlockingMap.remove(authFieldValue);
            }
        }

        Lock lock = authFieldLock.computeIfAbsent(authFieldValue, k -> new ReentrantLock());
        try {
            lock.lock();
            try {
                Thread.sleep(authSleep);
            } catch (Exception ignore) {
            }

            UserDTO userDTO = ModuleUtils.executeAndGetElement(executionContextTool, 0, List.of(authFieldValue))
                    .map((e) -> {
                        UserDTO userDTOTmp = ModuleUtils.convertFromObjectElement(e, UserDTO.class, true, true);
                        userDTOTmp.setObjectElement(e);
                        if (userDTOTmp.getBlocking() != null) {
                            Instant instant = Instant.ofEpochMilli(userDTOTmp.getBlocking().getTime());
                            if (instant.isAfter(Instant.now()))
                                authFieldBlockingMap.put(authFieldValue, instant);
                        }
                        return userDTOTmp;
                    })
                    .orElse(null);
            if (userDTO == null) {
                executionContextTool.addError("user not exists");
                return;
            }
            if (userDTO.getDisabled() != null) {
                executionContextTool.addError("user is disabled");
                return;
            }
            if (authFieldBlockingMap.containsKey(authFieldValue)) {
                executionContextTool.addError("user blocking");
                return;
            }
            if (!UserAuthType.FIELD.equals(userDTO.getAuthType()) && !UserAuthType.ALL.equals(userDTO.getAuthType())) {
                executionContextTool.addError("auth method not allowed");
                return;
            }
            loginEnd(configurationTool, executionContextTool, userDTO, remoteAddr, authFieldValue, true);
        } finally {
            lock.unlock();
        }

    }

    private void loginEnd(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, UserDTO userDTO, String remoteAddr, String authValue, boolean isLogin) {
        List<RoleDTO> roleDTOS = ModuleUtils.executeAndGetObjects(executionContextTool, 1, List.of(userDTO.getId()), RoleDTO.class, true).orElse(List.of());

        if (executionContextTool.getFlowControlTool().countManagedExecutionContexts() > 2 && !tfaCheckFieldName.isBlank() &&
                userDTO.getObjectElement().findFieldIgnoreCase(tfaCheckFieldName).map(ModuleUtils::toBoolean).orElse(false)) {
            configurationTool.loggerTrace(String.format("tfa check for %s", userDTO.getLogin()));
            Boolean additionalCheck = ModuleUtils.executeAndGetMessages(executionContextTool, 2, List.of(userDTO.getObjectElement()))
                    .map(l -> ModuleUtils.toBoolean(l.get(0))).orElse(false);
            if (!additionalCheck) {
                executionContextTool.addError("tfa check fail");
                saveFail(configurationTool, executionContextTool, userDTO, authValue, isLogin);
                return;
            }
        }

        // String accessToken = generateToken(userDTO, roleDTOS, accessTokenExpires, true, null);
        String refreshToken = generateToken(userDTO, roleDTOS, refreshTokenExpires, remoteAddr);
        Claims claims = parseToken(configurationTool, refreshToken);
        String accessToken = regenerateToken(claims, accessTokenExpires, true);

        if (executionContextTool.getFlowControlTool().countManagedExecutionContexts() > 4) {
            configurationTool.loggerTrace(String.format("save new refresh token for %s", userDTO.getLogin()));
            if (ModuleUtils.executeAndGetFirstMessage(executionContextTool, 4, List.of(userDTO.getId(), refreshToken)).isEmpty())
                configurationTool.loggerWarn("save new refresh fail");
        }

        // cacheRefreshToken.put(refreshToken, Map.entry(userDTO, roleDTOS));
        configurationTool.loggerDebug(String.format("login %s", userDTO.getLogin()));

        executionContextTool.addMessage(new ObjectArray(new ObjectElement(
                new ObjectField("accessToken", accessToken),
                new ObjectField("refreshToken", refreshToken))));
    }

    private void saveFail(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, UserDTO userDTO, String authValue, boolean isLogin) {
        int authFails = (isLogin ? loginFailsMap : authFieldFailsMap).compute(authValue, (k, v) -> {
            if (v == null) {
                v = 1;
            } else {
                v++;
            }
            return v;
        });
        if (authFails >= countLoginFailBeforeBlocking) {
            configurationTool.loggerInfo(String.format("blocking user %s", userDTO.getLogin()));
            Instant blocking = Instant.now().plus(blockingTime, ChronoUnit.SECONDS);
            (isLogin ? loginBlockingMap : authFieldBlockingMap).put(authValue, blocking);
            if (executionContextTool.getFlowControlTool().countManagedExecutionContexts() > 3) {
                if (ModuleUtils.executeAndGetFirstMessage(executionContextTool, 3, List.of(userDTO.getId(), blocking.toEpochMilli())).isEmpty())
                    configurationTool.loggerWarn("blocking user in external storage fail");
            }
        }
    }

    // private void logout(ConfigurationTool configurationTool, ExecutionContextTool executionContextTool, List<LinkedList<IMessage>> messagesList) {
    //     LinkedList<IMessage> messages = messagesList.get(0);
    //     String token = ModuleUtils.toString(messages.poll());
    //     Claims claims = parseToken(token);
    //     // Jwts.builder().setClaims(claims)
    //     // Map.Entry<UserDTO, List<RoleDTO>> entry = cacheRefreshToken.getIfPresent(refreshToken);
    //     if (claims != null) {
    //         // cacheRefreshToken.invalidate(refreshToken);
    //         configurationTool.loggerDebug(String.format("logout %s", /*entry.getKey().getLogin()*/claims.getSubject()));
    //
    //         //invalidate refreshToken in external repository
    //         if (executionContextTool.getFlowControlTool().countManagedExecutionContexts() > 0)
    //             executionContextTool.getFlowControlTool().executeNow(CommandType.EXECUTE, 0, List.of(claims.getSubject()/*entry.getKey().getId(), entry.getKey().getLogin()*/));
    //     }
    // }

    private String generateToken(UserDTO userDTO, List<RoleDTO> roleDTOS, int tokenExpires, String remoteAddr) {
        try {
            /*
            JwtClaimsBuilder builder = Jwt.issuer(issuer)
                    .subject(userDTO.getId().toString())
                    .expiresIn(accessTokenExpires);
            */

            TimeUnit unit = TimeUnit.SECONDS;
            Date now = new Date();
            Date expirationDate = new Date(now.getTime() + unit.toMillis(tokenExpires));

            JwtBuilder builder = Jwts.builder()
                    .setIssuer(issuer)
                    .setSubject(userDTO.getId().toString()) // The principal in the token
                    .setIssuedAt(now)    // When the token was issued
                    .setExpiration(expirationDate);// When the token expires

            // if (upn != null && !upn.isBlank())
            //     builder.upn(upn);

            if (remoteAddr != null)
                builder.addClaims(Map.of(CLAIM_NAME_RADDR, remoteAddr));

            Set<String> groups = roleDTOS != null && !roleDTOS.isEmpty() ?
                    roleDTOS.stream().map(RoleDTO::getName).collect(Collectors.toSet()) :
                    null;
            if (groups != null && !groups.isEmpty()) {
                // builder.groups(groups);
                builder.addClaims(Map.of(CLAIM_NAME_GROUPS, groups/*String.join(",", groups)*/));
            }

            Map<String, Object> claims = !fieldNames.isEmpty() ?
                    ModuleUtils.findFields(new ObjectArray(userDTO.getObjectElement()), fieldNames).stream()
                            .filter(l -> !l.isEmpty())
                            .collect(Collectors.toMap(l -> l.get(0).getName(), l -> l.get(0).getValue())) :
                    null;
            if (claims != null)
                claims.forEach((k, v) -> builder.claim(k, v.toString()));
            builder.claim(CLAIM_NAME_IS_AT, false);
            // return builder.sign(privateKey);
            return builder
                    .signWith(privateKey) // Sign with the key and algorithm
                    .compact(); // Build and serialize to a compact, URL-safe string
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign JWT token", e);
        }
    }

    private String regenerateToken(Claims claims, int tokenExpires, boolean isAccessToken) {
        try {
            TimeUnit unit = TimeUnit.SECONDS;
            Date now = new Date();
            Date expirationDate = isAccessToken || claims.getExpiration() == null ? new Date(now.getTime() + unit.toMillis(tokenExpires)) : claims.getExpiration();

            JwtBuilder builder = Jwts.builder()
                    .setClaims(new DefaultClaims(claims))
                    .setIssuedAt(now)    // When the token was issued
                    .setExpiration(expirationDate);// When the token expires
            builder.claim(CLAIM_NAME_IS_AT, isAccessToken);
            return builder
                    .signWith(privateKey) // Sign with the key and algorithm
                    .compact(); // Build and serialize to a compact, URL-safe string
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign JWT token", e);
        }
    }

    /*
    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    */

    /*
    private JsonWebToken parseToken(String token, ExecutionContextTool executionContextTool) throws Exception {
        if (token != null && token.startsWith("Bearer "))
            token = token.substring(7);
        JsonWebToken jwt = parser.parse(token);
        if (jwt == null) {
            executionContextTool.addError("wrong token");
            return jwt;
        }
        if (jwt.getExpirationTime() < (System.currentTimeMillis() / 1000)) {
            executionContextTool.addError("token expiration time");
            return null;
        }
        return jwt;
    }
    */

    /**
     * Validates and parses a JWT token.
     *
     * @param token The JWT string to validate.
     * @return The claims (payload) if valid.
     * @throws io.jsonwebtoken.JwtException if the token is invalid or expired.
     */
    private Claims parseToken(ConfigurationTool configurationTool, String token) {
        if (token == null || token.isBlank())
            return null;
        if (token.startsWith("Bearer "))
            token = token.substring(7);
        try {
            Jws<Claims> claimsJws = parser.parseClaimsJws(token);
            return claimsJws.getBody();
        } catch (Exception e) {
            configurationTool.loggerTrace("parse error " + e.getMessage());
            return null;
        }
    }

    private boolean verifyPassword(String password, Object passHash) {
        if (passHash instanceof byte[])
            return BCrypt.verifyer().verify(password.toCharArray(), (byte[]) passHash).verified;
        String passHashStr = passHash.toString();
        if (!plainPass.isBlank() && passHashStr.startsWith(plainPass))
            return true;
        return BCrypt.verifyer().verify(password.toCharArray(), passHashStr).verified;
    }

    private String genPasswordHash(String password) {
        return BCrypt.withDefaults().hashToString(bcryptCost, password.toCharArray());
    }

    private enum Type {
        LOGIN/*, LOGOUT*/, PARSE, REFRESH_TOKENS, HAS_ROLE, GEN_HASH, LOGIN_FIELD
    }

}

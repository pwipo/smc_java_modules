package ru.smcsystem.smcmodules.module;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
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
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class DB implements Module {

    private enum Type {
        derbyInMemory("org.apache.derby.jdbc.EmbeddedDriver", "jdbc:derby:memory:%s;create=true", "jdbc:derby:memory:%s", "values 1"),
        derby("org.apache.derby.jdbc.EmbeddedDriver", "jdbc:derby:%s;create=true", "jdbc:derby:%s", "values 1"),
        postgreeClient("org.postgresql.Driver", null, "jdbc:postgresql://%s", "select 1"),
        mysqlClient("com.mysql.jdbc.Driver", null, "jdbc:mysql://%s", "select 1"),
        oracleClient("oracle.jdbc.driver.OracleDriver", null, "jdbc:oracle:thin:%s", "select 1 from dual"),
        db2Client("com.ibm.db2.jcc.DB2Driver", null, "jdbc:db2:%s", "select 1 from sysibm.sysdummy1");

        final String driver;
        final String urlStart;
        final String urlPattern;
        final String validationQuery;

        Type(String driver, String urlStart, String urlPattern, String validationQuery) {
            this.driver = driver;
            this.urlStart = urlStart;
            this.urlPattern = urlPattern;
            this.validationQuery = validationQuery;
        }
    }

    private Type type;
    private String connection_params;
    private String login;
    private String password;
    private boolean useAutoConvert;

    private BasicDataSource dataSource;

    private Map<Long, Connection> connections;
    private AtomicLong connectionId;
    private String jdbc_url;

    private enum ResultFormat {
        OBJECT_SERIALIZATION,
        OBJECT,
        OBJECT_WITHOUT_NULL,
    }

    private ResultFormat resultFormat;

    private enum OperationType {
        EXECUTE_IN_ONE_TRANSACTION,
        BEGIN_EXTERNAL_TRANSACTION,
        END_AND_COMMIT_EXTERNAL_TRANSACTION,
        END_AND_ROLLBACK_EXTERNAL_TRANSACTION,
        EXECUTE_IN_EXTERNAL_TRANSACTION,
        EXECUTE_EACH_IN_OWN_TRANSACTION,
        EXECUTE_WITH_PARAMS_IN_ONE_TRANSACTION,
        EXECUTE_WITH_PARAMS_IN_EXTERNAL_TRANSACTION,
        EXECUTE_WITH_PARAMS_ARRAY_IN_ONE_TRANSACTION,
        EXECUTE_WITH_PARAMS_ARRAY_IN_EXTERNAL_TRANSACTION,
        EXECUTE_UPDATE_IN_ONE_TRANSACTION_RETURN_GENERATED_KEY,
        EXECUTE_UPDATE_IN_EXTERNAL_TRANSACTION_RETURN_GENERATED_KEY,
        EXECUTE_UPDATE_EACH_IN_OWN_TRANSACTION_RETURN_GENERATED_KEY,
        EXECUTE_UPDATE_WITH_PARAMS_IN_ONE_TRANSACTION_RETURN_GENERATED_KEY,
        EXECUTE_UPDATE_WITH_PARAMS_IN_EXTERNAL_TRANSACTION_RETURN_GENERATED_KEY,
        EXECUTE_UPDATE_WITH_PARAMS_ARRAY_IN_ONE_TRANSACTION_RETURN_GENERATED_KEY,
        EXECUTE_UPDATE_WITH_PARAMS_ARRAY_IN_EXTERNAL_TRANSACTION_RETURN_GENERATED_KEY,
    }

    private Boolean resultSetColumnNameToUpperCase;
    private Integer queryTimeout;

    @Override
    public void start(ConfigurationTool externalConfigurationTool) throws ModuleException {
        type = Type.valueOf((String) externalConfigurationTool.getSetting("type").orElseThrow(() -> new ModuleException("type setting")).getValue());
        connection_params = (String) externalConfigurationTool.getSetting("connection_params").orElseThrow(() -> new ModuleException("connection_params setting")).getValue();
        login = (String) externalConfigurationTool.getSetting("login").orElseThrow(() -> new ModuleException("login setting")).getValue();
        if (StringUtils.equals(login, " "))
            login = "";
        password = (String) externalConfigurationTool.getSetting("password").orElseThrow(() -> new ModuleException("password setting")).getValue();
        if (StringUtils.equals(password, " "))
            password = "";
        useAutoConvert = Boolean.parseBoolean((String) externalConfigurationTool.getSetting("useAutoConvert").orElseThrow(() -> new ModuleException("useAutoConvert setting")).getValue());
        resultFormat = ResultFormat.valueOf((String) externalConfigurationTool.getSetting("resultFormat").orElseThrow(() -> new ModuleException("resultFormat setting")).getValue());
        resultSetColumnNameToUpperCase = Boolean.parseBoolean((String) externalConfigurationTool.getSetting("resultSetColumnNameToUpperCase").orElseThrow(() -> new ModuleException("resultSetColumnNameToUpperCase setting")).getValue());
        queryTimeout = (Integer) externalConfigurationTool.getSetting("queryTimeout").orElseThrow(() -> new ModuleException("queryTimeout setting")).getValue();

        String dbParam = connection_params;
        if (Type.derby.equals(type)/* || Type.derbyInMemory.equals(type)*/) {
            dbParam = new File(externalConfigurationTool.getWorkDirectory(), connection_params).getAbsolutePath();
        }
        jdbc_url = String.format(type.urlPattern, dbParam);

        if (StringUtils.isNotBlank(type.urlStart)) {
            try {
                DriverManager.getConnection(String.format(type.urlStart, dbParam));
            } catch (SQLException e) {
                throw new ModuleException("urlStart", e);
            }
        }

        // System.setProperty("derby.system.home", externalConfigurationTool.getWorkDirectory());
        BasicDataSource ds = new BasicDataSource();
        ds.setDriverClassName(type.driver);
        ds.setUrl(jdbc_url);
        ds.setUsername(login);
        ds.setPassword(password);
        ds.setValidationQuery(type.validationQuery);
        // ds.setMaxTotal(env.getProperty(JDBC_MAX_TOTAL, Integer.class, 10));
        ds.setDefaultAutoCommit(false);
        dataSource = ds;

        /*
        if (Type.derbyInMemoryDump.equals(type)) {
            File file = new File(externalConfigurationTool.getWorkDirectory() + File.separator + "dump");
            List<String> tableNames = new ArrayList<>();
            if (file.exists() && file.listFiles() != null)
                Arrays.stream(file.listFiles()).forEach(f -> tableNames.add(f.getPath()));

            try (Connection connection = dataSource.getConnection()) {
                try {
                    tableNames.forEach(name -> {
                        try (Statement st = connection.createStatement(); PreparedStatement ps = connection.prepareStatement(
                                "CALL SYSCS_UTIL.SYSCS_IMPORT_TABLE(?,?,?,?,?,?,?)")) {
                            ps.setString(1, null);
                            ps.setString(2, FilenameUtils.getBaseName(name));
                            ps.setString(3, name);
                            ps.setString(4, "%");
                            ps.setString(5, null);
                            ps.setString(6, null);
                            ps.setInt(7, 0);
                            ps.execute();
                        } catch (Exception e) {
                            throw new ModuleException("dump import", e);
                        }
                    });
                    connection.commit();
                } catch (Exception e) {
                    try {
                        connection.rollback();
                    } catch (SQLException e1) {
                        ModuleException exception = new ModuleException("dump import", e1);
                        exception.addSuppressed(e);
                        throw exception;
                    }
                    throw new ModuleException("dump import", e);
                }
            } catch (SQLException e) {
                throw new ModuleException("dump import", e);
            }
        }
        */

        File install = new File(externalConfigurationTool.getWorkDirectory(), "install.sql");
        if (install.exists() && install.isFile()) {
            try (Connection connection = dataSource.getConnection()) {
                try {
                    connection.setAutoCommit(false);
                    executeSqlScript(connection, install);
                    connection.commit();
                } catch (Exception e) {
                    try {
                        connection.rollback();
                    } catch (SQLException e1) {
                        ModuleException exception = new ModuleException("install.sql: " + e1.getMessage(), e1);
                        exception.addSuppressed(e);
                        throw exception;
                    }
                    throw new ModuleException("install.sql: " + e.getMessage(), e);
                }
            } catch (Exception e) {
                // throw new ModuleException("error", e);
                externalConfigurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
            }
        }

        connectionId = new AtomicLong(0);
        connections = new HashMap<>();
    }

    @Override
    public void update(ConfigurationTool externalConfigurationTool) throws ModuleException {
        stop(externalConfigurationTool);
        start(externalConfigurationTool);
    }

    @Override
    public void process(ConfigurationTool externalConfigurationTool, ExecutionContextTool externalExecutionContextTool) throws ModuleException {
        ModuleUtils.processMessages(externalConfigurationTool, externalExecutionContextTool, (id, messages) -> {
            OperationType operationType = OperationType.values()[ModuleUtils.getNumber(messages.poll()).intValue() - 1];
            switch (operationType) {
                case EXECUTE_IN_ONE_TRANSACTION:
                    execute(externalConfigurationTool, externalExecutionContextTool, messages, true, false);
                    break;
                case BEGIN_EXTERNAL_TRANSACTION:
                    startConnectionWithTransaction(externalExecutionContextTool);
                    break;
                case END_AND_COMMIT_EXTERNAL_TRANSACTION:
                    commitAndStopConnection(externalConfigurationTool, externalExecutionContextTool, ModuleUtils.getNumber(messages.poll()).longValue());
                    break;
                case END_AND_ROLLBACK_EXTERNAL_TRANSACTION:
                    rollbackAndStopConnection(externalConfigurationTool, externalExecutionContextTool, ModuleUtils.getNumber(messages.poll()).longValue());
                    break;
                case EXECUTE_IN_EXTERNAL_TRANSACTION:
                    executeInTransaction(externalConfigurationTool, externalExecutionContextTool, ModuleUtils.getNumber(messages.poll()).longValue(), messages, false);
                    break;
                case EXECUTE_EACH_IN_OWN_TRANSACTION:
                    execute(externalConfigurationTool, externalExecutionContextTool, messages, false, false);
                    break;
                case EXECUTE_WITH_PARAMS_IN_ONE_TRANSACTION:
                    executePreparedStatement(externalConfigurationTool, externalExecutionContextTool, messages, false, false);
                    break;
                case EXECUTE_WITH_PARAMS_IN_EXTERNAL_TRANSACTION:
                    executePreparedStatementInTransaction(externalConfigurationTool, externalExecutionContextTool, ModuleUtils.getNumber(messages.poll()).longValue(), messages, false, false);
                    break;
                case EXECUTE_WITH_PARAMS_ARRAY_IN_ONE_TRANSACTION:
                    executePreparedStatement(externalConfigurationTool, externalExecutionContextTool, messages, true, false);
                    break;
                case EXECUTE_WITH_PARAMS_ARRAY_IN_EXTERNAL_TRANSACTION:
                    executePreparedStatementInTransaction(externalConfigurationTool, externalExecutionContextTool, ModuleUtils.getNumber(messages.poll()).longValue(), messages, true, false);
                    break;
                case EXECUTE_UPDATE_IN_ONE_TRANSACTION_RETURN_GENERATED_KEY:
                    execute(externalConfigurationTool, externalExecutionContextTool, messages, true, true);
                    break;
                case EXECUTE_UPDATE_IN_EXTERNAL_TRANSACTION_RETURN_GENERATED_KEY:
                    executeInTransaction(externalConfigurationTool, externalExecutionContextTool, ModuleUtils.getNumber(messages.poll()).longValue(), messages, true);
                    break;
                case EXECUTE_UPDATE_EACH_IN_OWN_TRANSACTION_RETURN_GENERATED_KEY:
                    execute(externalConfigurationTool, externalExecutionContextTool, messages, false, true);
                    break;
                case EXECUTE_UPDATE_WITH_PARAMS_IN_ONE_TRANSACTION_RETURN_GENERATED_KEY:
                    executePreparedStatement(externalConfigurationTool, externalExecutionContextTool, messages, false, true);
                    break;
                case EXECUTE_UPDATE_WITH_PARAMS_IN_EXTERNAL_TRANSACTION_RETURN_GENERATED_KEY:
                    executePreparedStatementInTransaction(externalConfigurationTool, externalExecutionContextTool, ModuleUtils.getNumber(messages.poll()).longValue(), messages, false, true);
                    break;
                case EXECUTE_UPDATE_WITH_PARAMS_ARRAY_IN_ONE_TRANSACTION_RETURN_GENERATED_KEY:
                    executePreparedStatement(externalConfigurationTool, externalExecutionContextTool, messages, true, true);
                    break;
                case EXECUTE_UPDATE_WITH_PARAMS_ARRAY_IN_EXTERNAL_TRANSACTION_RETURN_GENERATED_KEY:
                    executePreparedStatementInTransaction(externalConfigurationTool, externalExecutionContextTool, ModuleUtils.getNumber(messages.poll()).longValue(), messages, true, true);
                    break;
            }
        });
    }

    @Override
    public void stop(ConfigurationTool externalConfigurationTool) throws ModuleException {
        ModuleException exception = null;
        /*
        if (Type.derbyInMemoryDump.equals(type)) {
            try (Connection conn = dataSource.getConnection()) {
                DatabaseMetaData md = conn.getMetaData();
                ResultSet rs = md.getTables(null, null, "%", new String[]{"TABLE"});
                List<String> tableNames = new ArrayList<>();
                while (rs.next())
                    tableNames.add(rs.getString(3));

                File file = new File(externalConfigurationTool.getWorkDirectory() + File.separator + "dump");
                if(!file.exists())
                    file.mkdirs();
                if (file.exists() && file.listFiles() != null)
                    Arrays.stream(file.listFiles()).forEach(File::delete);

                tableNames.forEach(name -> {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "CALL SYSCS_UTIL.SYSCS_EXPORT_TABLE (?,?,?,?,?,?)")) {
                        File file1 = new File(file, name + ".dat");
                        if(file1.exists())
                            file1.delete();
                        // file1.createNewFile();
                        ps.setString(1, null);
                        ps.setString(2, name);
                        ps.setString(3, file1.getPath());
                        ps.setString(4, "%");
                        ps.setString(5, null);
                        ps.setString(6, null);
                        ps.execute();
                    } catch (Exception e) {
                        throw new ModuleException("dump", e);
                    }
                });
            } catch (Exception e) {
                exception = new ModuleException("dump", e);
            }
        }
        */
        if (Type.derbyInMemory.equals(type)/* || Type.derby.equals(type)*/) {
            try {
                DriverManager.getConnection(jdbc_url + ";drop=true");
            } catch (Exception e) {
                if (!(e instanceof SQLException) || (!"08006".equals(((SQLException) e).getSQLState())))
                    exception = new ModuleException("dump", e);
            }
        }
        try {
            dataSource.close();
        } catch (Exception e) {
            exception = new ModuleException("dump", e);
        } finally {
            dataSource = null;
        }

        type = null;
        connection_params = null;
        login = null;
        password = null;
        jdbc_url = null;
        if (exception != null)
            throw exception;
    }

    private void execute(ConfigurationTool externalConfigurationTool, ExecutionContextTool externalExecutionContextTool, List<IMessage> messages, boolean useTransaction, boolean isUpdateReturnKeys) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(!useTransaction);
            for (int i = 0; i < messages.size(); i++) {
                IMessage message = messages.get(i);
                try (Statement stm = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
                    stm.setQueryTimeout(queryTimeout);
                    if (isUpdateReturnKeys) {
                        stm.executeUpdate((String) message.getValue(), Statement.RETURN_GENERATED_KEYS);
                    } else {
                        stm.execute((String) message.getValue());
                    }
                    printStmResult(externalConfigurationTool, externalExecutionContextTool, stm, isUpdateReturnKeys);
                } catch (Exception e) {
                    // e.printStackTrace();
                    if (useTransaction) {
                        // try {
                        connection.rollback();
                        /*
                        } catch (SQLException e1) {
                            RuntimeException exception = new RuntimeException(e1);
                            exception.addSuppressed(e);
                            throw exception;
                        }
                        */
                    }
                    throw new ModuleException(String.format("%s: %d %s", e.getMessage(), i, message.getValue()), e);
                }
            }
            if (useTransaction) {
                connection.commit();
            }
        }
    }

    private void executePreparedStatement(ConfigurationTool externalConfigurationTool, ExecutionContextTool externalExecutionContextTool, LinkedList<IMessage> messages, boolean isArray, boolean isUpdateReturnKeys) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            executePreparedStatement(externalConfigurationTool, externalExecutionContextTool, messages, connection, true, isArray, isUpdateReturnKeys);
            connection.commit();
        }
    }

    private void executePreparedStatement(ConfigurationTool externalConfigurationTool, ExecutionContextTool externalExecutionContextTool, LinkedList<IMessage> messages, Connection connection, boolean needRollback, boolean isArray, boolean isUpdateReturnKeys) throws Exception {
        int elementId = 0;
        while (!messages.isEmpty()) {
            String sql = (String) messages.poll().getValue();
            if (!isArray) {
                try (PreparedStatement stm = isUpdateReturnKeys ? connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS) : connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
                    stm.setQueryTimeout(queryTimeout);
                    ParameterMetaData parameterMetaData = stm.getParameterMetaData();
                    int parameterCount = parameterMetaData.getParameterCount();
                    if ((messages.size() < parameterCount) || (useAutoConvert && type == Type.mysqlClient && messages.size() < parameterCount * 2)) {
                        externalExecutionContextTool.addError("need " + parameterCount + " params");
                        return;
                    }
                    for (int i = 1; i <= parameterCount; i++) {
                        IMessage message = messages.poll();
                        Object value = message.getValue();
                        if (useAutoConvert) {
                            int parameterType;
                            if (type == Type.mysqlClient) {
                                parameterType = ModuleUtils.getNumber(messages.poll()).intValue();
                            } else {
                                parameterType = parameterMetaData.getParameterType(i);
                            }
                            if ("NULL".equals(value)) {
                                stm.setNull(i, parameterType);
                            } else if (value instanceof ObjectArray) {
                                ObjectArray objectArrayValue = (ObjectArray) value;
                                if (objectArrayValue.isSimple()) {
                                    Array array;
                                    if (objectArrayValue.getType() != ObjectType.VALUE_ANY && ModuleUtils.isArrayContainNumber(objectArrayValue)) {
                                        if (objectArrayValue.getType() == ObjectType.FLOAT || objectArrayValue.getType() == ObjectType.DOUBLE || objectArrayValue.getType() == ObjectType.BIG_DECIMAL) {
                                            List<Float> lst = new ArrayList<>(objectArrayValue.size());
                                            for (int k = 0; k < objectArrayValue.size(); k++)
                                                lst.add(((Number) objectArrayValue.get(k)).floatValue());
                                            array = stm.getConnection().createArrayOf("FLOAT", lst.toArray());
                                        } else {
                                            List<Long> lst = new ArrayList<>(objectArrayValue.size());
                                            for (int k = 0; k < objectArrayValue.size(); k++)
                                                lst.add(((Number) objectArrayValue.get(k)).longValue());
                                            array = stm.getConnection().createArrayOf("BIGINT", lst.toArray());
                                        }
                                    } else {
                                        List<String> lst = new ArrayList<>(objectArrayValue.size());
                                        for (int k = 0; k < objectArrayValue.size(); k++)
                                            lst.add(objectArrayValue.get(k).toString());
                                        array = stm.getConnection().createArrayOf("VARCHAR", lst.toArray());
                                    }
                                    stm.setArray(i, array);
                                } else {
                                    stm.setNull(i, parameterType);
                                }
                            } else {
                                switch (parameterType) {
                                    case Types.BOOLEAN:
                                    case Types.BIT:
                                        value = toBoolean(message);
                                        break;
                                    case Types.TIMESTAMP:
                                        value = !ModuleUtils.isString(message) || NumberUtils.isCreatable(ModuleUtils.getString(message)) ? new Timestamp(toLong(message)) : message.getValue();
                                        break;
                                    case Types.DATE:
                                        value = !ModuleUtils.isString(message) || NumberUtils.isCreatable(ModuleUtils.getString(message)) ? new java.sql.Date(toLong(message)) : message.getValue();
                                        break;
                                    case Types.TIME:
                                        value = !ModuleUtils.isString(message) || NumberUtils.isCreatable(ModuleUtils.getString(message)) ? new Time(toLong(message)) : message.getValue();
                                        break;
                                }
                                stm.setObject(i, value);
                            }
                            // System.out.println(i + " " + parameterType + " " + value);
                        } else {
                            stm.setObject(i, value);
                        }
                    }
                    if (isUpdateReturnKeys) {
                        stm.executeUpdate();
                    } else {
                        stm.execute();
                    }
                    printStmResult(externalConfigurationTool, externalExecutionContextTool, stm, isUpdateReturnKeys);
                } catch (Exception e) {
                    if (needRollback)
                        connection.rollback();
                    throw new ModuleException(String.format("%s: %d %s", e.getMessage(), elementId, sql), e);
                }
            } else {
                ObjectArray objectArray = ModuleUtils.deserializeToObject(messages);
                if (ModuleUtils.isArrayContainObjectElements(objectArray)) {
                    String fields = ModuleUtils.getString(messages.peek());
                    List<String> fieldNames;
                    if (fields != null) {
                        messages.poll();
                        fieldNames = Arrays.stream(fields.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isBlank())
                                .collect(Collectors.toList());
                    } else {
                        ObjectElement objectElement = (ObjectElement) objectArray.get(0);
                        fieldNames = objectElement.getFields().stream().map(ObjectField::getName).collect(Collectors.toList());
                    }
                    List<Integer> parameterTypes = List.of();
                    if (useAutoConvert && type == Type.mysqlClient) {
                        ObjectArray objectArrayTypes = ModuleUtils.deserializeToObject(messages);
                        if (objectArrayTypes.size() == 0 || !objectArrayTypes.isSimple()) {
                            externalExecutionContextTool.addError("need param types");
                            return;
                        }
                        parameterTypes = new ArrayList<>(objectArrayTypes.size());
                        for (int i = 0; i < objectArrayTypes.size(); i++)
                            parameterTypes.add(((Number) objectArrayTypes.get(i)).intValue());
                    }
                    for (int i = 0; i < objectArray.size(); i++) {
                        ObjectElement objectElement = (ObjectElement) objectArray.get(i);
                        if (objectElement.getFields().isEmpty())
                            continue;
                        try (PreparedStatement stm = isUpdateReturnKeys ? connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS) : connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
                            stm.setQueryTimeout(queryTimeout);
                            ParameterMetaData parameterMetaData = stm.getParameterMetaData();
                            int parameterCount = parameterMetaData.getParameterCount();
                            if (objectElement.getFields().size() < parameterCount || objectElement.getFields().size() < fieldNames.size() || (useAutoConvert && type == Type.mysqlClient && parameterTypes.size() < parameterCount)) {
                                externalExecutionContextTool.addError("need " + parameterCount + " params");
                                return;
                            }
                            for (int j = 1; j <= parameterCount; j++) {
                                int jj = j - 1;
                                ObjectField f = objectElement.findField(fieldNames.get(jj)).orElseThrow(() -> new NoSuchElementException(fieldNames.get(jj)));
                                if (useAutoConvert) {
                                    int parameterType;
                                    if (type == Type.mysqlClient) {
                                        parameterType = parameterTypes.get(jj);
                                    } else {
                                        parameterType = parameterMetaData.getParameterType(j);
                                    }
                                    // String parameterClassName = parameterMetaData.getParameterClassName(j);
                                    Object value = f.getValue();
                                    if (ModuleUtils.isString(f) && ModuleUtils.getString(f).equals("NULL")) {
                                        stm.setNull(j, parameterType);
                                        // System.out.println(f);
                                    } else if (ModuleUtils.isObjectArray(f)) {
                                        ObjectArray objectArrayValue = ModuleUtils.getObjectArray(f);
                                        if (objectArrayValue.isSimple()) {
                                            Array array;
                                            if (objectArrayValue.getType() != ObjectType.VALUE_ANY && ModuleUtils.isArrayContainNumber(objectArrayValue)) {
                                                if (objectArrayValue.getType() == ObjectType.FLOAT || objectArrayValue.getType() == ObjectType.DOUBLE || objectArrayValue.getType() == ObjectType.BIG_DECIMAL) {
                                                    List<Float> lst = new ArrayList<>(objectArrayValue.size());
                                                    for (int k = 0; k < objectArrayValue.size(); k++)
                                                        lst.add(((Number) objectArrayValue.get(k)).floatValue());
                                                    array = stm.getConnection().createArrayOf("FLOAT", lst.toArray());
                                                } else {
                                                    List<Long> lst = new ArrayList<>(objectArrayValue.size());
                                                    for (int k = 0; k < objectArrayValue.size(); k++)
                                                        lst.add(((Number) objectArrayValue.get(k)).longValue());
                                                    array = stm.getConnection().createArrayOf("BIGINT", lst.toArray());
                                                }
                                            } else {
                                                List<String> lst = new ArrayList<>(objectArrayValue.size());
                                                for (int k = 0; k < objectArrayValue.size(); k++)
                                                    lst.add(objectArrayValue.get(k).toString());
                                                array = stm.getConnection().createArrayOf("VARCHAR", lst.toArray());
                                            }
                                            stm.setArray(j, array);
                                        } else {
                                            stm.setNull(j, parameterType);
                                        }
                                    } else {
                                        switch (parameterType) {
                                            case Types.BOOLEAN:
                                                value = toBoolean(f);
                                                break;
                                            case Types.TIMESTAMP:
                                                value = !ModuleUtils.isString(f) || NumberUtils.isCreatable(ModuleUtils.getString(f)) ? new Timestamp(toLong(f)) : f.getValue();
                                                break;
                                            case Types.DATE:
                                                value = !ModuleUtils.isString(f) || NumberUtils.isCreatable(ModuleUtils.getString(f)) ? new java.sql.Date(toLong(f)) : f.getValue();
                                                break;
                                            case Types.TIME:
                                                value = !ModuleUtils.isString(f) || NumberUtils.isCreatable(ModuleUtils.getString(f)) ? new Time(toLong(f)) : f.getValue();
                                                break;
                                        }
                                        stm.setObject(j, value);
                                        // System.out.println(j + " " + f.getName() + " " + parameterType + " " + parameterClassName + " " + value);
                                    }
                                } else {
                                    stm.setObject(j, f.getValue());
                                }
                            }
                            if (isUpdateReturnKeys) {
                                stm.executeUpdate();
                            } else {
                                stm.execute();
                            }
                            printStmResult(externalConfigurationTool, externalExecutionContextTool, stm, isUpdateReturnKeys);
                        } catch (Exception e) {
                            if (needRollback)
                                connection.rollback();
                            throw new ModuleException(String.format("%s: %d %s", e.getMessage(), elementId, sql), e);
                        }
                        if (externalExecutionContextTool.isNeedStop()) {
                            if (needRollback)
                                connection.rollback();
                            break;
                        }
                    }
                } else if (ModuleUtils.isArrayContainArrays(objectArray)) {
                    List<Integer> parameterTypes = List.of();
                    if (useAutoConvert && type == Type.mysqlClient) {
                        ObjectArray objectArrayTypes = ModuleUtils.deserializeToObject(messages);
                        if (objectArrayTypes.size() == 0 || !objectArrayTypes.isSimple()) {
                            externalExecutionContextTool.addError("need param types");
                            return;
                        }
                        parameterTypes = new ArrayList<>(objectArrayTypes.size());
                        for (int i = 0; i < objectArrayTypes.size(); i++)
                            parameterTypes.add(((Number) objectArrayTypes.get(i)).intValue());
                    }
                    for (int i = 0; i < objectArray.size(); i++) {
                        ObjectArray objectArray1 = (ObjectArray) objectArray.get(i);
                        if (objectArray1.size() == 0)
                            continue;
                        try (PreparedStatement stm = isUpdateReturnKeys ? connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS) : connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
                            stm.setQueryTimeout(queryTimeout);
                            ParameterMetaData parameterMetaData = stm.getParameterMetaData();
                            int parameterCount = parameterMetaData.getParameterCount();
                            if ((objectArray1.size() < parameterCount) || (useAutoConvert && type == Type.mysqlClient && parameterTypes.size() < parameterCount)) {
                                externalExecutionContextTool.addError("need " + parameterCount + " params");
                                return;
                            }
                            for (int j = 1; j <= parameterCount; j++) {
                                int jj = j - 1;
                                Object value = objectArray1.get(jj);
                                if (useAutoConvert) {
                                    int parameterType;
                                    if (type == Type.mysqlClient) {
                                        parameterType = parameterTypes.get(jj);
                                    } else {
                                        parameterType = parameterMetaData.getParameterType(j);
                                    }
                                    if ("NULL".equals(value)) {
                                        stm.setNull(j, parameterType);
                                    } else if (value instanceof ObjectArray) {
                                        ObjectArray objectArrayValue = (ObjectArray) value;
                                        if (objectArrayValue.isSimple()) {
                                            Array array;
                                            if (objectArrayValue.getType() != ObjectType.VALUE_ANY && ModuleUtils.isArrayContainNumber(objectArrayValue)) {
                                                if (objectArrayValue.getType() == ObjectType.FLOAT || objectArrayValue.getType() == ObjectType.DOUBLE || objectArrayValue.getType() == ObjectType.BIG_DECIMAL) {
                                                    List<Float> lst = new ArrayList<>(objectArrayValue.size());
                                                    for (int k = 0; k < objectArrayValue.size(); k++)
                                                        lst.add(((Number) objectArrayValue.get(k)).floatValue());
                                                    array = stm.getConnection().createArrayOf("FLOAT", lst.toArray());
                                                } else {
                                                    List<Long> lst = new ArrayList<>(objectArrayValue.size());
                                                    for (int k = 0; k < objectArrayValue.size(); k++)
                                                        lst.add(((Number) objectArrayValue.get(k)).longValue());
                                                    array = stm.getConnection().createArrayOf("BIGINT", lst.toArray());
                                                }
                                            } else {
                                                List<String> lst = new ArrayList<>(objectArrayValue.size());
                                                for (int k = 0; k < objectArrayValue.size(); k++)
                                                    lst.add(objectArrayValue.get(k).toString());
                                                array = stm.getConnection().createArrayOf("VARCHAR", lst.toArray());
                                            }
                                            stm.setArray(j, array);
                                        } else {
                                            stm.setNull(j, parameterType);
                                        }
                                    } else {
                                        boolean isString = value instanceof String;
                                        switch (parameterType) {
                                            case Types.BOOLEAN:
                                                value = toBooleanObj(value);
                                                break;
                                            case Types.TIMESTAMP:
                                                value = !isString || NumberUtils.isCreatable((String) value) ? new Timestamp(toLongObj(value)) : value;
                                                break;
                                            case Types.DATE:
                                                value = !isString || NumberUtils.isCreatable((String) value) ? new java.sql.Date(toLongObj(value)) : value;
                                                break;
                                            case Types.TIME:
                                                value = !isString || NumberUtils.isCreatable((String) value) ? new Time(toLongObj(value)) : value;
                                                break;
                                        }
                                        stm.setObject(j, value);
                                    }
                                } else {
                                    stm.setObject(j, value);
                                }
                            }
                            if (isUpdateReturnKeys) {
                                stm.executeUpdate();
                            } else {
                                stm.execute();
                            }
                            printStmResult(externalConfigurationTool, externalExecutionContextTool, stm, isUpdateReturnKeys);
                        } catch (Exception e) {
                            if (needRollback)
                                connection.rollback();
                            throw new ModuleException(String.format("%s: %d %s", e.getMessage(), elementId, sql), e);
                        }
                        if (externalExecutionContextTool.isNeedStop()) {
                            if (needRollback)
                                connection.rollback();
                            break;
                        }
                    }
                }
            }
            elementId++;
        }
    }

    private long toLong(IMessage message) {
        if (ModuleUtils.isString(message)) {
            return NumberUtils.toLong(ModuleUtils.getString(message));
        } else if (ModuleUtils.isBytes(message)) {
            return 0;
        } else if (ModuleUtils.isNumber(message)) {
            return ModuleUtils.getNumber(message).longValue();
        } else {
            return 0;
        }
    }

    private long toLong(ObjectField field) {
        if (ModuleUtils.isString(field)) {
            return NumberUtils.toLong(ModuleUtils.getString(field));
        } else if (ModuleUtils.isBytes(field)) {
            return 0;
        } else if (ModuleUtils.isNumber(field)) {
            return ModuleUtils.getNumber(field).longValue();
        } else {
            return 0;
        }
    }

    private long toLongObj(Object obj) {
        if (obj instanceof String) {
            return NumberUtils.toLong((String) obj);
        } else if (obj instanceof Number) {
            return ((Number) obj).longValue();
        } else {
            return 0;
        }
    }

    private boolean toBoolean(IMessage message) {
        if (ModuleUtils.isString(message)) {
            return BooleanUtils.toBoolean(ModuleUtils.getString(message));
        } else if (ModuleUtils.isBytes(message)) {
            return false;
        } else if (ModuleUtils.isNumber(message)) {
            return ModuleUtils.getNumber(message).longValue() > 0;
        } else {
            return false;
        }
    }

    private boolean toBoolean(ObjectField field) {
        if (ModuleUtils.isString(field)) {
            return BooleanUtils.toBoolean(ModuleUtils.getString(field));
        } else if (ModuleUtils.isBytes(field)) {
            return false;
        } else if (ModuleUtils.isNumber(field)) {
            return ModuleUtils.getNumber(field).longValue() > 0;
        } else {
            return false;
        }
    }

    private boolean toBooleanObj(Object obj) {
        if (obj instanceof String) {
            return BooleanUtils.toBoolean((String) obj);
        } else if (obj instanceof Number) {
            return ((Number) obj).longValue() > 0;
        } else {
            return false;
        }
    }

    private void printStmResult(ConfigurationTool externalConfigurationTool, ExecutionContextTool externalExecutionContextTool, Statement stm, boolean getGeneratedKeys) throws Exception {
        ResultSet resultSet = getGeneratedKeys ? stm.getGeneratedKeys() : stm.getResultSet();
        if (resultSet != null) {
            addMessages(externalConfigurationTool, externalExecutionContextTool, resultSet, getGeneratedKeys);
            resultSet.close();
        } else {
            externalExecutionContextTool.addMessage(stm.getUpdateCount());
        }
    }

    private void addMessages(ConfigurationTool externalConfigurationTool, ExecutionContextTool externalExecutionContextTool, ResultSet rs, boolean getGeneratedKeys) throws Exception {
        /*
        if(!rs.next())
            return;
        rs.beforeFirst();
        */
        if (!getGeneratedKeys && !rs.isBeforeFirst() && rs.getRow() == 0)
            return;

        ResultSetMetaData rsmd = rs.getMetaData();
        int numColumns = rsmd.getColumnCount();
        // externalExecutionContextTool.addMessage(numColumns);
        ObjectArray objectArray = new ObjectArray(10, ObjectType.OBJECT_ELEMENT);
        // results.add(numColumns);
        while (rs.next()) {
            ObjectElement objectElement = new ObjectElement();
            for (int i = 1; i < numColumns + 1; i++) {
                String column_name = rsmd.getColumnName(i);
                // externalExecutionContextTool.addMessage(column_name);
                Object value;
                int columnType = rsmd.getColumnType(i);
                if (columnType == java.sql.Types.ARRAY) {
                    // externalExecutionContextTool.addMessage(ValueType.BYTES, rs.getArray(column_name));
                    // externalExecutionContextTool.addMessage(convertNull(rs.getObject(column_name)).toString());
                    value = convertNull(rs.getObject(column_name)).toString();
                } else if (columnType == java.sql.Types.BIGINT) {
                    // externalExecutionContextTool.addMessage(rs.getInt(column_name));
                    value = rs.getLong(column_name);
                } else if (columnType == java.sql.Types.BOOLEAN) {
                    // externalExecutionContextTool.addMessage(BooleanUtils.toStringTrueFalse(rs.getBoolean(column_name)));
                    value = BooleanUtils.toStringTrueFalse(rs.getBoolean(column_name));
                } else if (columnType == java.sql.Types.BLOB) {
                    /*
                    Blob blob = rs.getBlob("SomeDatabaseField");
                    int blobLength = (int) blob.length();
                    byte[] blobAsBytes = blob.getBytes(1, blobLength);
                    blob.free();
                    */
                    // externalExecutionContextTool.addMessage(convertNull(rs.getBytes(column_name)));
                    value = convertNull(rs.getBytes(column_name));
                } else if (columnType == java.sql.Types.CLOB) {
                    Clob clob = rs.getClob(i);
                    // externalExecutionContextTool.addMessage(convertNull(clob != null && clob.length() > 0 ? IOUtils.toString(clob.getCharacterStream()) : null));
                    value = convertNull(clob != null && clob.length() > 0 ? IOUtils.toString(clob.getCharacterStream()) : null);
                } else if (columnType == java.sql.Types.DOUBLE) {
                    // externalExecutionContextTool.addMessage(rs.getDouble(column_name));
                    value = rs.getDouble(column_name);
                } else if (columnType == java.sql.Types.FLOAT) {
                    // externalExecutionContextTool.addMessage(rs.getFloat(column_name));
                    value = rs.getFloat(column_name);
                } else if (columnType == java.sql.Types.INTEGER) {
                    // externalExecutionContextTool.addMessage(rs.getInt(column_name));
                    value = rs.getInt(column_name);
                } else if (columnType == Types.DECIMAL) {
                    // externalExecutionContextTool.addMessage(rs.getInt(column_name));
                    value = rs.getBigDecimal(column_name);
                } else if (columnType == java.sql.Types.NVARCHAR) {
                    // externalExecutionContextTool.addMessage(convertNull(rs.getNString(column_name)));
                    value = convertNull(rs.getNString(column_name));
                } else if (columnType == java.sql.Types.VARCHAR) {
                    // externalExecutionContextTool.addMessage(convertNull(rs.getString(column_name)));
                    value = convertNull(rs.getString(column_name));
                } else if (columnType == java.sql.Types.TINYINT) {
                    // externalExecutionContextTool.addMessage(rs.getInt(column_name));
                    value = rs.getShort(column_name);
                } else if (columnType == java.sql.Types.SMALLINT) {
                    // externalExecutionContextTool.addMessage(rs.getInt(column_name));
                    value = rs.getShort(column_name);
                } else if (columnType == java.sql.Types.DATE) {
                    Date date = rs.getDate(column_name);
                    // externalExecutionContextTool.addMessage(date != null ? date.getTime() : "NULL");
                    value = date != null ? date.getTime() : "NULL";
                } else if (columnType == java.sql.Types.TIMESTAMP) {
                    Timestamp timestamp = rs.getTimestamp(column_name);
                    // externalExecutionContextTool.addMessage(timestamp != null ? timestamp.getTime() : "NULL");
                    value = timestamp != null ? timestamp.getTime() : "NULL";
                } else {
                    // externalExecutionContextTool.addMessage(convertNull(rs.getObject(column_name)).toString());
                    value = convertNull(rs.getObject(column_name)).toString();
                    // externalConfigurationTool.loggerInfo(column_name + " " + columnType + " " + rsmd.getColumnClassName(i) + value);
                }
                if (resultFormat != ResultFormat.OBJECT_WITHOUT_NULL || !rs.wasNull())
                    objectElement.getFields().add(new ObjectField(resultSetColumnNameToUpperCase ? column_name.toUpperCase() : column_name, ModuleUtils.getObjectType(value), value));
            }
            objectArray.add(objectElement);
        }
        if (getGeneratedKeys) {
            Long id = 0L;
            if (objectArray.size() > 0) {
                ObjectElement objectElement = (ObjectElement) objectArray.get(0);
                id = objectElement.findField("1").map(ModuleUtils::getString).filter(NumberUtils::isCreatable).map(NumberUtils::createNumber).map(Number::longValue)
                        .orElseGet(() -> objectElement.findFieldIgnoreCase("id").map(ModuleUtils::getNumber).map(Number::longValue)
                                .orElseGet(() -> objectElement.getFields().stream().filter(ModuleUtils::isNumber).findAny().map(ModuleUtils::getNumber).map(Number::longValue).orElse(0L)));
            }
            externalExecutionContextTool.addMessage(id);
        } else {
            switch (resultFormat) {
                case OBJECT_SERIALIZATION:
                    externalExecutionContextTool.addMessage(ModuleUtils.serializeFromObject(objectArray));
                    break;
                case OBJECT:
                case OBJECT_WITHOUT_NULL:
                    externalExecutionContextTool.addMessage(objectArray);
                    break;
            }
        }
    }

    private Object convertNull(Object obj) {
        return obj != null ? obj : "NULL";
    }

    private void startConnectionWithTransaction(ExecutionContextTool externalExecutionContextTool) throws Exception {
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        long id = connectionId.incrementAndGet();
        connections.put(id, connection);
        externalExecutionContextTool.addMessage(id);
    }

    private void commitAndStopConnection(ConfigurationTool externalConfigurationTool, ExecutionContextTool externalExecutionContextTool, long transactionId) throws Exception {
        Connection connection = connections.remove(transactionId);
        if (connection != null) {
            try {
                connection.commit();
            } finally {
                try {
                    connection.close();
                } catch (Exception e) {
                    // throw new ModuleException("error", e);
                    externalConfigurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
                }
            }
            externalExecutionContextTool.addMessage(transactionId);
        }
    }

    private void rollbackAndStopConnection(ConfigurationTool externalConfigurationTool, ExecutionContextTool externalExecutionContextTool, long transactionId) throws Exception {
        Connection connection = connections.remove(transactionId);
        if (connection != null) {
            try {
                connection.rollback();
            } finally {
                try {
                    connection.close();
                } catch (Exception e) {
                    // throw new ModuleException("error", e);
                    externalConfigurationTool.loggerWarn(ModuleUtils.getStackTraceAsString(e));
                }
            }
            externalExecutionContextTool.addMessage(transactionId);
        }
    }

    private void executeInTransaction(ConfigurationTool externalConfigurationTool, ExecutionContextTool externalExecutionContextTool, long transactionId, List<IMessage> messages, boolean isUpdateReturnKeys) throws Exception {
        Connection connection = connections.get(transactionId);
        if (connection != null) {
            for (int i = 0; i < messages.size(); i++) {
                IMessage message = messages.get(i);
                try (Statement stm = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
                    stm.setQueryTimeout(queryTimeout);
                    if (isUpdateReturnKeys) {
                        stm.executeUpdate((String) message.getValue(), Statement.RETURN_GENERATED_KEYS);
                    } else {
                        stm.execute((String) message.getValue());
                    }
                    printStmResult(externalConfigurationTool, externalExecutionContextTool, stm, isUpdateReturnKeys);
                } catch (Exception e) {
                    throw new ModuleException(String.format("%s: %d %s", e.getMessage(), i, message.getValue()), e);
                }
            }
        }
    }

    private void executePreparedStatementInTransaction(ConfigurationTool externalConfigurationTool, ExecutionContextTool externalExecutionContextTool, long transactionId, LinkedList<IMessage> messages, boolean isArray, boolean isUpdateReturnKeys) throws Exception {
        Connection connection = connections.get(transactionId);
        if (connection != null) {
            executePreparedStatement(externalConfigurationTool, externalExecutionContextTool, messages, connection, false, isArray, isUpdateReturnKeys);
        }
    }

    private void executeSqlScript(Connection connection, File inputFile) throws Exception {
        // Delimiter
        // String delimiter = ";";

        try (Scanner scanner = new Scanner(inputFile)) {
            scanner.useDelimiter("(;(\r)?\n)|(--\n)");
            // Statement st = null;

            // Loop through the SQL file statements
            // Statement currentStatement = null;
            try (Statement st = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
                int elementId = 0;
                while (scanner.hasNext()) {
                    // Get statement
                    String rawStatement = scanner.next()/* + delimiter*/;
                    if (rawStatement.startsWith("/*!") && rawStatement.endsWith("*/")) {
                        int i = rawStatement.indexOf(' ');
                        rawStatement = rawStatement.substring(i + 1, rawStatement.length() - " */".length());
                    }

                    if (rawStatement.trim().length() > 0) {
                        try {
                            st.execute(rawStatement);
                        } catch (Exception e) {
                            throw new ModuleException(String.format("%s: %d %s", e.getMessage(), elementId, rawStatement), e);
                        }
                    }
                    elementId++;
                }
            }
        }
    }

}

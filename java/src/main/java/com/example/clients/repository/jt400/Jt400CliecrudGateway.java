package com.example.clients.repository.jt400;

import com.example.clients.domain.Client;
import com.example.clients.domain.ClientErrorCode;
import com.example.clients.domain.ClientException;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.ProgramCall;
import com.ibm.as400.access.ProgramParameter;
import com.ibm.as400.access.QSYSObjectPathName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * IBM Toolbox for Java transport. Each call opens its own AS400 connection; swap in an
 * {@code AS400ConnectionPool} / pooled DataSource if call volume requires it.
 */
public class Jt400CliecrudGateway implements CliecrudGateway {

    private static final Logger log = LoggerFactory.getLogger(Jt400CliecrudGateway.class);

    static final String PROGRAM = "CLIECRUD";
    static final String LIST_SQL = "SELECT CLDNI, CLNAME, CHAR(CLCRTS), CLCRUS, CHAR(CLUPTS), CLUPUS,"
            + " CHAR(CLDETS), CLDEUS FROM TRCLIE WHERE CLDEUS = '' ORDER BY CLDNI";

    private final Jt400Properties properties;
    private final CliesrvCodec codec;

    public Jt400CliecrudGateway(Jt400Properties properties, CliesrvCodec codec) {
        properties.validate();
        this.properties = properties;
        this.codec = codec;
    }

    @Override
    public CliecrudResponse call(char op, long dni, String name) {
        AS400 system = new AS400(properties.host(), properties.user(), properties.password().toCharArray());
        try {
            system.setGuiAvailable(false);
            addDataLibraryToLibraryList(system);

            ProgramParameter[] parms = {
                    new ProgramParameter(codec.encodeOp(op)),
                    new ProgramParameter(codec.encodeDni(dni)),
                    new ProgramParameter(codec.encodeName(name)),
                    new ProgramParameter(CliesrvCodec.blanks(CliesrvCodec.STATUS_LENGTH), CliesrvCodec.STATUS_LENGTH),
                    new ProgramParameter(CliesrvCodec.blanks(CliesrvCodec.CLIENT_LENGTH), CliesrvCodec.CLIENT_LENGTH)
            };
            ProgramCall program = new ProgramCall(system);
            program.setProgram(QSYSObjectPathName.toPath(properties.programLibrary(), PROGRAM, "PGM"), parms);
            if (!program.run()) {
                throw ioError("CLIECRUD call failed: " + messages(program.getMessageList()), null);
            }
            return new CliecrudResponse(parms[3].getOutputData(), parms[4].getOutputData());
        } catch (ClientException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ioError("CLIECRUD call interrupted", e);
        } catch (Exception e) {
            throw ioError("CLIECRUD call failed", e);
        } finally {
            system.disconnectAllServices();
        }
    }

    @Override
    public List<Client> selectActive() {
        String url = "jdbc:as400://" + properties.host() + "/" + properties.fileLibrary()
                + ";naming=sql;errors=full;prompt=false";
        Properties info = new Properties();
        info.setProperty("user", properties.user());
        info.setProperty("password", properties.password());
        try {
            Class.forName("com.ibm.as400.access.AS400JDBCDriver");
            try (Connection connection = DriverManager.getConnection(url, info);
                 PreparedStatement statement = connection.prepareStatement(LIST_SQL);
                 ResultSet rs = statement.executeQuery()) {
                List<Client> clients = new ArrayList<>();
                while (rs.next()) {
                    clients.add(CliesrvCodec.toClient(rs.getLong(1), rs.getString(2),
                            rs.getString(3), rs.getString(4),
                            rs.getString(5), rs.getString(6),
                            rs.getString(7), rs.getString(8)));
                }
                return clients;
            }
        } catch (ClassNotFoundException | SQLException e) {
            throw ioError("TRCLIE query failed", e);
        }
    }

    /** CLIECRUD opens TRCLIE unqualified, so the data library must be on the server job's library list. */
    private void addDataLibraryToLibraryList(AS400 system) throws Exception {
        CommandCall command = new CommandCall(system);
        if (!command.run("ADDLIBLE LIB(" + properties.fileLibrary() + ")")) {
            AS400Message[] messages = command.getMessageList();
            boolean alreadyInList = Arrays.stream(messages).anyMatch(m -> "CPF2103".equals(m.getID()));
            if (!alreadyInList) {
                throw ioError("ADDLIBLE failed: " + messages(messages), null);
            }
        }
    }

    private static String messages(AS400Message[] messages) {
        return messages == null ? "" : Arrays.stream(messages)
                .map(m -> m.getID() + " " + m.getText())
                .collect(Collectors.joining("; "));
    }

    private static ClientException ioError(String detail, Exception cause) {
        log.error("{}", detail, cause);
        return new ClientException(ClientErrorCode.IOERR, "IBM i request failed", cause);
    }
}

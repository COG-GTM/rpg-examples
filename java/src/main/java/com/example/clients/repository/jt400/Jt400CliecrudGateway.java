package com.example.clients.repository.jt400;

import com.example.clients.domain.ClientErrorCode;
import com.example.clients.domain.ClientException;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400JDBCDataSource;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.ProgramCall;
import com.ibm.as400.access.ProgramParameter;
import com.ibm.as400.access.QSYSObjectPathName;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Real IBM i transport: {@link ProgramCall} for CLIECRUD and JDBC for the list query. */
public class Jt400CliecrudGateway implements CliecrudGateway {

    static final String LIST_SQL = "SELECT CLDNI, CLNAME, CHAR(CLCRTS), CLCRUS, CHAR(CLUPTS), CLUPUS, "
            + "CHAR(CLDETS), CLDEUS FROM TRCLIE WHERE CLDEUS = '' ORDER BY CLDNI";
    /** ADDLIBLE answers CPF2103 when the library is already in the library list. */
    static final String ALREADY_IN_LIBL = "CPF2103";

    private final Jt400Properties props;
    private final CliesrvCodec codec;
    private final AS400JDBCDataSource dataSource;

    public Jt400CliecrudGateway(Jt400Properties props, CliesrvCodec codec) {
        this.props = props;
        this.codec = codec;
        this.dataSource = new AS400JDBCDataSource(props.host(), props.user(), props.password());
        dataSource.setNaming("system");
        dataSource.setLibraries(props.effectiveDataLibrary());
        dataSource.setPrompt(false);
    }

    @Override
    public CliecrudReply call(char op, long dni, String name) {
        AS400 system = new AS400(props.host(), props.user(), props.password().toCharArray());
        try {
            system.setGuiAvailable(false);
            addDataLibrary(system);
            ProgramParameter[] parms = {
                new ProgramParameter(codec.encodeOp(op)),
                new ProgramParameter(codec.encodeDni(dni)),
                new ProgramParameter(codec.encodeName(name)),
                new ProgramParameter(CliesrvCodec.STATUS_LENGTH),
                new ProgramParameter(CliesrvCodec.CLIENT_LENGTH),
            };
            String path = QSYSObjectPathName.toPath(props.library().trim().toUpperCase(), "CLIECRUD", "PGM");
            ProgramCall pgm = new ProgramCall(system, path, parms);
            if (!pgm.run()) {
                throw new ClientException(ClientErrorCode.IOERR, "CLIECRUD failed: " + messages(pgm.getMessageList()));
            }
            return new CliecrudReply(parms[3].getOutputData(), parms[4].getOutputData());
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ClientException(ClientErrorCode.IOERR, "IBM i call failed: " + e.getMessage(), e);
        } finally {
            system.disconnectAllServices();
        }
    }

    /** CLIECRUD opens TRCLIE unqualified, so the data library must be on the job's library list. */
    private void addDataLibrary(AS400 system) throws Exception {
        CommandCall cmd = new CommandCall(system);
        if (!cmd.run("ADDLIBLE LIB(" + props.effectiveDataLibrary().toUpperCase() + ")")) {
            AS400Message[] msgs = cmd.getMessageList();
            boolean alreadyThere = Arrays.stream(msgs).anyMatch(m -> ALREADY_IN_LIBL.equals(m.getID()));
            if (!alreadyThere) {
                throw new ClientException(ClientErrorCode.IOERR, "ADDLIBLE failed: " + messages(msgs));
            }
        }
    }

    @Override
    public List<TrclieRow> selectActive() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(LIST_SQL);
             ResultSet rs = ps.executeQuery()) {
            List<TrclieRow> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(new TrclieRow(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8)));
            }
            return rows;
        } catch (SQLException e) {
            throw new ClientException(ClientErrorCode.IOERR, "TRCLIE query failed: " + e.getMessage(), e);
        }
    }

    private static String messages(AS400Message[] msgs) {
        if (msgs == null || msgs.length == 0) {
            return "no messages";
        }
        return Arrays.stream(msgs).map(m -> m.getID() + " " + m.getText()).collect(Collectors.joining("; "));
    }
}

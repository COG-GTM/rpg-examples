package com.example.clients.repository.jt400;

import com.example.clients.domain.Client;
import com.example.clients.domain.ClientOperationException;
import com.example.clients.domain.ClientStatus;
import com.example.clients.repository.ClientRepository;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400ConnectionPool;
import com.ibm.as400.access.AS400JDBCDataSource;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.ProgramCall;
import com.ibm.as400.access.ProgramParameter;
import com.ibm.as400.access.QSYSObjectPathName;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Phase 2 implementation: CRUD goes through the RPG business logic by calling the CLIECRUD driver
 * (structured mode, 5 parameters) with jt400 {@link ProgramCall}; CLIECRUD delegates to CLIESRV.
 * The read-only list uses JDBC over TRCLIE because CLIESRV has no list operation.
 */
@Repository
@ConditionalOnProperty(name = "clients.repository", havingValue = "jt400")
public class Jt400ClientRepository implements ClientRepository {

    private static final String LIST_SQL = """
            SELECT CLDNI, CLNAME, CLCRTS, CLCRUS, CLUPTS, CLUPUS, CLDETS, CLDEUS
              FROM %s.TRCLIE
             WHERE CLDEUS = ''
             ORDER BY CLDNI""";

    private final Jt400Properties props;
    private final CliesrvCodec codec;
    private final AS400ConnectionPool pool = new AS400ConnectionPool();
    private final AS400JDBCDataSource dataSource;
    private final String programPath;

    public Jt400ClientRepository(Jt400Properties props) {
        this.props = props;
        this.codec = new CliesrvCodec(props.ccsid());
        this.programPath = QSYSObjectPathName.toPath(props.library(), props.program(), "PGM");
        this.dataSource = new AS400JDBCDataSource(props.system(), props.user(), props.password());
        this.dataSource.setNaming("sql");
    }

    @Override
    public List<Client> findAllActive() {
        String sql = LIST_SQL.formatted(props.library());
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Client> clients = new ArrayList<>();
            while (rs.next()) {
                clients.add(new Client(
                        rs.getLong(1), rs.getString(2).stripTrailing(),
                        toLocal(rs.getTimestamp(3)), rs.getString(4).trim(),
                        toLocal(rs.getTimestamp(5)), rs.getString(6).trim(),
                        toLocal(rs.getTimestamp(7)), rs.getString(8).trim()));
            }
            return clients;
        } catch (SQLException e) {
            throw new ClientOperationException(ClientStatus.IO_ERROR, "Error listing clients", e);
        }
    }

    @Override
    public Client read(long dni) {
        return call('R', dni, "").client();
    }

    @Override
    public void create(long dni, String name) {
        call('C', dni, name);
    }

    @Override
    public void update(long dni, String name) {
        call('U', dni, name);
    }

    @Override
    public void delete(long dni) {
        call('D', dni, "");
    }

    private Result call(char op, long dni, String name) {
        ProgramParameter[] params = {
            new ProgramParameter(codec.encodeOp(op)),
            new ProgramParameter(codec.encodeDni(dni)),
            new ProgramParameter(codec.encodeName(name)),
            new ProgramParameter(CliesrvCodec.STATUS_LENGTH),
            new ProgramParameter(CliesrvCodec.CLIENT_LENGTH)
        };
        AS400 system = null;
        try {
            system = pool.getConnection(props.system(), props.user(), props.password(), AS400.COMMAND);
            ProgramCall pgm = new ProgramCall(system, programPath, params);
            if (!pgm.run()) {
                throw new ClientOperationException(ClientStatus.IO_ERROR,
                        "CLIECRUD failed: " + messages(pgm.getMessageList()));
            }
        } catch (ClientOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new ClientOperationException(ClientStatus.IO_ERROR, "CLIECRUD call failed", e);
        } finally {
            if (system != null) {
                pool.returnConnectionToPool(system);
            }
        }

        CliesrvCodec.Status status = codec.decodeStatus(params[3].getOutputData());
        if (!status.success()) {
            throw new ClientOperationException(status.code(), status.message());
        }
        Client client = op == 'R' ? codec.decodeClient(params[4].getOutputData()) : null;
        return new Result(status, client);
    }

    private static String messages(AS400Message[] list) {
        return list == null ? "" : Arrays.stream(list)
                .map(m -> m.getID() + " " + m.getText())
                .collect(Collectors.joining("; "));
    }

    private static LocalDateTime toLocal(Timestamp ts) {
        if (ts == null) {
            return null;
        }
        LocalDateTime value = ts.toLocalDateTime();
        return value.getYear() <= 1 ? null : value;
    }

    @PreDestroy
    void close() {
        pool.close();
    }

    private record Result(CliesrvCodec.Status status, Client client) {
    }
}

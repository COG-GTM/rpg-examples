package com.example.clients.clientsv;

import com.example.clients.domain.Client;
import com.example.clients.domain.ClientException;
import com.example.clients.domain.ClientsvCode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/**
 * CLIENTSV over jt400: CRUD goes through the service program (business rules stay in RPG);
 * the list is a read-only SQL query on TRCLIE.
 */
public class Jt400ClientsvGateway implements ClientsvGateway {

    /** CHAR(ts) returns the same 26-char *ISO form as the RPG DS. */
    static final String LIST_SQL = "SELECT CLDNI, CLNAME, CHAR(CLCRTS), CLCRUS, CHAR(CLUPTS), CLUPUS, "
            + "CHAR(CLDETS), CLDEUS FROM TRCLIE";
    static final String ACTIVE_ONLY = " WHERE CLDEUS = ''";
    static final String ORDER = " ORDER BY CLDNI";

    private final ServiceProgramInvoker invoker;
    private final ClientsvCodec codec;
    private final DataSource dataSource;

    public Jt400ClientsvGateway(ServiceProgramInvoker invoker, ClientsvCodec codec, DataSource dataSource) {
        this.invoker = invoker;
        this.codec = codec;
        this.dataSource = dataSource;
    }

    @Override
    public ClientsvResponse execute(ClientsvRequest request) {
        return codec.decodeResponse(invoker.execute(codec.encodeRequest(request)));
    }

    @Override
    public List<Client> list(boolean includeDeleted) {
        String sql = LIST_SQL + (includeDeleted ? "" : ACTIVE_ONLY) + ORDER;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Client> rows = new ArrayList<>();
            while (rs.next()) {
                String name = rs.getString(2);
                rows.add(new Client(rs.getLong(1), name == null ? "" : name.stripTrailing(),
                        ClientsvCodec.timestamp(rs.getString(3)), ClientsvCodec.user(rs.getString(4)),
                        ClientsvCodec.timestamp(rs.getString(5)), ClientsvCodec.user(rs.getString(6)),
                        ClientsvCodec.timestamp(rs.getString(7)), ClientsvCodec.user(rs.getString(8))));
            }
            return rows;
        } catch (SQLException e) {
            throw new ClientException(ClientsvCode.IOERR, "TRCLIE query failed: " + e.getMessage(), e);
        }
    }
}

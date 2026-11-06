package com.example.clients.clientsv;

import com.example.clients.domain.ClientException;
import com.example.clients.domain.ClientsvCode;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.CommandCall;
import com.ibm.as400.access.ProgramParameter;
import com.ibm.as400.access.QSYSObjectPathName;
import com.ibm.as400.access.ServiceProgramCall;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Calls exported procedure {@code clientsvExecute} of *SRVPGM CLIENTSV with jt400
 * {@link ServiceProgramCall}. CLIENTSV opens TRCLIE unqualified, so the data library is put first
 * on the library list of the same host-server job before the call.
 */
public class Jt400ServiceProgramInvoker implements ServiceProgramInvoker {

    static final String PROCEDURE = "clientsvExecute";
    private static final String ALREADY_IN_LIBL = "CPF2103";

    private final IbmiProperties props;

    public Jt400ServiceProgramInvoker(IbmiProperties props) {
        this.props = props;
    }

    @Override
    public byte[] execute(byte[] in) {
        AS400 system = new AS400(props.host(), props.user(), props.password().toCharArray());
        try {
            system.setGuiAvailable(false);
            addDataLibrary(system);
            ProgramParameter[] parms = {
                new ProgramParameter(ProgramParameter.PASS_BY_REFERENCE, in),
                new ProgramParameter(ProgramParameter.PASS_BY_REFERENCE, new byte[ClientsvCodec.OUT_LENGTH],
                        ClientsvCodec.OUT_LENGTH),
            };
            String path = QSYSObjectPathName.toPath(props.library().trim().toUpperCase(), "CLIENTSV", "SRVPGM");
            ServiceProgramCall call = new ServiceProgramCall(system, path, PROCEDURE,
                    ServiceProgramCall.NO_RETURN_VALUE, parms);
            if (!call.run()) {
                throw new ClientException(ClientsvCode.IOERR, "CLIENTSV call failed: " + messages(call.getMessageList()));
            }
            return parms[1].getOutputData();
        } catch (ClientException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ClientException(ClientsvCode.IOERR, "IBM i call failed: " + e.getMessage(), e);
        } finally {
            system.disconnectAllServices();
        }
    }

    private void addDataLibrary(AS400 system) throws Exception {
        String lib = props.effectiveDataLibrary().trim().toUpperCase();
        CommandCall cmd = new CommandCall(system);
        cmd.run("RMVLIBLE LIB(" + lib + ")");
        if (!cmd.run("ADDLIBLE LIB(" + lib + ") POSITION(*FIRST)")) {
            AS400Message[] msgs = cmd.getMessageList();
            if (Arrays.stream(msgs).noneMatch(m -> ALREADY_IN_LIBL.equals(m.getID()))) {
                throw new ClientException(ClientsvCode.IOERR, "ADDLIBLE failed: " + messages(msgs));
            }
        }
    }

    private static String messages(AS400Message[] msgs) {
        if (msgs == null || msgs.length == 0) {
            return "no messages";
        }
        return Arrays.stream(msgs).map(m -> m.getID() + " " + m.getText()).collect(Collectors.joining("; "));
    }
}

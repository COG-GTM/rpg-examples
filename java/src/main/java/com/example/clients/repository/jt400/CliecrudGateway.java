package com.example.clients.repository.jt400;

import java.util.List;

/** Transport to the IBM i, split out so {@link Jt400ClientRepository} is testable with a fake. */
public interface CliecrudGateway {

    /** {@code CALL CLIECRUD (op dni name status client)} in structured (5-parm) mode. */
    CliecrudReply call(char op, long dni, String name);

    /** Active TRCLIE rows ordered by CLDNI. */
    List<TrclieRow> selectActive();
}

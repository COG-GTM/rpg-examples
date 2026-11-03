-- External stored procedure over program CLIECRUD (service program CLIESRV),
-- so clients can also be managed with a plain SQL / JDBC CALL.
-- CLIECRUD and file TRCLIE are resolved through the library list
-- (or qualify EXTERNAL NAME, e.g. 'PCOSTILLA/CLIECRUD').
--
-- All 5 parameters are passed, so CLIECRUD runs in structured mode (no DSPLY).
--   P_STATUS (63)  = SUCCESS(1) '1'/'0' | CODE(10) | MESSAGE(52)
--   P_CLIENT (146) = DNI(8) NAME(30) CREATED_TS(26) CREATED_BY(10)
--                    UPDATED_TS(26) UPDATED_BY(10) DELETED_TS(26) DELETED_BY(10)
CREATE OR REPLACE PROCEDURE CLIECRUD (
    IN  P_OP      CHAR(1),
    IN  P_DNI     NUMERIC(8, 0),
    IN  P_NAME    CHAR(30),
    OUT P_STATUS  CHAR(63),
    OUT P_CLIENT  CHAR(146))
  LANGUAGE RPGLE
  SPECIFIC CLIECRUD
  NOT DETERMINISTIC
  NO SQL
  CALLED ON NULL INPUT
  EXTERNAL NAME CLIECRUD
  PARAMETER STYLE GENERAL;

-- Convenience wrapper: same call, status split into columns.
CREATE OR REPLACE PROCEDURE CLIENT_CRUD (
    IN  P_OP       CHAR(1),
    IN  P_DNI      NUMERIC(8, 0),
    IN  P_NAME     CHAR(30),
    OUT P_SUCCESS  CHAR(1),
    OUT P_CODE     VARCHAR(10),
    OUT P_MESSAGE  VARCHAR(52),
    OUT P_CLIENT   CHAR(146))
  LANGUAGE SQL
  SPECIFIC CLIENT_CRUD
  MODIFIES SQL DATA
BEGIN
  DECLARE V_STATUS CHAR(63);
  CALL CLIECRUD(P_OP, P_DNI, P_NAME, V_STATUS, P_CLIENT);
  SET P_SUCCESS = SUBSTR(V_STATUS, 1, 1);
  SET P_CODE    = RTRIM(SUBSTR(V_STATUS, 2, 10));
  SET P_MESSAGE = RTRIM(SUBSTR(V_STATUS, 12, 52));
END;

-- Examples
-- CALL CLIECRUD('C', 12345678, 'ANA PEREZ', ?, ?);
-- CALL CLIENT_CRUD('R', 12345678, ' ', ?, ?, ?, ?);

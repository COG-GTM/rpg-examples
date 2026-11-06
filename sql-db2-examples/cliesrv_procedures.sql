-- SQL external procedure over program CLIECRUD (structured 5-parm mode).
-- Replace CLIELIB with the library that holds CLIECRUD, CLIESRV and TRCLIE.
--
-- STATUS (63 bytes)  = success(1) code(10) message(52)
-- CLIENT (146 bytes) = dni zoned(8,0), name(30), created ts(26)/user(10),
--                      updated ts(26)/user(10), deleted ts(26)/user(10)
CREATE OR REPLACE PROCEDURE CLIELIB.CLIECRUD (
    IN  OP     CHAR(1),
    IN  DNI    NUMERIC(8, 0),
    IN  NAME   CHAR(30),
    OUT STATUS CHAR(63),
    OUT CLIENT CHAR(146)
)
    LANGUAGE RPGLE
    SPECIFIC CLIELIB.CLIECRUD_SP
    NOT DETERMINISTIC
    NO SQL
    EXTERNAL NAME 'CLIELIB/CLIECRUD'
    PARAMETER STYLE GENERAL;

-- Example:
-- CALL CLIELIB.CLIECRUD('R', 12345678, ' ', ?, ?);

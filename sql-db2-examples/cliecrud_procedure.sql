-- SQL external procedure over program CLIECRUD (5-parm structured mode).
-- Run with the CLIECRUD library as current schema, e.g. SET SCHEMA MYLIB
--   P_STATUS: cliesrv_status_t, 63 bytes  (success 1 | code 10 | message 52)
--   P_CLIENT: cliesrv_client_t, 146 bytes (dni 8 | name 30 | 3 x (ts 26 | user 10))
-- Example: CALL CLIECRUD_SP('R', 12345678, ' ', ?, ?)
create or replace procedure cliecrud_sp (
  in  p_op     char(1),
  in  p_dni    numeric(8, 0),
  in  p_name   char(30),
  out p_status char(63),
  out p_client char(146))
  language rpgle
  specific cliecrud_sp
  not deterministic
  no sql
  parameter style general
  external name cliecrud

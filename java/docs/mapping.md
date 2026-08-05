# RPG → Java construct mapping

This document is the starting point for traceability during migration. Mappings
should be refined as each RPG example is migrated and tested.

| RPG construct | Java/Spring target |
|---|---|
| `dsply` | `System.out.println` or an injected application logger/output port |
| Data structure (`DS`) | Java `record`, class, or value object |
| `%lookup`, `sorta`, `%xfoot` | Java Streams and collection operations |
| `CHAIN` | Repository lookup such as `findById` |
| Embedded SQL cursor | `JdbcTemplate` query/callback or an explicit JDBC cursor |
| `PREPARE` / `EXECUTE` | Parameterized `PreparedStatement` |
| Compile-time arrays (`CTDATA`) | Java constants, immutable collections, or resource files |
| Date built-in functions | `java.time` types and formatters |
| CL compile/run commands | Gradle tasks and/or CI workflow steps |

## Per-file traceability matrix

| RPG source | Java target | Mapping notes | Verification |
|---|---|---|---|
| `rpg ile/basics/free_hello_world.rpgle` | `com.example.rpgexamples.basics.HelloWorld` | `dsply` → `println` | `HelloWorldTest (ConsoleCapture stdout assertion)` |

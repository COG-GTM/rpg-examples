/**
 * Seam for a future {@code JpaClientRepository} (selected with {@code CLIENTS_REPOSITORY=jpa}).
 *
 * <p>It would map an {@code @Entity} directly onto TRCLIE (record RETRCLIE, {@code @Id} CLDNI
 * {@code NUMERIC(8,0)}, CLNAME {@code CHAR(30)}, CLCRTS/CLUPTS/CLDETS {@code TIMESTAMP},
 * CLCRUS/CLUPUS/CLDEUS {@code CHAR(10)}) over the IBM i JDBC driver, convert the
 * {@code 0001-01-01-00.00.00.000000} sentinel to {@code null}, and re-implement the CLIESRV rules
 * (INVDNI, DUPDNI/NOTFND, DELETED) in Java. Unlike the jt400 path it bypasses CLIESRV, so the RPG
 * and Java copies of the rules would have to be kept in sync.
 *
 * <p>Until it exists, {@code RepositoryConfig} fails fast at startup when {@code jpa} is selected.
 */
package com.example.clients.repository.jpa;

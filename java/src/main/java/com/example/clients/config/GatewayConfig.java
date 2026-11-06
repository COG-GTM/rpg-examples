package com.example.clients.config;

import com.example.clients.clientsv.ClientsvCodec;
import com.example.clients.clientsv.ClientsvGateway;
import com.example.clients.clientsv.IbmiProperties;
import com.example.clients.clientsv.InMemoryClientsvGateway;
import com.example.clients.clientsv.Jt400ClientsvGateway;
import com.example.clients.clientsv.Jt400ServiceProgramInvoker;
import com.example.clients.domain.Client;
import com.ibm.as400.access.AS400JDBCDataSource;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    @ConditionalOnProperty(name = "clients.backend", havingValue = "memory", matchIfMissing = true)
    ClientsvGateway inMemoryGateway(ClientsProperties props) {
        InMemoryClientsvGateway gateway = new InMemoryClientsvGateway(props.auditUser(), Clock.systemDefaultZone());
        if (props.seed()) {
            LocalDateTime t = LocalDateTime.of(2026, 1, 15, 9, 30);
            gateway.put(new Client(20123456, "ANA GARCIA", t, "QPGMR", null, null, null, null));
            gateway.put(new Client(27654321, "LUIS PEREZ", t.plusDays(3), "QPGMR", t.plusDays(40), "JSMITH", null, null));
            gateway.put(new Client(31222333, "MARIA LOPEZ", t.plusDays(10), "QPGMR", null, null, t.plusDays(60), "JSMITH"));
        }
        return gateway;
    }

    @Bean
    @ConditionalOnProperty(name = "clients.backend", havingValue = "jt400")
    ClientsvGateway jt400Gateway(IbmiProperties ibmi) {
        ibmi.validate();
        AS400JDBCDataSource ds = new AS400JDBCDataSource(ibmi.host(), ibmi.user(), ibmi.password());
        ds.setNaming("system");
        ds.setLibraries(ibmi.effectiveDataLibrary());
        ds.setPrompt(false);
        return new Jt400ClientsvGateway(new Jt400ServiceProgramInvoker(ibmi), new ClientsvCodec(ibmi.ccsid()), ds);
    }
}

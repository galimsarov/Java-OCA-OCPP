package pss.mira.orp.JavaOCAOCPP.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitConfiguration {
    @Bean
    public ConnectionFactory connectionFactory() {
        // remote -> 10.10.254.40
        // local -> 192.168.6.110
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory("192.168.6.110");
        connectionFactory.setUsername("pss");
        connectionFactory.setPassword("p$$");
        connectionFactory.setConnectionTimeout(3000);
        connectionFactory.setRequestedHeartBeat(30);
        return connectionFactory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        return new RabbitTemplate(connectionFactory());
    }
}
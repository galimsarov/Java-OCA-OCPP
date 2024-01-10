package pss.mira.orp.JavaOCAOCPP.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitConfiguration {
    @Value("${rabbit.hostname}")
    private String hostName;
    @Value("${rabbit.username}")
    private String userName;
    @Value("${rabbit.password}")
    private String password;
    @Value("${rabbit.connectionTimeout}")
    private int connectionTimeout;
    @Value("${rabbit.heartbeat}")
    private int heartbeat;

    // Конфигурация подключения к рэббит
    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(hostName);
        connectionFactory.setUsername(userName);
        connectionFactory.setPassword(password);
        connectionFactory.setConnectionTimeout(connectionTimeout);
        connectionFactory.setRequestedHeartBeat(heartbeat);
        return connectionFactory;
    }

    @Bean
    public AmqpAdmin amqpAdmin() {
        return new RabbitAdmin(connectionFactory());
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        return new RabbitTemplate(connectionFactory());
    }

    // Указание используемых очередей
    @Bean
    public Queue myQueue1() {
        return new Queue("myQueue1");
    }

    @Bean
    public Queue myQueue2() {
        return new Queue("myQueue2");
    }

    // Создание обменника с определенным типом (DirectExchange в данном случае)
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange("direct-exchange");
    }

    // Привязка обменника к очередям по условию
    @Bean
    public Binding binding1() {
        return BindingBuilder.bind(myQueue1()).to(directExchange()).with("error");
    }

    @Bean
    public Binding binding2() {
        return BindingBuilder.bind(myQueue2()).to(directExchange()).with("warning");
    }

    @Bean
    public Binding binding3() {
        return BindingBuilder.bind(myQueue2()).to(directExchange()).with("info");
    }
}
package com.flowable.onboarding.config;

import org.flowable.spring.SpringProcessEngineConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class FlowableConfiguration {

    @Bean
    public SpringProcessEngineConfiguration processEngineConfiguration(
            DataSource dataSource,
            PlatformTransactionManager transactionManager) {

        SpringProcessEngineConfiguration config = new SpringProcessEngineConfiguration();
        config.setDataSource(dataSource);
        config.setTransactionManager(transactionManager);
        config.setDatabaseSchemaUpdate("true");
        // enable and configure async executor using current Flowable API
        config.setAsyncExecutorActivate(true);
        config.setAsyncExecutorCorePoolSize(2);
        config.setAsyncExecutorMaxPoolSize(5);
        // queue size configuration removed; newer Flowable versions manage this differently
        // rest API is auto-configured by the flowable-spring-boot-starter; no explicit setter needed

        return config;
    }

}

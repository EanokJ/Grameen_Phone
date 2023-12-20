package com.grameenphone.wipro.task_executor.config;

import com.grameenphone.wipro.task_executor.util.PropertyUtil;
import com.grameenphone.wipro.task_executor.util.orm.NamingStrategy;
import org.hibernate.dialect.MySQL57Dialect;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Properties;

@Configuration
@EnableTransactionManagement
@ComponentScan("com.grameenphone.wipro.task_executor")
public class FmfsOrmConfig implements ApplicationContextAware {
    @Bean
    public LocalContainerEntityManagerFactoryBean getEntityManagerFactoryBean() {
        LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
        factoryBean.setDataSource(FlexMFSDbConnectionPool.dataSource);
        factoryBean.setPackagesToScan("com.grameenphone.wipro.task_executor.model.orm.fmfs");
        factoryBean.setPersistenceUnitName("fmfs");
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setShowSql(Boolean.parseBoolean(PropertyUtil.getProperty("fmfs.orm.show_sql", "false")));
        vendorAdapter.setDatabasePlatform(MySQL57Dialect.class.getName());
        factoryBean.setJpaVendorAdapter(vendorAdapter);
        factoryBean.setJpaProperties(new Properties() {{
            put("hibernate.implicit_naming_strategy", NamingStrategy.class.getName());
            put("hibernate.connection.autocommit", "true");
        }});
        return factoryBean;
    }

    @Bean
    public JpaTransactionManager getJpaTransactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(getEntityManagerFactoryBean().getObject());
        return transactionManager;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ((StandardEnvironment)applicationContext.getEnvironment()).getPropertySources().addLast(PropertyUtil.getPropertySource());
    }
}
package com.grameenphone.wipro.task_executor;

import com.grameenphone.wipro.task_executor.config.CbpOrmConfig;
import com.grameenphone.wipro.task_executor.config.FmfsOrmConfig;
import com.grameenphone.wipro.task_executor.processors.BillCollector;
import com.grameenphone.wipro.task_executor.processors.BillPayer;
import com.grameenphone.wipro.task_executor.processors.DisputeStatusChecker;
import com.grameenphone.wipro.task_executor.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.net.URISyntaxException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {
    public final static ThreadPoolExecutor notificationSenderExecutors = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
    public static GenericApplicationContext cbpContext;
    public static GenericApplicationContext fmfsContext;
    private static ThreadPoolExecutor scheduledProcessExecutors = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);

    public static void initApplicationContexts() {
        cbpContext = new AnnotationConfigApplicationContext(CbpOrmConfig.class);
        fmfsContext = new AnnotationConfigApplicationContext(FmfsOrmConfig.class);
        fmfsContext.getEnvironment().getPropertySources().addLast(PropertyUtil.getPropertySource());
    }

    public static void openSession() {
        EntityManagerFactory entityManagerFactory = cbpContext.getBean(EntityManagerFactory.class);
        EntityManager em = entityManagerFactory.createEntityManager();
        EntityManagerHolder emHolder = new EntityManagerHolder(em);
        TransactionSynchronizationManager.bindResource(entityManagerFactory, emHolder);

        entityManagerFactory = fmfsContext.getBean(EntityManagerFactory.class);
        em = entityManagerFactory.createEntityManager();
        emHolder = new EntityManagerHolder(em);
        TransactionSynchronizationManager.bindResource(entityManagerFactory, emHolder);
    }

    public static void closeSession() {
        EntityManagerFactory entityManagerFactory = cbpContext.getBean(EntityManagerFactory.class);
        EntityManagerHolder emHolder = (EntityManagerHolder) TransactionSynchronizationManager.unbindResource(entityManagerFactory);
        EntityManagerFactoryUtils.closeEntityManager(emHolder.getEntityManager());

        entityManagerFactory = fmfsContext.getBean(EntityManagerFactory.class);
        emHolder = (EntityManagerHolder) TransactionSynchronizationManager.unbindResource(entityManagerFactory);
        EntityManagerFactoryUtils.closeEntityManager(emHolder.getEntityManager());
    }

    public static void main(String[] args) throws URISyntaxException {
        if (!PropertyUtil.cacheProperties(PropertyUtil.DEFAULT_PROPERTY_FILE_NAME)) {
            System.out.println("Terminating as configuration file could not be read");
            return;
        }
        initApplicationContexts();
        openSession();

        Logger logger = LoggerFactory.getLogger(Main.class);
        try {
            logger.debug("Starting corporate task executor.");

            scheduledProcessExecutors.submit(new BillCollector());
            scheduledProcessExecutors.submit(new BillPayer());
            if ("true".equals(PropertyUtil.getProperty("is_dispute_checker_node"))) {
                scheduledProcessExecutors.submit(new DisputeStatusChecker());
            }
            scheduledProcessExecutors.shutdown();
            try {
                scheduledProcessExecutors.awaitTermination(1, TimeUnit.DAYS);
                logger.debug("Successfully complete corporate task executor.");
            } catch (InterruptedException e) {
                logger.error("Error occured in corporate task executor: ", e.getMessage());
            }
            notificationSenderExecutors.shutdown();
            try {
                notificationSenderExecutors.awaitTermination(1, TimeUnit.DAYS);
            } catch (InterruptedException e) {
            }
        } finally {
            closeSession();
        }
    }
}
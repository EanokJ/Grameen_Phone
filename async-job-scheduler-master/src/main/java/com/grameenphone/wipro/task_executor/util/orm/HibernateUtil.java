package com.grameenphone.wipro.task_executor.util.orm;

import com.grameenphone.wipro.task_executor.repository.CrudDao;
import org.hibernate.Hibernate;
import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

public class HibernateUtil {
    public static <T> T initializeProxy(T source) {
        if(source instanceof HibernateProxy || source instanceof AbstractPersistentCollection) {
            if(!Hibernate.isInitialized(source)) {
                if(source instanceof HibernateProxy) {
                    LazyInitializer lazyInitializer = ((HibernateProxy)source).getHibernateLazyInitializer();
                    SharedSessionContractImplementor session = lazyInitializer.getSession();
                    if(session == null || !session.isOpen()) {
                        CrudDao.get(source.getClass()).setInSession(lazyInitializer);
                    }
                }
                if(source instanceof AbstractPersistentCollection) {
                    SharedSessionContractImplementor session = ((AbstractPersistentCollection)source).getSession();
                    if(session == null || !session.isOpen()) {
                        CrudDao.get(((AbstractPersistentCollection) source).getOwner().getClass()).initialize((AbstractPersistentCollection)source);
                        return source;
                    }
                }
                Hibernate.initialize(source);
            }
        }
        return source;
    }
}
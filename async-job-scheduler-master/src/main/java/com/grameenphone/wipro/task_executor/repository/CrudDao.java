package com.grameenphone.wipro.task_executor.repository;

import com.grameenphone.wipro.task_executor.Main;
import com.grameenphone.wipro.task_executor.util.KV;
import com.grameenphone.wipro.task_executor.util.orm.WhereBuilder;
import org.hibernate.Session;
import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.query.criteria.internal.OrderImpl;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.io.Closeable;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generic dao for dealing with data and object
 * @param <T> Class to be passed
 */
@Repository
public class CrudDao<T> {
	@PersistenceContext
	protected EntityManager entityManager;

	private Class<T> clazz;

	public CrudDao() {
		if(this.getClass().getSuperclass().equals(CrudDao.class)) {
			setClazz((Class<T>) ((ParameterizedType)this.getClass().getGenericSuperclass()).getActualTypeArguments()[0]);
		}
	}

	protected CrudDao(Class<T> clazz) {
		setClazz(clazz);
	}

	private EntityManager getEntityManager() {
		if(entityManager == null) {
			throw new RuntimeException("Generic clazz not defined");
		}
		return entityManager;
	}

	protected void setClazz(Class<T> clazz) {
		this.clazz = clazz;
	}

	public static <T> CrudDao<T> get(Class<T> clazz) {
		GenericApplicationContext context = clazz.getPackageName().contains("orm.cbp") ? Main.cbpContext : Main.fmfsContext;
		CrudDao dao;
		try {
			dao = (CrudDao) context.getBean(clazz.getName() + "Dao");
		} catch (NoSuchBeanDefinitionException n) {
			CrudDao<T> _dao = new CrudDao<>(clazz);
			context.registerBean(clazz.getName() + "Dao", CrudDao.class, () -> _dao);
			dao = (CrudDao)context.getBean(clazz.getName() + "Dao");
		}
		return dao;
	}

	@Repository
	@Scope("prototype")
	public static class ClosableCrudDao<U> extends CrudDao<U> implements Closeable {
		public ClosableCrudDao(Class<U> clazz) {
			super(clazz);
			if (clazz.getPackageName().contains("orm.cbp")) {
				entityManager = Main.cbpContext.getBean(EntityManagerFactory.class).createEntityManager();
			} else {
				entityManager = Main.fmfsContext.getBean(EntityManagerFactory.class).createEntityManager();
			}
		}

		@Override
		public void close() {
			entityManager.close();
		}
	}

	public static <T> ClosableCrudDao<T> getNewSession(Class<T> clazz) {
		if (clazz.getPackageName().contains("orm.cbp")) {
			return Main.cbpContext.getBean(ClosableCrudDao.class, clazz);
		} else {
			return Main.fmfsContext.getBean(ClosableCrudDao.class, clazz);
		}
	}

	public Class<T> getClazz() {
		return this.clazz;
	}

	public T findOne(long id) {
		return getEntityManager().find(clazz, id);
	}

	public T save(T entity) {
		getEntityManager().persist(entity);
		getEntityManager().flush();
		return entity;
	}

	public Iterable<T> saveAll(Iterable<T> entities) {
		List<T> savedEntities = new ArrayList<>();
		entities.forEach(en -> savedEntities.add(save(en)));
		return savedEntities;
	}

	public boolean existsById(Long aLong) {
		return false;
	}

	public List<T> findAll() {
		return getEntityManager().createQuery("from " + clazz.getName()).getResultList();
	}

	public List<T> findAll(long offset, int limit) {
		Query query = getEntityManager().createQuery("from " + clazz.getName());
		query.setMaxResults(limit);
		query.setFirstResult((int)offset);
		return query.getResultList();
	}

	public Iterable<T> findAllById(Iterable<Long> longs) {
		return null;
	}

	public long count() {
		return (Long)getEntityManager().createQuery("select count(T) from " + clazz.getName() + " T").getSingleResult();
	}

	public long count(CriteriaQuery criteriaQuery) {
		return (Long)getEntityManager().createQuery(criteriaQuery).getSingleResult();
	}

	private long count(Map<String, Object> criterias, boolean anyMatch) {
		CriteriaBuilder builder = getBuilder();
		CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);
		Root<T> root = criteriaQuery.from(clazz);
		criteriaQuery.select(builder.count(root));
		Expression<Boolean> whereCondition;
		if(criterias != null) {
			if (criterias.size() > 1) {
				List<Predicate> predicates = new ArrayList<>();
				criterias.forEach((k, v) -> {
					predicates.add(builder.equal(root.get(k), v));
				});
				Predicate[] matches = predicates.toArray(new Predicate[0]);
				whereCondition = anyMatch ? builder.or(matches) : builder.and(matches);
			} else {
				Map.Entry<String, Object> entry = criterias.entrySet().iterator().next();
				whereCondition = builder.equal(root.get(entry.getKey()), entry.getValue());
			}
			criteriaQuery.where(whereCondition);
		}
		return getEntityManager().createQuery(criteriaQuery).getSingleResult();
	}

	public long countByAllMatch(Map<String, Object> criterias) {
		return count(criterias, false);
	}

	public long countByAnyMatch(Map<String, Object> criterias) {
		return count(criterias, true);
	}

	private CriteriaBuilder getBuilder() {
		return getEntityManager().getCriteriaBuilder();
	}

	public Object first(CriteriaQuery criteriaQuery) {
		Query query = getEntityManager().createQuery(criteriaQuery);
		try {
			query.setMaxResults(1);
			return query.getSingleResult();
		} catch(NoResultException e) {
			return null;
		}
	}

	public Object getOne(CriteriaQuery criteriaQuery) {
		Query query = getEntityManager().createQuery(criteriaQuery);
		try {
			return query.getSingleResult();
		} catch(NoResultException e) {
			return null;
		}
	}

	public List getAll(CriteriaQuery criteriaQuery, Long offset, Integer limit) {
		Query query = getEntityManager().createQuery(criteriaQuery);
		if (limit != null && limit != -1) {
			query.setMaxResults(limit);
			query.setFirstResult(offset.intValue());
		}
		return query.getResultList();
	}

	public WhereBuilder<T> query() {
		CriteriaBuilder builder = getBuilder();
		return new WhereBuilder<>(this, builder);
	}

	public List<Object> executeQuery(String query, long offset, int perPage, KV<String, Object>... params) {
		return executeQuery(query, offset, perPage, Arrays.stream(params).collect(Collectors.toMap(kv -> kv.key, kv -> kv.value)));
	}

	public List<Object> executeQuery(String query, long offset, int perPage, Map<String, Object> params) {
		Query query1 = getEntityManager().createQuery(query);
		params.forEach((k, v) -> query1.setParameter(k, v));
		if(perPage != -1) {
			query1.setFirstResult((int)offset);
			query1.setMaxResults(perPage);
		}
		return query1.getResultList();
	}

	private CriteriaQuery<T> prepareQuery(Map<String, Object> criterias, boolean allMatch) {
		CriteriaBuilder builder = getBuilder();
		CriteriaQuery<T> criteriaQuery = builder.createQuery(clazz);
		Root<T> root = criteriaQuery.from(clazz);
		criteriaQuery.select(root);
		Expression<Boolean> whereCondition;
		if(criterias != null) {
			if (criterias.size() > 1) {
				List<Predicate> predicates = new ArrayList<>();
				criterias.forEach((k, v) -> {
					if(v == null) {
						predicates.add(builder.isNull(root.get(k)));
					} else {
						predicates.add(builder.equal(root.get(k), v));
					}
				});
				Predicate[] matches = predicates.toArray(new Predicate[0]);
				whereCondition = allMatch ? builder.and(matches) : builder.or(matches);
			} else {
				Map.Entry<String, Object> entry = criterias.entrySet().iterator().next();
				whereCondition = builder.equal(root.get(entry.getKey()), entry.getValue());
			}
			criteriaQuery.where(whereCondition);
		}
		return criteriaQuery;
	}

	private CriteriaQuery<T> prepareQueryAllMatch(Map<String, Object> criterias) {
		return prepareQuery(criterias, true);
	}

	private CriteriaQuery<T> prepareQueryAnyMatch(Map<String, Object> criterias) {
		return prepareQuery(criterias, false);
	}

	public T findOneByAllMatches(Map<String, Object> criterias) {
		return (T) getOne(prepareQueryAllMatch(criterias));
	}

	public T findOneByAllMatches(KV<String, Object>... criteria) {
		Map<String, Object> criterias = new HashMap<>();
		Arrays.stream(criteria).forEach(kv -> criterias.put(kv.key, kv.value));
		return (T) getOne(prepareQueryAllMatch(criterias));
	}

	public List<T> findAllByAllMatches(Map<String, Object> criterias) {
		return findAllByAllMatches(criterias, 0L, -1, null, null);
	}

	public List<T> findAllByAllMatches(Map<String, Object> criterias, Long offset, Integer limit, String order, String dir) {
		CriteriaQuery<T> criteriaQuery = prepareQueryAllMatch(criterias);
		if(order != null) {
			Root<T> root = (Root<T>)criteriaQuery.getRoots().iterator().next();
			criteriaQuery.orderBy(new OrderImpl(root.get(order), dir.equals("asc")));
		}
		return getAll(criteriaQuery, offset, limit);
	}

	public List<T> findAllByAnyMatches(Map<String, Object> criterias, Long offset, Integer limit, String order, String dir) {
		CriteriaQuery<T> criteriaQuery = prepareQueryAnyMatch(criterias);
		if(order != null) {
			Root<T> root = (Root<T>)criteriaQuery.getSelection();
			criteriaQuery.orderBy(new OrderImpl(root.get(order), dir.equals("asc")));
		}
		return getAll(criteriaQuery, offset, limit);
	}

	@Transactional
	public boolean update(String hql, KV<String, Object>... params) {
		Query query = getEntityManager().createQuery(hql);
		for(KV<String, Object> kv : params) {
			query.setParameter(kv.key, kv.value);
		}
		return query.executeUpdate() > 0;
	}

	public T update(T entity) {
		return getEntityManager().merge(entity);
	}

	@Transactional
	public boolean updateSql(String sql, KV<String, Object>... params) {
		Query query = getEntityManager().createNativeQuery(sql);
		for(KV<String, Object> kv : params) {
			query.setParameter(kv.key, kv.value);
		}
		return query.executeUpdate() > 0;
	}

	public void delete(T entity) {
		getEntityManager().remove(entity);
	}

	public void deleteAll(Iterable<? extends T> entities) {}

	public void deleteAll() {}

	public void deleteById(long entityId) {
		T entity = findOne(entityId);
		delete(entity);
	}

	public T proxy(Long id) {
		return getEntityManager().getReference(clazz, id);
	}

	@Transactional(readOnly = true)
	public T refresh(T entity) {
		getEntityManager().refresh(entity);
		return entity;
	}

	@Transactional(readOnly = true)
	public T attach(T entity) {
		return getEntityManager().merge(entity);
	}

	@Transactional(readOnly = true)
	public void setInSession(LazyInitializer initializer) {
		initializer.setSession(getEntityManager().unwrap(SharedSessionContractImplementor.class));
	}

	public void initialize(AbstractPersistentCollection collection) {
		SharedSessionContractImplementor session = getEntityManager().unwrap(SharedSessionContractImplementor.class);
		collection.setCurrentSession(session);
		boolean isJTA = session.getTransactionCoordinator().getTransactionCoordinatorBuilder().isJta();
		if ( !isJTA ) {
			( (Session) session ).beginTransaction();
		}
		try {
			session.getPersistenceContextInternal().addUninitializedDetachedCollection(session.getFactory().getCollectionPersister(collection.getRole()), collection);
			session.initializeCollection(collection, false);
		} finally {
			if ( !isJTA ) {
				( (Session) session ).getTransaction().rollback();
			}
		}
	}
}
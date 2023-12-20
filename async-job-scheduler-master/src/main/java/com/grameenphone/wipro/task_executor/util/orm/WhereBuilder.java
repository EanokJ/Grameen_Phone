package com.grameenphone.wipro.task_executor.util.orm;

import com.grameenphone.wipro.task_executor.repository.CrudDao;
import org.hibernate.query.criteria.internal.OrderImpl;

import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.FetchParent;
import javax.persistence.criteria.From;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import javax.persistence.criteria.Subquery;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

public class WhereBuilder<T> {
	protected CrudDao<T> dao;
	protected CriteriaBuilder builder;
	protected AbstractQuery query;
	protected Root<T> root;
	protected WhereBuilder<T> upLevel;
	protected boolean applicable = true;
	protected List<Predicate> predicates = new ArrayList<>();
	protected Map<String, FetchParent> fetches = new LinkedHashMap<>();

	public interface SafeValue<X> {
		X value();
	}

	public WhereBuilder(CrudDao<T> dao, CriteriaBuilder builder) {
		this.dao = dao;
		this.builder = builder;
		query = builder.createQuery(Object.class);
		root = query.from(dao.getClazz());
	}

	protected WhereBuilder() {}

	private class OrAndClosure<T> extends WhereBuilder<T> {
		private boolean isAnd;

		public OrAndClosure(WhereBuilder upLevel, boolean isAnd) {
			dao = upLevel.dao;
			this.builder = upLevel.builder;
			query = upLevel.query;
			root = upLevel.root;
			this.upLevel = upLevel;
			this.isAnd = isAnd;
		}

		public WhereBuilder<T> close() {
			if(predicates.size() == 1) {
				upLevel.predicates.add(predicates.get(0));
			} else if(predicates.size() > 1) {
				Predicate predicate;
				Predicate[] levelPredecates = predicates.toArray(new Predicate[] {});
				if(isAnd) {
					predicate = builder.and(levelPredecates);
				} else {
					predicate = builder.or(levelPredecates);
				}
				upLevel.predicates.add(predicate);
			}
			return upLevel;
		}
	}

	private class SubWhereBuilder<U> extends WhereBuilder<U> {
		private WhereBuilder<T> containerBuilder;

		public SubWhereBuilder(WhereBuilder<T> containerBuilder, Class<U> newSubClass) {
			query = containerBuilder.query.subquery(Long.class);
			root = query.from(newSubClass);
			this.containerBuilder = containerBuilder;
			builder = containerBuilder.builder;
			((Subquery)query).select(builder.count(root));
		}

		public WhereBuilder ex() {
			finalizeWhere();
			containerBuilder.predicates.add(builder.gt((Subquery)query, 0));
			return containerBuilder;
		}

		public WhereBuilder ne() {
			finalizeWhere();
			containerBuilder.predicates.add(builder.equal((Subquery)query, 0));
			return containerBuilder;
		}

		protected <E> Expression<E> fieldExpression(String field) {
			if(field.startsWith(":root")) {
				return super.fieldExpression(field.substring(6), containerBuilder.root);
			} else {
				return super.fieldExpression(field);
			}
		}
	}

	/**
	 * starts a new block in which all conditions will have 'or' operator between them
	 * @param applicable
	 * @return
	 */
	public WhereBuilder<T> orif(BooleanSupplier applicable) {
		OrAndClosure level = (OrAndClosure)or();
		level.applicable = this.applicable && applicable.getAsBoolean();
		return level;
	}

	public WhereBuilder<T> or() {
		OrAndClosure<T> subBuilder = new OrAndClosure<T>(this, false);
		if(!applicable) {
			subBuilder.applicable = false;
		}
		return subBuilder;
	}

	/**
	 * starts a new block in which all conditions will have 'and' operator between them
	 * @param applicable
	 * @return
	 */
	public WhereBuilder<T> andif(BooleanSupplier applicable) {
		OrAndClosure level = (OrAndClosure) and();
		level.applicable = this.applicable && applicable.getAsBoolean();
		return level;
	}

	public WhereBuilder<T> and() {
		OrAndClosure<T> subBuilder = new OrAndClosure<T>(this, true);
		if(!applicable) {
			subBuilder.applicable = false;
		}
		return subBuilder;
	}

	public WhereBuilder<T> eqif(BooleanSupplier applicable, String field, Object value) {
		return eqif(applicable, field, () -> value);
	}

	public WhereBuilder<T> eqif(BooleanSupplier applicable, String field, SafeValue value) {
		if(this.applicable && applicable.getAsBoolean()) {
			eq(field, value);
		}
		return this;
	}

	public WhereBuilder<T> eq(String field, Object value) {
		return eq(field, () -> value);
	}

	public WhereBuilder<T> eq(String field, SafeValue value) {
		if(applicable) {
			Object v = value.value();
			if(v == null) {
				predicates.add(builder.isNull(fieldExpression(field)));
			} else {
				predicates.add(builder.equal(fieldExpression(field), v));
			}
		}
		return this;
	}

	public WhereBuilder<T> eqf(String field1, String field2) {
		if(applicable) {
			predicates.add(builder.equal(fieldExpression(field1), fieldExpression(field2)));
		}
		return this;
	}

	public <U> WhereBuilder<T> sub(Class<U> entityClass) {
		return new SubWhereBuilder(this, entityClass);
	}

	public WhereBuilder<T> eqsub(String field, Object v) {
		if(applicable) {
			Subquery sub = query.subquery(Long.class);
			Root subRoot = sub.from(root.getModel());
			String[] multiPart = field.split("\\.");
			int partCount = multiPart.length;
			From currentJoin = subRoot;
			for (int h = 0; h < partCount - 1; h++) {
				currentJoin = currentJoin.join(multiPart[h], JoinType.INNER);
			}
			sub.select(builder.count(subRoot));
			Path lastPredicate = currentJoin.get(multiPart[multiPart.length - 1]);
			sub.where(builder.equal(root.get("id"), subRoot.get("id")), v == null ? builder.isNull(lastPredicate) : builder.equal(lastPredicate, v));
			predicates.add(builder.gt(sub, 0));
		}
		return this;
	}

	public WhereBuilder<T> insub(String field, Collection<Object> v) {
		if(v.size() == 0) {
			predicates.add(builder.equal(builder.literal(1), 0));
		} else if(applicable) {
			Subquery sub = query.subquery(Long.class);
			Root subRoot = sub.from(root.getModel());
			String[] multiPart = field.split("\\.");
			int partCount = multiPart.length;
			From currentJoin = subRoot;
			for (int h = 0; h < partCount - 1; h++) {
				currentJoin = currentJoin.join(multiPart[h], JoinType.INNER);
			}
			sub.select(builder.count(subRoot));
			Path lastPredicate = currentJoin.get(multiPart[multiPart.length - 1]);
			sub.where(builder.equal(root.get("id"), subRoot.get("id")), lastPredicate.in(v));
			predicates.add(builder.gt(sub, 0));
		}
		return this;
	}

	public WhereBuilder<T> neif(BooleanSupplier applicable, String field, Object value) {
		return neif(applicable, field, () -> value);
	}

	public WhereBuilder<T> neif(BooleanSupplier applicable, String field, SafeValue value) {
		if(this.applicable && applicable.getAsBoolean()) {
			ne(field, value);
		}
		return this;
	}

	public WhereBuilder<T> ne(String field, Object value) {
		return ne(field, () -> value);
	}

	public WhereBuilder<T> ne(String field, SafeValue value) {
		if(applicable) {
			Object v = value.value();
			if(v == null) {
				predicates.add(builder.isNotNull(fieldExpression(field)));
			} else {
				predicates.add(builder.notEqual(fieldExpression(field), v));
			}
		}
		return this;
	}

	/**
	 * like
	 * @param applicable
	 * @param field
	 * @param pattern
	 * @return
	 */
	public WhereBuilder<T> lkif(BooleanSupplier applicable, String field, String pattern) {
		return lkif(applicable, field, () -> pattern);
	}

	public WhereBuilder<T> lkif(BooleanSupplier applicable, String field, SafeValue<String> pattern) {
		if(this.applicable && applicable.getAsBoolean()) {
			lk(field, pattern);
		}
		return this;
	}

	public WhereBuilder<T> lk(String field, String pattern) {
		return lk(field, () -> pattern);
	}

	public WhereBuilder<T> lk(String field, SafeValue<String> pattern) {
		if(!this.applicable) {
			return this;
		}
		predicates.add(builder.like(fieldExpression(field), pattern.value()));
		return this;
	}

	/**
	 * greaterOrEqual
	 * @param applicable
	 * @param field
	 * @param value
	 * @return
	 */
	public WhereBuilder<T> geif(BooleanSupplier applicable, String field, Object value) {
		return geif(applicable, field, () -> value);
	}

	public WhereBuilder<T> geif(BooleanSupplier applicable, String field, SafeValue value) {
		if(this.applicable && applicable.getAsBoolean()) {
			ge(field, value);
		}
		return this;
	}

	public WhereBuilder<T> ge(String field, Object value) {
		return ge(field, () -> value);
	}

	public WhereBuilder<T> ge(String field, SafeValue value) {
		if(applicable) {
			predicates.add(builder.greaterThanOrEqualTo(fieldExpression(field), (Comparable)value.value()));
		}
		return this;
	}

	/**
	 * greaterOrEqual
	 * @param applicable
	 * @param field
	 * @param value
	 * @return
	 */
	public WhereBuilder<T> gtif(BooleanSupplier applicable, String field, Object value) {
		return gtif(applicable, field, () -> value);
	}

	public WhereBuilder<T> gtif(BooleanSupplier applicable, String field, SafeValue value) {
		if(this.applicable && applicable.getAsBoolean()) {
			gt(field, value);
		}
		return this;
	}

	public WhereBuilder<T> gt(String field, Object value) {
		return gt(field, () -> value);
	}

	public WhereBuilder<T> gt(String field, SafeValue value) {
		if(applicable) {
			predicates.add(builder.greaterThan(fieldExpression(field), (Comparable)value.value()));
		}
		return this;
	}

	/**
	 * lessOrEqual
	 * @param applicable
	 * @param field
	 * @param value
	 * @return
	 */
	public WhereBuilder<T> leif(BooleanSupplier applicable, String field, Object value) {
		return leif(applicable, field, () -> value);
	}

	public WhereBuilder<T> leif(BooleanSupplier applicable, String field, SafeValue value) {
		if(this.applicable && applicable.getAsBoolean()) {
			le(field, value);
		}
		return this;
	}

	public WhereBuilder<T> le(String field, Object value) {
		return le(field, () -> value);
	}

	public WhereBuilder<T> le(String field, SafeValue value) {
		if(applicable) {
			predicates.add(builder.lessThanOrEqualTo(fieldExpression(field), (Comparable)value.value()));
		}
		return this;
	}

	/**
	 * isEmpty
	 * @param applicable
	 * @param field
	 * @return
	 */
	public WhereBuilder<T> emif(BooleanSupplier applicable, String field) {
		if(this.applicable && applicable.getAsBoolean()) {
			em(field);
		}
		return this;
	}

	public WhereBuilder<T> em(String field) {
		if(applicable) {
			predicates.add(builder.isEmpty(fieldExpression(field)));
		}
		return this;
	}

	/**
	 * isNotNull
	 * @param applicable
	 * @param field
	 * @return
	 */
	public WhereBuilder<T> nnif(BooleanSupplier applicable, String field) {
		if(this.applicable && applicable.getAsBoolean()) {
			nn(field);
		}
		return this;
	}

	public WhereBuilder<T> nn(String field) {
		if(applicable) {
			predicates.add(builder.isNotNull(fieldExpression(field)));
		}
		return this;
	}

	/**
	 * is null
	 * @param applicable
	 * @param field
	 * @return
	 */
	public WhereBuilder<T> nlif(BooleanSupplier applicable, String field) {
		if(this.applicable && applicable.getAsBoolean()) {
			nl(field);
		}
		return this;
	}

	/**
	 * is null
	 * @param field
	 * @return
	 */
	public WhereBuilder<T> nl(String field) {
		if(applicable) {
			predicates.add(builder.isNull(fieldExpression(field)));
		}
		return this;
	}

	/**
	 * Not Like
	 * @param applicable
	 * @param field
	 * @return
	 */
	public WhereBuilder<T> nlif(BooleanSupplier applicable, String field, String pattern) {
		return nlif(applicable, field, () -> pattern);
	}

	public WhereBuilder<T> nlif(BooleanSupplier applicable, String field, SafeValue<String> pattern) {
		if(this.applicable && applicable.getAsBoolean()) {
			nl(field, pattern);
		}
		return this;
	}

	/**
	 * not like
	 * @param field
	 * @param pattern
	 * @return
	 */
	public WhereBuilder<T> nl(String field, String pattern) {
		return nl(field, () -> pattern);
	}

	public WhereBuilder<T> nl(String field, SafeValue<String> pattern) {
		if(applicable) {
			predicates.add(builder.notLike(fieldExpression(field), pattern.value()));
		}
		return this;
	}

	/**
	 * isTrue
	 * @param applicable
	 * @param field
	 * @return
	 */
	public WhereBuilder<T> trif(BooleanSupplier applicable, String field) {
		if(this.applicable && applicable.getAsBoolean()) {
			tr(field);
		}
		return this;
	}

	public WhereBuilder<T> tr(String field) {
		if(applicable) {
			predicates.add(builder.isTrue(fieldExpression(field)));
		}
		return this;
	}

	public enum Existance {
		ALL,
		ANY,
		NOT_ANY,
		NONE
	}

	private Existance negateExistance(Existance existance) {
		switch(existance) {
			case ALL:
				return Existance.NOT_ANY;
			case ANY:
				return Existance.NONE;
			case NOT_ANY:
				return Existance.ALL;
		}
		return Existance.ANY;
	}

	/**
	 * isMember
	 * @param applicable
	 * @param field
	 * @param value
	 * @return
	 */
	public WhereBuilder<T> mrif(BooleanSupplier applicable, String field, Object value) {
		return mrif(applicable, field, () -> value, Existance.ALL);
	}

	public WhereBuilder<T> mrif(BooleanSupplier applicable, String field, SafeValue value) {
		if(this.applicable && applicable.getAsBoolean()) {
			mr(field, value, Existance.ALL);
		}
		return this;
	}

	public WhereBuilder<T> mr(String field, Object value) {
		return mr(field, () -> value, Existance.ALL);
	}

	public WhereBuilder<T> mr(String field, SafeValue value) {
		return mr(field, value, Existance.ALL);
	}

	public WhereBuilder<T> mrif(BooleanSupplier applicable, String field, Object value, Existance existance) {
		return mrif(applicable, field, () -> value, existance);
	}

	public WhereBuilder<T> mrif(BooleanSupplier applicable, String field, SafeValue value, Existance existance) {
		if(this.applicable && applicable.getAsBoolean()) {
			mr(field, value, existance);
		}
		return this;
	}

	public WhereBuilder<T> mr(String field, Object value, Existance existance) {
		return mr(field, () -> value, existance);
	}

	public WhereBuilder<T> mr(String field, SafeValue value, Existance existance) {
		if(applicable) {
			Object _value = value.value();
			if(_value instanceof Collection) {
				int collectionSize = ((Collection)_value).size();
				if(collectionSize == 0) {
					return this;
				} else if(collectionSize == 1) {
					_value = ((Collection)_value).iterator().next();
				} else {
					memberCountChecker(field, (Collection)_value, existance);
					return this;
				}
			}
			predicates.add(builder.isMember(_value, fieldExpression(field)));
		}
		return this;
	}

	/**
	 * isNotMember
	 * @param applicable
	 * @param field
	 * @param value
	 * @return
	 */
	public WhereBuilder<T> nmif(BooleanSupplier applicable, String field, Object value) {
		return nmif(applicable, field, () -> value, Existance.ALL);
	}

	public WhereBuilder<T> nmif(BooleanSupplier applicable, String field, SafeValue value) {
		if(this.applicable && applicable.getAsBoolean()) {
			nm(field, value, Existance.ALL);
		}
		return this;
	}

	public WhereBuilder<T> nm(String field, Object value) {
		return nm(field, () -> value, Existance.ALL);
	}

	public WhereBuilder<T> nm(String field, SafeValue value) {
		return nm(field, value, Existance.ALL);
	}

	public WhereBuilder<T> nmif(BooleanSupplier applicable, String field, Object value, Existance existance) {
		return nmif(applicable, field, () -> value, existance);
	}

	public WhereBuilder<T> nmif(BooleanSupplier applicable, String field, SafeValue value, Existance existance) {
		if(this.applicable && applicable.getAsBoolean()) {
			nm(field, value, existance);
		}
		return this;
	}

	public WhereBuilder<T> nm(String field, Object value, Existance existance) {
		return nm(field, () -> value, existance);
	}

	public WhereBuilder<T> nm(String field, SafeValue value, Existance existance) {
		if(applicable) {
			Object _value = value.value();
			if(_value instanceof Collection) {
				int collectionSize = ((Collection)_value).size();
				if(collectionSize == 0) {
					return this;
				} else if(collectionSize == 1) {
					_value = ((Collection)_value).iterator().next();
				} else {
					memberCountChecker(field, (Collection)_value, negateExistance(existance));
					return this;
				}
			}
			predicates.add(builder.isNotMember(_value, fieldExpression(field)));
		}
		return this;
	}

	private void memberCountChecker(String field, Collection _value, Existance existance) {
		Subquery sub = query.subquery(Long.class);
		Root subRoot = sub.from(root.getModel());
		String[] multiPart = field.split("\\.");
		int partCount = multiPart.length;
		From currentJoin = subRoot;
		for (int h = 1; h <= partCount; h++) {
			if (h == partCount) {
				currentJoin = currentJoin.joinCollection(multiPart[h - 1], JoinType.INNER);
			} else if (h == 1) {
				currentJoin = currentJoin.join(multiPart[h - 1], JoinType.INNER);
			}
		}
		sub.select(builder.count(subRoot));
		sub.where(builder.equal(root.get("id"), subRoot.get("id")), currentJoin.in(_value));
		int expectedCount = existance == Existance.ALL ? _value.size() : (existance == Existance.NONE ? 0 : -1);
		if(expectedCount != -1) {
			predicates.add(builder.equal(sub, expectedCount));
		} else if(existance == Existance.ANY) {
			predicates.add(builder.gt(sub, 0));
		} else {
			predicates.add(builder.lessThanOrEqualTo(sub, _value.size()));
		}
	}

	/**
	 * Checking of member with like matching
	 * @param applicable
	 * @param field
	 * @param value
	 * @return
	 */
	public WhereBuilder<T> mlif(BooleanSupplier applicable, String field, Object value) {
		return mlif(applicable, field, () -> value);
	}

	public WhereBuilder<T> mlif(BooleanSupplier applicable, String field, SafeValue value) {
		if(this.applicable && applicable.getAsBoolean()) {
			ml(field, value);
		}
		return this;
	}

	public WhereBuilder<T> ml(String field, String value) {
		return ml(field, () -> value);
	}

	public WhereBuilder<T> ml(String field, SafeValue<String> value) {
		if(applicable) {
			Subquery<String> subquery = query.subquery(String.class);
			Root subRoot = subquery.from(dao.getClazz());
			subquery.where(builder.and(builder.like(subRoot.join(field, JoinType.INNER), value.value()), builder.equal(root.get("id"), subRoot.get("id"))));
			subquery.select(subRoot);
			predicates.add(builder.exists(subquery));
		}
		return this;
	}

	/**
	 * completes current 'or' or 'and' block and sets enclosure block as current block.
	 * do nothing if current block is top block
	 * @return
	 */
	public WhereBuilder<T> close() {
		throw new QueryBuilderException("No parenthesized condition available");
	}

	/**
	 * to be implemented by subquery to check whether that exists or not
	 * @return
	 */
	public WhereBuilder<T> ex() {
		throw new QueryBuilderException("Exists is applicable for subquery only");
	}

	/**
	 * to be implemented by subquery to check whether that not exists or not
	 * @return
	 */
	public WhereBuilder<T> ne() {
		throw new QueryBuilderException("Not Exists is applicable for subquery only");
	}

	/**
	 * inList
	 * @param applicable
	 * @param field
	 * @param value
	 * @return
	 */
	public WhereBuilder<T> inif(BooleanSupplier applicable, String field, Collection value) {
		return inif(applicable, field, () -> value);
	}

	public WhereBuilder<T> inif(BooleanSupplier applicable, String field, SafeValue<Collection> value) {
		if(applicable.getAsBoolean()) {
			in(field, value);
		}
		return this;
	}

	public WhereBuilder<T> in(String field, SafeValue<Collection> value) {
		if(applicable) {
			predicates.add(fieldExpression(field).in(value.value()));
		}
		return this;
	}

	public WhereBuilder<T> in(String field, Collection value) {
		in(field, () -> value);
		return this;
	}

	public WhereBuilder<T> distinct() {
		query.distinct(true);
		return this;
	}

	private WhereBuilder<T> select(String... fields) {
		if(this instanceof OrAndClosure || this instanceof SubWhereBuilder) {
			throw new QueryBuilderException("Query can not be finalized at this level. May be some parenthesized condition yet not closed");
		}
		CriteriaQuery _query = (CriteriaQuery)query;
		if(fields.length == 0) {
			_query.select(root);
		} else if(fields.length > 1) {
			List<Selection> selections = new ArrayList<>();
			for(String field : fields) {
				selections.add(fieldExpression(field));
			}
			_query.multiselect(selections);
		} else {
			_query.select(fieldExpression(fields[0]));
		}
		return this;
	}

	private void setFetch(String field, FetchParent fetch, List<String> tobePart, JoinType type) {
		FetchParent[] fetchFinal = new FetchParent[] {fetch};
		String[] fieldFinal = new String[] {field};
		tobePart.forEach(f -> {
			fetchFinal[0] = fetchFinal[0].fetch(f, type);
			fieldFinal[0] = fieldFinal[0] + "." + f;
			fetches.put(fieldFinal[0], fetchFinal[0]);
		});
	}

	public WhereBuilder<T> fetch(String field, JoinType type) {
		FetchParent f = fetches.get("." + field);
		if(f != null) {
			return this;
		}
		List<String> tobePart = new ArrayList<>();
		while(f == null) {
			int lastDot = field.lastIndexOf('.');
			String fieldStr;
			if(lastDot == -1) {
				fieldStr = field;
			} else {
				fieldStr = field.substring(lastDot + 1);
			}
			tobePart.add(0, fieldStr);
			if(lastDot == -1) {
				field = "";
				break;
			}
			field = field.substring(0, lastDot);
			f = fetches.get("." + field);
		}
		if(tobePart.size() > 0) {
			setFetch(field, f == null ? root : f, tobePart, type);
		}
		return this;
	}

	public WhereBuilder<T> order(String order, String dir) {
		if(this instanceof OrAndClosure || this instanceof SubWhereBuilder) {
			throw new QueryBuilderException("Order can not be set at this level. May be some parenthesized condition yet not closed");
		}
		CriteriaQuery _query = (CriteriaQuery)query;
		_query.orderBy(new OrderImpl(fieldExpression(order), dir.equals("asc")));
		return this;
	}

	public List<T> list() {
		return list(null, null, null, null);
	}

	public List<T> list(Long offset, Integer limit) {
		return list(offset, limit, null, null);
	}

	public List<T> list(String order, String dir) {
		return list(null, null, order, dir);
	}

	public List<T> list(Long offset, Integer limit, String order, String dir) {
		if(this instanceof OrAndClosure || this instanceof SubWhereBuilder) {
			throw new QueryBuilderException("Query can not be finalized at this level. May be some parenthesized condition yet not closed");
		}
		CriteriaQuery _query = (CriteriaQuery)query;
		_query.select(root);
		finalizeWhere();
		if(order != null) {
			_query.orderBy(new OrderImpl(fieldExpression(order), dir.equals("asc")));
		}
		return dao.getAll(_query, offset, limit);
	}

	/**
	 * Applicable for multiple projection
	 * @param fields
	 * @return
	 */
	public List<Object[]> selectAll(String... fields) {
		return selectAll(null, null, fields);
	}

	public List<Object[]> selectAll(Long offset, Integer limit, String... fields) {
		select(fields);
		finalizeWhere();
		CriteriaQuery _query = (CriteriaQuery)query;
		return (List<Object[]>)dao.getAll(_query, offset, limit);
	}

	public <R> List<R> selectAll(Long offset, Integer limit, Class<R> selectClass, String... fields) {
		if(this instanceof OrAndClosure || this instanceof SubWhereBuilder) {
			throw new QueryBuilderException("Query can not be finalized at this level. May be some parenthesized condition yet not closed");
		}
		CriteriaQuery _query = (CriteriaQuery)query;
		_query.select(builder.construct(selectClass, Arrays.stream(fields).map(field -> (Selection)fieldExpression(field)).toArray(x -> new Selection[x])));
		finalizeWhere();
		return (List<R>)dao.getAll(_query, offset, limit);
	}

	public <R> List<R> selectAll(Class<R> selectClass, String... fields) {
		return selectAll(null, null, selectClass, fields);
	}

	public Object[] selectOne(String... fields) {
		List<Object[]> data = selectAll(0L, 1, fields);
		if(data.size() == 0) {
			return null;
		}
		return data.get(0);
	}

	public Object selectOne(String field) {
		List<Object> data = (List<Object>)(List)selectAll(0L, 1, new String[] {field}); //As only one field so return value will not be an array
		if(data.size() == 0) {
			return null;
		}
		return data.get(0);
	}

	public static class KeyValue<X, Y> {
		public X key;
		public Y value;

		public KeyValue(X key, Y value) {
			this.key = key;
			this.value = value;
		}
	}

	public <A, B> Map<A, Set<B>> toMultiValueMap(Function<Object[], KeyValue<A, B>> converter, String... fields) {
		List<Object[]> mapData = selectAll(fields);
		LinkedHashMap<A, Set<B>> map = new LinkedHashMap<>();
		if(mapData.size() == 0) {
			return map;
		}
		mapData.forEach((k) -> {
			KeyValue<A, B> kk = converter.apply(k);
			Set<B> value = map.get(kk.key);
			if(value == null) {
				map.put(kk.key, value = new LinkedHashSet<>());
			}
			if(kk.value != null) {
				value.add(kk.value);
			}
		});
		return map;
	}

	public T findOne() {
		if(this instanceof OrAndClosure || this instanceof SubWhereBuilder) {
			throw new QueryBuilderException("Query can not be finalized at this level. May be some parenthesized condition yet not closed");
		}
		CriteriaQuery _query = (CriteriaQuery)query;
		_query.select(root);
		finalizeWhere();
		return (T)dao.getOne(_query);
	}

	public T first() {
		if(this instanceof OrAndClosure || this instanceof SubWhereBuilder) {
			throw new QueryBuilderException("Query can not be finalized at this level. May be some parenthesized condition yet not closed");
		}
		CriteriaQuery _query = (CriteriaQuery)query;
		_query.select(root);
		finalizeWhere();
		return (T)dao.first(_query);
	}

	protected void finalizeWhere() {
		if(predicates.size() > 0) {
			query.where(predicates.toArray(new Predicate[0]));
		}
	}

	protected <E> Expression<E> fieldExpression(String field, Root root) {
		if(field.equals("*")) {
			return (Expression<E>)root;
		} else if(field.contains(" as ")) {
			String[] splits = field.split("\\s+as\\s+");
			Class<E> targetClazz;
			try {
				targetClazz = (Class<E>)Class.forName("java.lang." + splits[1].trim());
			} catch (Exception k) {
				throw new IllegalArgumentException("Invalid target class: " + splits[1]);
			}
			return fieldExpression(splits[0]).as(targetClazz);
		} else if(field.contains(".")) {
			String[] splits = field.split("\\.");
			Path path = root;
			for (String split : splits) {
				path = path.get(split);
			}
			return path;
		} else if(field.contains("(")) {
			String[] splits = field.split("[\\(\\)]");
			String functionName = splits[0].trim();
			try {
				Method method = CriteriaBuilder.class.getDeclaredMethod(functionName, Expression.class);
				return (Expression<E>)method.invoke(builder, fieldExpression(splits[1].trim()));
			} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
				throw new QueryBuilderException(e.getMessage());
			}
		}
		return root.get(field);
	}

	protected <E> Expression<E> fieldExpression(String field) {
		return fieldExpression(field, root);
	}

	public Number avg(String field) {
		if(this instanceof OrAndClosure || this instanceof SubWhereBuilder) {
			throw new QueryBuilderException("Query can not be finalized at this level. May be some parenthesized condition yet not closed");
		}
		CriteriaQuery _query = (CriteriaQuery)query;
		_query.select(builder.avg(fieldExpression(field)));
		finalizeWhere();
		return (Number) dao.getOne(_query);
	}

	public Number sum(String field) {
		if(this instanceof OrAndClosure || this instanceof SubWhereBuilder) {
			throw new QueryBuilderException("Query can not be finalized at this level. May be some parenthesized condition yet not closed");
		}
		CriteriaQuery _query = (CriteriaQuery)query;
		_query.select(builder.sum(fieldExpression(field)));
		finalizeWhere();
		return (Number) dao.getOne(_query);
	}

	public Comparable max(String field) {
		if(this instanceof OrAndClosure || this instanceof SubWhereBuilder) {
			throw new QueryBuilderException("Query can not be finalized at this level. May be some parenthesized condition yet not closed");
		}
		CriteriaQuery _query = (CriteriaQuery)query;
		_query.select(builder.max(fieldExpression(field)));
		finalizeWhere();
		return (Comparable) dao.getOne(_query);
	}

	public Comparable min(String field) {
		if(this instanceof OrAndClosure || this instanceof SubWhereBuilder) {
			throw new QueryBuilderException("Query can not be finalized at this level. May be some parenthesized condition yet not closed");
		}
		CriteriaQuery _query = (CriteriaQuery)query;
		_query.select(builder.min(fieldExpression(field)));
		finalizeWhere();
		return (Comparable) dao.getOne(_query);
	}

	public Comparable greatest(String field) {
		if(this instanceof OrAndClosure || this instanceof SubWhereBuilder) {
			throw new QueryBuilderException("Query can not be finalized at this level. May be some parenthesized condition yet not closed");
		}
		CriteriaQuery _query = (CriteriaQuery)query;
		_query.select(builder.greatest((Expression) fieldExpression(field)));
		finalizeWhere();
		return (Comparable) dao.getOne(_query);
	}

	public Comparable least(String field) {
		if(this instanceof OrAndClosure || this instanceof SubWhereBuilder) {
			throw new QueryBuilderException("Query can not be finalized at this level. May be some parenthesized condition yet not closed");
		}
		CriteriaQuery _query = (CriteriaQuery)query;
		_query.select(builder.least((Expression) fieldExpression(field)));
		finalizeWhere();
		return (Comparable) dao.getOne(_query);
	}

	public Long count() {
		if(this instanceof OrAndClosure || this instanceof SubWhereBuilder) {
			throw new QueryBuilderException("Query can not be finalized at this level. May be some parenthesized condition yet not closed");
		}
		CriteriaQuery _query = (CriteriaQuery)query;
		_query.select(builder.count(root));
		finalizeWhere();
		return dao.count(_query);
	}

	public Long count(String field, boolean isCollection) {
		if(this instanceof OrAndClosure || this instanceof SubWhereBuilder) {
			throw new QueryBuilderException("Query can not be finalized at this level. May be some parenthesized condition yet not closed");
		}
		CriteriaQuery _query = (CriteriaQuery)query;
		_query.select(isCollection ? builder.size(root.get(field)) : builder.count(fieldExpression(field)));
		finalizeWhere();
		Object x = dao.getOne(_query);
		if(x instanceof Integer) {
			return ((Integer) x).longValue();
		}
		return (Long)x;
	}

	public Number count(String field) {
		return count(field, false);
	}

	public boolean exists() {
		return count() > 0;
	}

	public Number countDistinct(String field) {
		if(this instanceof OrAndClosure || this instanceof SubWhereBuilder) {
			throw new QueryBuilderException("Query can not be finalized at this level. May be some parenthesized condition yet not closed");
		}
		CriteriaQuery _query = (CriteriaQuery)query;
		_query.select(builder.countDistinct(fieldExpression(field)));
		finalizeWhere();
		return (Number)dao.getOne(_query);
	}
}
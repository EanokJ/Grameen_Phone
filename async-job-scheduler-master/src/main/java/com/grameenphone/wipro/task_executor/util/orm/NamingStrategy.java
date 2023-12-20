package com.grameenphone.wipro.task_executor.util.orm;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitJoinTableNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * Custom hibernate naming strategy to convert camel case identifiers to underscore case
 * configured in application.yml
 */
public class NamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl {
	public Identifier determineJoinTableName(ImplicitJoinTableNameSource source) {
		String name = source.getOwningPhysicalTableName() + "_"
				+ source.getAssociationOwningAttributePath().getProperty();
		return toIdentifier(name, source.getBuildingContext());
	}

	@Override
	protected Identifier toIdentifier(String stringForm, MetadataBuildingContext buildingContext) {
		return convertToSnakeCase(super.toIdentifier(stringForm, buildingContext));
	}

	private Identifier convertToSnakeCase(final Identifier identifier) {
		final String regex = "([^A-Z])([A-Z])";
		final String replacement = "$1_$2";
		final String newName = identifier.getText()
				.replaceAll(regex, replacement)
				.toLowerCase();
		return Identifier.toIdentifier(newName);
	}
}
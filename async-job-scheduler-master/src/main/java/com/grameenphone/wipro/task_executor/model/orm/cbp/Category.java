package com.grameenphone.wipro.task_executor.model.orm.cbp;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import java.util.Collection;

/**
 * @author wipro.tribhuwan
 */
@Entity
public class Category {
	@Id
	@GeneratedValue(
			strategy = GenerationType.AUTO,
			generator = "native"
	)
	@GenericGenerator(
			name = "native",
			strategy = "native"
	)
    private long id;
    
    private String name;
    
    private String code;

	@OneToMany
	@JoinColumn(name = "category_id")
    private Collection<Company> companies;

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Collection<Company> getCompanies() {
		return companies;
	}

	public void setCompanies(Collection<Company> companies) {
		this.companies = companies;
	}
}
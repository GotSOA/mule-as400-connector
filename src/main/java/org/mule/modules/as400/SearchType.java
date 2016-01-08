package org.mule.modules.as400;

import java.util.EnumSet;

/**
 * AS400 Connector
 *
 * @author ravenugo
 */
public enum SearchType {

	EQUAL("EQ", "equal"), NOT_EQUAL("NE", "not equal"), LESS_THAN("LT",
			"less than"), LESS_THAN_OR_EQUAL("LE", "less than or equal"), GREATER_THAN(
			"GT", "greater than"), GREATER_THAN_OR_EQUAL("GE",
			"greater than or equal");

	private String searchType;
	private String description;

	SearchType(String searchType, String description) {
		this.searchType = searchType;
		this.description = description;
	}

	/**
	 * @return the searchType
	 */
	public String getSearchType() {
		return searchType;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return description;
	}

	public static SearchType getByValue(String value) {
		for (final SearchType element : EnumSet.allOf(SearchType.class)) {
			if (element.toString().equals(value)) {
				return element;
			}
		}
		return null;
	}

}

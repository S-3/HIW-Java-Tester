package hiw.api.test;

import java.util.List;

import org.json.JSONObject;

/**
 * The outermost part of a Filter.
 */
public class Filter extends FilterGroup {
	/**
	 * The page of data to return.
	 */
	public int Page = 1;
	
	public Filter() {
		super();
	}
	
	public Filter(List<FilterCriterion> criteria) {
		super(criteria);
	}
	
	public Filter(FilterType type, List<FilterCriterion> criteria) {
		super(criteria);
		this.Type = type;
	}
	
	public Filter(int page) {
		this.Page = page;
	}
	
	public Filter(int page, List<FilterCriterion> criteria) {
		this(criteria);
		this.Page = page;
	}
	
	/**
	 * Sets the page of data to return.
	 * @return This instance.
	 */
	public Filter setPage(Integer page) {
		this.Page = page;
		return this;
	}
	
	/**
	 * Adds a criterion to this group.
	 * @return This instance.
	 */
	public Filter addCriterion(FilterCriterion criterion) {
		super.addCriterion(criterion);
		return this;
	}

	/**
	 * Adds a criterion to this group.
	 * @return This instance.
	 */
	public Filter addCriterion(String name, FilterOperator operator) {
		super.addCriterion(name, operator, null);
		return this;
	}

	/**
	 * Adds a criterion to this group.
	 * @return This instance.
	 */
	public Filter addCriterion(String name, FilterOperator operator, Object value) {
		super.addCriterion(new FilterCriterion(name, operator, value));
		return this;
	}

	/**
	 * Convert this instance to JSON.
	 */
	public JSONObject toJSON() throws Exception {
		JSONObject json = super.toJSON();
		
		//Remove __type for the outer-most filter object.
		json.remove("__type");
		
		return json;
	}
}

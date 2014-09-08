package hiw.api.test;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Represents a group of filter criterion.
 */
public class FilterGroup extends FilterCriterion {
	public FilterType Type = FilterType.And;
	public List<FilterCriterion> Criteria = new ArrayList<FilterCriterion>();
	
	public FilterGroup() {}
	
	public FilterGroup(List<FilterCriterion> criteria) {
		this.Criteria = criteria;
	}
	
	public FilterGroup(FilterType type, List<FilterCriterion> criteria) {
		this(criteria);
		this.Type = type;
	}
	
	/**
	 * Adds a criterion to this group.
	 * @return This instance.
	 */
	public FilterGroup addCriterion(FilterCriterion criterion) {
		this.Criteria.add(criterion);
		
		return this;
	}

	/**
	 * Adds a criterion to this group.
	 * @return This instance.
	 */
	public FilterGroup addCriterion(String name, FilterOperator operator) {
		return addCriterion(name, operator, null);
	}

	/**
	 * Adds a criterion to this group.
	 * @return This instance.
	 */
	public FilterGroup addCriterion(String name, FilterOperator operator, Object value) {
		return addCriterion(new FilterCriterion(name, operator, value));
	}

	/**
	 * Convert this instance to JSON.
	 */
	public JSONObject toJSON() throws Exception {
		JSONObject json = new JSONObject();
		JSONArray criteria = new JSONArray();
		
		//General properties.
		json
			.put("__type", "SearchGroup:#S3.Common.Search")
			.put("Mode", this.Type.toString())
			.put("Elements", criteria);
		
		//Convert each criterion.
		for (FilterCriterion c : this.Criteria) {
			criteria.put(c.toJSON());
		}
		
		return json;
	}
}

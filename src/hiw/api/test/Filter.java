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
	
	public Filter(int page, List<FilterCriterion> criteria) {
		this(criteria);
		this.Page = page;
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

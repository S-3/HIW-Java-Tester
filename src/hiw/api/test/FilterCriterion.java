package hiw.api.test;

import org.json.JSONObject;

/**
 * Represents a single criterion.
 */
public class FilterCriterion {
	/**
	 * The name of the parameter upon which to apply this filter.
	 */
	public String Name = null;
	
	/**
	 * The operator to use.
	 */
	public FilterOperator Operator = null;
	
	/**
	 * The value to use.
	 */
	public Object Value = null;
	
	public FilterCriterion() {}

	public FilterCriterion(String name, FilterOperator operator) {
		this(name, operator, null);
	}
	
	public FilterCriterion(String name, FilterOperator operator, Object value) {
		this.Name = name;
		this.Operator = operator;
		this.Value = value;
	}

	/**
	 * Convert this instance to JSON.
	 */
	public JSONObject toJSON() throws Exception {
		//Treat "in" and "not in" operators different.
		if (this.Operator == FilterOperator.In || this.Operator == FilterOperator.NotIn) {
			if (!(this.Value instanceof Object[]))
				throw new Exception("Value must of an Object array when filtering using 'in' or 'not in' operators.");
			
			//Create a new FilterGroup and "or" or "and" each value together.
			FilterGroup group = new FilterGroup();
			Object[] values = (Object[])this.Value;
			
			group.Type = (this.Operator == FilterOperator.In ? FilterType.Or : FilterType.And);
			
			//Add each value as a criterion.
			for (Object o : values)
				group.addCriterion(this.Name, FilterOperator.Equal, o);
			
			return group.toJSON();
		}
		else {
			JSONObject json = new JSONObject();

			//General properties.
			json
				.put("__type", "SearchParameter:#S3.Common.Search")
				.put("Name", this.Name)
				.put("Operator", this.Operator.toString());
			
			//Only include the value if the operator supports a value.
			if (this.Operator != FilterOperator.Null && this.Operator != FilterOperator.NotNull)
				json.put("Value", this.Value);
			
			return json;
		}
	}
}

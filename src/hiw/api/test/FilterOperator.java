package hiw.api.test;

/**
 * The valid set of operators we can use to filter an API call.
 */
public enum FilterOperator {
	LessThan,
	LessThanOrEqual, 
	Equal, 
	NotEqual, 
	GreaterThanOrEqual, 
	GreaterThan, 
	Null, 
	NotNull,
	In,
	NotIn
}

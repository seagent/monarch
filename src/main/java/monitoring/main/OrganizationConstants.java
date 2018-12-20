package monitoring.main;

public class OrganizationConstants {

	/**
	 * TODO: Sorgu federated yapılacak
	 */
	public static String STOCK_QUERY_TEMPLATE = "SELECT ?sameCompany ?count ?stock WHERE {"
			+ "<%s> <http://www.w3.org/2002/07/owl#sameAs> ?sameCompany. "
			+ "?sameCompany <http://data.nytimes.com/elements/associated_article_count> ?count."
			+ "?sameCompany <http://stockmarket.com/elements/stockValue> ?value}";

	public static String RESOURCE_PATH = "src/main/resources/";
	public static String VOID_PATH = RESOURCE_PATH + "void/";

}

package monitoring.main;

public class OrganizationData {

	private String dbpediaCompany;
	private String nytimesCompany;
	private String articleCount;

	public OrganizationData(String dbpediaCompany, String nytimesCompany, String articleCount) {
		super();
		this.dbpediaCompany = dbpediaCompany;
		this.nytimesCompany = nytimesCompany;
		this.articleCount = articleCount;
	}

	public String getDbpediaCompany() {
		return dbpediaCompany;
	}

	public String getNytimesCompany() {
		return nytimesCompany;
	}

	public String getArticleCount() {
		return articleCount;
	}

}

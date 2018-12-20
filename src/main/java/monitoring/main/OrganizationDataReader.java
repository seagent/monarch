package monitoring.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class OrganizationDataReader {

	private static final String SPACE = " ";

	public static List<OrganizationData> readOrganizationData(String fileName) throws URISyntaxException, IOException {
		List<OrganizationData> organizationDataList = new ArrayList<OrganizationData>();

		InputStream is = OrganizationDataReader.class.getResourceAsStream(fileName);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));

		String line;
		while ((line = br.readLine()) != null) {
			String[] pieces = line.split(SPACE);
			if (pieces != null && pieces.length == 3) {
				organizationDataList.add(new OrganizationData(pieces[0], pieces[1], pieces[2]));
			}

		}

		return organizationDataList;
	}
}

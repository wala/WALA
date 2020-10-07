package javaonepointseven;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Linghui Luo
 * @see <a href="https://docs.oracle.com/javase/7/docs/technotes/guides/language/try-with-resources.html">Java 7 docs</a>
 */
public class TryWithResourcesStatement {
	
	public static void main(String[] args) throws IOException, SQLException {
		readFirstLineFromFile(args[0]);
		viewTable(null);
		TryWithResourcesStatement x = new TryWithResourcesStatement();
	}
	
  public static String readFirstLineFromFile(String path) throws IOException {
    try (BufferedReader br = new BufferedReader(new FileReader(path))) {
      return br.readLine();
    }
  }

  public static void viewTable(Connection con) throws SQLException {
    String query = "select COF_NAME, SUP_ID, PRICE, SALES, TOTAL from COFFEES";
    try (Statement stmt = con.createStatement()) {
      ResultSet rs = stmt.executeQuery(query);

      while (rs.next()) {
        String coffeeName = rs.getString("COF_NAME");
        int supplierID = rs.getInt("SUP_ID");
        float price = rs.getFloat("PRICE");
        int sales = rs.getInt("SALES");
        int total = rs.getInt("TOTAL");
        System.out.println(
            coffeeName + ", " + supplierID + ", " + price + ", " + sales + ", " + total);
      }

      rs.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}

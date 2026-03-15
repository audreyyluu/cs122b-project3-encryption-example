import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.jasypt.util.password.StrongPasswordEncryptor;

public class VerifyPassword {

	/*
	 * After you update the passwords in customers table,
	 *   you can use this program as an example to verify the password.
	 *   
	 * Verify the password is simple:
	 * success = new StrongPasswordEncryptor().checkPassword(password, encryptedPassword);
	 *
	 * Note that you need to use the same StrongPasswordEncryptor when encrypting the passwords
	 *
	 */
	public static void main(String[] args) throws Exception {

		System.out.println(verifyCredentials("a@email.com", "a2"));
		System.out.println(verifyCredentials("a@email.com", "a3"));

	}

	private static boolean verifyCredentials(String email, String password) throws Exception {
		
		String loginUser = System.getenv().getOrDefault("DB_USER", System.getProperty("db.user", "mytestuser"));
		String loginPasswd = System.getenv().getOrDefault("DB_PASS", System.getProperty("db.pass", "My6$Password"));
		String dbHost = System.getenv().getOrDefault("DB_HOST", System.getProperty("db.host", "mysql-primary"));
		String dbPort = System.getenv().getOrDefault("DB_PORT", System.getProperty("db.port", "3306"));
		String dbName = System.getenv().getOrDefault("DB_NAME", System.getProperty("db.name", "moviedb"));

		String loginUrl = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
				dbHost, dbPort, dbName);

		System.out.println("Using JDBC URL: " + loginUrl);

		Class.forName("com.mysql.cj.jdbc.Driver");
		// try-with-resources
		try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
			 Statement statement = connection.createStatement()) {

			String query = String.format("SELECT * from customers where email='%s'", email);

			ResultSet rs = statement.executeQuery(query);

			boolean success = false;
			if (rs.next()) {
			    // get the encrypted password from the database
				String encryptedPassword = rs.getString("password");

				// use the same encryptor to compare the user input password with encrypted password stored in DB
				success = new StrongPasswordEncryptor().checkPassword(password, encryptedPassword);
			}

			rs.close();

			System.out.println("verify " + email + " - " + password);

			return success;
		}
	}

}

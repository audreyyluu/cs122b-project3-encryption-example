import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;

import org.jasypt.util.password.PasswordEncryptor;
import org.jasypt.util.password.StrongPasswordEncryptor;

public class UpdateSecurePassword {

  /*
   *
   * This program updates your existing moviedb customers and employees table to change the
   * plain text passwords to encrypted passwords.
   *
   * You should only run this program **once**, because this program uses the
   * existing passwords as real passwords, then replace them. If you run it more
   * than once, it will treat the encrypted passwords as real passwords and
   * generate wrong values.
   *
   */
  public static void main(String[] args) throws Exception {

    // allow overriding connection settings via environment variables or system properties
    String loginUser = System.getenv().getOrDefault("DB_USER", System.getProperty("db.user", "mytestuser"));
    String loginPasswd = System.getenv().getOrDefault("DB_PASS", System.getProperty("db.pass", "My6$Password"));
    String dbHost = System.getenv().getOrDefault("DB_HOST", System.getProperty("db.host", "mysql-primary"));
    String dbPort = System.getenv().getOrDefault("DB_PORT", System.getProperty("db.port", "3306"));
    String dbName = System.getenv().getOrDefault("DB_NAME", System.getProperty("db.name", "moviedb"));

    // Build JDBC URL. When running from outside the cluster, you can set DB_HOST to localhost after kubectl port-forward.
    String loginUrl = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
        dbHost, dbPort, dbName);

    System.out.println("Using JDBC URL: " + loginUrl);
    System.out.println("DB user: " + loginUser);

    Class.forName("com.mysql.cj.jdbc.Driver");

    // Use try-with-resources so resources are always closed
    try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd);
         Statement statement = connection.createStatement()) {

        // we use the StrongPasswordEncryptor from jasypt library (Java Simplified Encryption)
        //  it internally use SHA-256 algorithm and 10,000 iterations to calculate the encrypted password
        PasswordEncryptor passwordEncryptor = new StrongPasswordEncryptor();

        // ---- Process customers table (primary key: id) ----
        System.out.println("encrypting customers passwords (this might take a while)");

        String custQuery = "SELECT id, password FROM customers";
        ResultSet rsCust = statement.executeQuery(custQuery);

        PreparedStatement updateCustStmt = connection.prepareStatement("UPDATE customers SET password=? WHERE id=?");

        int custCount = 0;
        while (rsCust.next()) {
          Object id = rsCust.getObject("id");
          String password = rsCust.getString("password");

          String encryptedPassword = passwordEncryptor.encryptPassword(password);

          updateCustStmt.setString(1, encryptedPassword);
          // use setObject to handle numeric or string id types
          updateCustStmt.setObject(2, id);
          int updateResult = updateCustStmt.executeUpdate();
          custCount += updateResult;
        }
        rsCust.close();
        updateCustStmt.close();

        System.out.println("updating customers password completed, " + custCount + " rows affected");

        // ---- Process employees table (primary key: email) ----
        System.out.println("encrypting employees passwords (this might take a while)");

        String empQuery = "SELECT email, password FROM employees";
        ResultSet rsEmp = statement.executeQuery(empQuery);

        PreparedStatement updateEmpStmt = connection.prepareStatement("UPDATE employees SET password=? WHERE email=?");

        int empCount = 0;
        while (rsEmp.next()) {
          String email = rsEmp.getString("email");
          String password = rsEmp.getString("password");

          String encryptedPassword = passwordEncryptor.encryptPassword(password);

          updateEmpStmt.setString(1, encryptedPassword);
          updateEmpStmt.setString(2, email);
          int updateResult = updateEmpStmt.executeUpdate();
          empCount += updateResult;
        }
        rsEmp.close();
        updateEmpStmt.close();

        System.out.println("updating employees password completed, " + empCount + " rows affected");

    }

    System.out.println("finished");

  }

}

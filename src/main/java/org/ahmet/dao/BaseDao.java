package org.ahmet.dao;

import org.ahmet.config.DatabaseConfig;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for all DAO implementations.
 * Provides common database operations and resource management.
 */
public abstract class BaseDao {
    private static final Logger LOGGER = Logger.getLogger(BaseDao.class.getName());
    protected final DataSource dataSource;

    protected BaseDao() {
        this.dataSource = DatabaseConfig.getDataSource();
    }

    /**
     * Gets a database connection from the connection pool.
     * @return Database connection
     * @throws SQLException if connection cannot be obtained
     */
    protected Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Safely closes database resources.
     * @param connection Database connection
     * @param statement Prepared statement
     * @param resultSet Result set
     */
    protected void closeResources(Connection connection, PreparedStatement statement, ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing ResultSet", e);
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing PreparedStatement", e);
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing Connection", e);
            }
        }
    }

    /**
     * Executes an update query (INSERT, UPDATE, DELETE).
     * @param sql SQL query
     * @param parameters Query parameters
     * @return Number of affected rows
     * @throws SQLException if query execution fails
     */
    protected int executeUpdate(String sql, Object... parameters) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(sql);
            setParameters(statement, parameters);
            return statement.executeUpdate();
        } finally {
            closeResources(connection, statement, null);
        }
    }

    /**
     * Sets parameters for a prepared statement.
     * @param statement Prepared statement
     * @param parameters Parameters to set
     * @throws SQLException if parameter setting fails
     */
    protected void setParameters(PreparedStatement statement, Object... parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
    }
}
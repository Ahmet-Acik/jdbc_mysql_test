#!/bin/bash

# Development setup script for JDBC MySQL Test project
# This script sets up the development environment including Docker MySQL

echo "Setting up development environment for JDBC MySQL Test..."

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "Error: Docker is required but not installed. Please install Docker first."
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo "Error: Docker Compose is required but not installed. Please install Docker Compose first."
    exit 1
fi

# Create Docker Compose file for MySQL
cat > docker-compose.yml << EOF
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    container_name: jdbc-mysql-test-db
    environment:
      MYSQL_ROOT_PASSWORD: root7623
      MYSQL_DATABASE: testdb
      MYSQL_USER: testuser
      MYSQL_PASSWORD: testpass
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./init-scripts:/docker-entrypoint-initdb.d
    command: --default-authentication-plugin=mysql_native_password
    restart: unless-stopped

volumes:
  mysql_data:
EOF

# Create init scripts directory
mkdir -p init-scripts

# Create initialization script
cat > init-scripts/01-init.sql << EOF
-- Create additional databases for testing if needed
CREATE DATABASE IF NOT EXISTS testdb_integration;
GRANT ALL PRIVILEGES ON testdb_integration.* TO 'testuser'@'%';

-- Ensure proper privileges
FLUSH PRIVILEGES;
EOF

# Create environment variables file template
cat > .env.template << EOF
# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=testdb
DB_USER=root
DB_PASSWORD=root7623

# Connection Pool Settings
DB_POOL_SIZE=10
DB_CONNECTION_TIMEOUT=30000
DB_IDLE_TIMEOUT=600000
DB_MAX_LIFETIME=1800000
EOF

# Copy template to actual .env file if it doesn't exist
if [ ! -f .env ]; then
    cp .env.template .env
    echo "Created .env file from template. Please review and update as needed."
fi

# Start MySQL container
echo "Starting MySQL container..."
docker-compose up -d mysql

# Wait for MySQL to be ready
echo "Waiting for MySQL to be ready..."
sleep 10

# Check if MySQL is responding
until docker exec jdbc-mysql-test-db mysql -uroot -proot7623 -e "SELECT 1" &> /dev/null; do
    echo "Waiting for MySQL connection..."
    sleep 2
done

echo "MySQL is ready!"

# Run Maven clean and compile
echo "Building project..."
mvn clean compile

echo ""
echo "Development environment setup complete!"
echo ""
echo "Next steps:"
echo "1. Review and update .env file with preferred settings"
echo "2. Run 'mvn test' to execute unit tests"
echo "3. Run 'mvn exec:java -Dexec.mainClass=org.ahmet.Main' to start the application"
echo "4. Use 'docker-compose down' to stop the MySQL container when done"
echo ""
echo "MySQL container details:"
echo "- Container name: jdbc-mysql-test-db"
echo "- Port: 3306"
echo "- Root password: root7623"
echo "- Database: testdb"
echo "- Connect with: mysql -h localhost -P 3306 -u root -p"
# VS Code Java 21 Setup Guide

## Running the Application from VS Code

Your project is now configured to run with Java 21 directly from VS Code. Here are the available options:

### 1. 🚀 Quick Run (Recommended)
1. Open `src/main/java/org/ahmet/Main.java` in VS Code
2. Look for the **"Run | Debug"** CodeLens above the `main` method
3. Click **"Run"** to execute the application
4. Click **"Debug"** to run in debug mode with breakpoints

### 2. 🎯 Launch Configuration
- Press `F5` or `Ctrl+F5` (macOS: `Cmd+F5`) to run
- Or use **Command Palette** (`Cmd+Shift+P` / `Ctrl+Shift+P`):
  - Type "Java: Run" or "Java: Debug"
  - Select "Run Main (JDBC MySQL Test)"

### 3. 📋 Tasks Menu
- Press `Cmd+Shift+P` (macOS) or `Ctrl+Shift+P` (Windows/Linux)
- Type "Tasks: Run Task"
- Select:
  - **"Maven: Clean Compile"** - Compile the project
  - **"Maven: Package"** - Build JAR file
  - **"Maven: Run Main"** - Run the application

### 4. 🔧 Terminal Integration
- Open VS Code integrated terminal (`Ctrl+`` ` or `View → Terminal`)
- Run: `mvn exec:java -Dexec.mainClass="org.ahmet.Main"`

## Environment Configuration

The application requires database environment variables. Default values are configured in `.vscode/launch.json`:

```
DB_HOST=localhost
DB_PORT=3306
DB_NAME=testdb
DB_USER=root
DB_PASSWORD=
DB_POOL_SIZE=10
```

To customize:
1. Copy `.env.example` to `.env`
2. Modify the values in `.env`
3. The launch configuration will use these values

## Java Features Available

With Java 21, you can now use:
- Virtual Threads
- Pattern Matching for switch
- Record Patterns
- String Templates (Preview)
- Sequenced Collections

## Troubleshooting

If you see "No Java runtime found" or similar errors:
1. Ensure Java 21 is installed: `java -version`
2. Install the "Extension Pack for Java" in VS Code
3. Reload VS Code window: `Cmd+R` (macOS) or `Ctrl+R` (Windows/Linux)

## Verification

✅ Project compiles with Java 21  
✅ VS Code launch configurations ready  
✅ Maven tasks configured  
✅ Debug support enabled  
✅ Environment variables set up  

Your Java 21 project is ready to run from VS Code! 🎉
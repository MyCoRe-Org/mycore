# MyCoRe Repository - Copilot Instructions

## Repository Overview

**MyCoRe** (My Content Repository) is an open source repository software framework for building disciplinary or institutional repositories, digital archives, digital libraries, and scientific journals. The project provides core functionality for metadata management, querying, OAI harvesting interface, and an image viewer module.

- **Project Type**: Maven multi-module Java project with TypeScript/Vue.js frontend components
- **Size**: ~2,200 Java files, ~79,000+ lines of Java code, 46 modules
- **License**: GNU General Public License v3
- **Primary Language**: Java 21
- **Frontend**: TypeScript, Vue.js 3, Less, Vite
- **Build Tool**: Maven 3.9+
- **Package Manager**: Yarn (for frontend)

## Build Requirements

### Runtime Versions
- **Java**: 21 (Temurin distribution recommended)
- **Maven**: 3.9.11 or higher
- **Node.js**: v20.11.0 (managed by frontend-maven-plugin)
- **Yarn**: Managed by frontend-maven-plugin

### Setting Java Version
**ALWAYS set Java 21 before any Maven command**:
```bash
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
java -version  # Verify Java 21 is active
```

## Build and Test Instructions

### Full Build (with Tests)
**Primary build command used in CI** - this is the most reliable way to build:
```bash
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
mvn -B -Plocal-testing,!standard-with-extra-repos install -T1C -DreuseForks=true -DforkCount=1
```

**Time**: Expect 10-15 minutes for a full build with tests.

**Profile Explanation**:
- `-Plocal-testing`: Activates local testing profile (used in CI)
- `-P!standard-with-extra-repos`: Disables extra repository profile
- `-T1C`: Uses 1 thread per CPU core for parallel builds
- `-DreuseForks=true -DforkCount=1`: Optimizes test execution

### Build Without Tests
```bash
mvn clean install -DskipTests -T1C
```

### Run Tests Only
```bash
mvn test
```

### Generate Javadoc
```bash
mvn -P!standard-with-extra-repos -B javadoc:javadoc javadoc:test-javadoc -T1C
```

### Clean Build Artifacts
```bash
mvn clean
```

## Code Quality Tools

### Checkstyle (Java Code Style)
**Configuration**: `checkstyle.xml` and `checkstyle-suppressions.xml` in root
- **Line length**: 120 characters max
- **Enforcement**: Failures WILL break the build (`checkstyle.failOnViolation=true`)
- Run checkstyle:
```bash
mvn checkstyle:check
```

### PMD (Static Analysis)
**Configuration**: `ruleset.xml` and `rules.xml` in root
- **Enforcement**: Failures WILL break the build (`pmd.failOnViolation=true`)
- Run PMD:
```bash
mvn pmd:check
```

### TSLint (TypeScript)
**Configuration**: `tslint.json` and `tslint-microsoft.json` in root
- Used for TypeScript validation in viewer and frontend modules
- Extends Microsoft TypeScript guidelines

### Running All Quality Checks
```bash
mvn verify
```

## Project Structure

### Module Organization
The project uses Maven multi-module structure with 46 modules:

**Core Modules**:
- `mycore-base`: Base components and core functionality
- `mycore-bom`: Bill of Materials for dependency management

**Key Feature Modules**:
- `mycore-viewer`: Image/document viewer (TypeScript, Vite, Vue.js)
- `mycore-restapi`: REST API implementation
- `mycore-solr`: Apache Solr integration
- `mycore-iiif`: IIIF image API support
- `mycore-oai`: OAI-PMH harvesting
- `mycore-mets`: METS/MODS metadata support

**Frontend Modules with Node.js/TypeScript**:
- `mycore-viewer`, `mycore-mets`, `mycore-wcms2`, `mycore-acl-editor2`, `mycore-webcli`, `mycore-classeditor`
- Use frontend-maven-plugin for Node.js/Yarn builds
- Vue.js components in `src/main/vue/` directories

### Directory Layout
```
mycore/
├── .github/
│   └── workflows/
│       └── ci.yml              # GitHub Actions CI workflow
├── mycore-*/                   # 46 feature modules
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/          # Java source code
│   │   │   ├── resources/     # Configuration, XSL, properties
│   │   │   └── vue/           # Vue.js components (some modules)
│   │   └── test/
│   │       ├── java/          # JUnit 5 tests
│   │       └── resources/     # Test resources
│   ├── pom.xml                # Module POM
│   └── package.json           # Node.js config (frontend modules)
├── src/
│   ├── changes/               # Release notes
│   └── site/                  # Maven site documentation
├── pom.xml                    # Parent POM
├── checkstyle.xml             # Checkstyle rules
├── checkstyle-suppressions.xml
├── ruleset.xml                # PMD rules
├── rules.xml                  # Additional PMD rules
└── tslint.json                # TypeScript linting
```

## Testing

### Test Framework
- **JUnit 5** for Java tests
- **Selenium 4.35.0** for web UI tests (Firefox/Gecko driver)
- **Mockito 5.20.0** for mocking

### Test Execution in CI
The CI workflow sets up Firefox for Selenium tests:
```bash
export FIREFOX_BIN=$(which firefox)
export SELENIUM_BROWSER=firefox
```

### Running Specific Module Tests
```bash
cd mycore-{module-name}
mvn test
```

## CI/CD Pipeline

### GitHub Actions Workflow
**File**: `.github/workflows/ci.yml`

**Triggers**: Push and pull requests

**Key Steps**:
1. Checkout code
2. Setup JDK 21 (Temurin)
3. Setup Firefox/Geckodriver for Selenium tests
4. Build and test with Maven (10-15 min)
5. Generate Javadoc
6. Upload test results/logs on failure

**Test Artifacts on Failure**:
- `**/surefire-reports` - Unit test results
- `**/failsafe-reports` - Integration test results
- `**/screenshots` - Selenium test screenshots
- `**/*error*.log` - Error logs
- `**/*test.log` - Test logs

## Common Build Issues and Workarounds

### Issue: Parent POM Not Found
**Symptom**: `Non-resolvable parent POM for org.mycore:mycore:2025.12.0-SNAPSHOT`
**Cause**: Network restrictions or missing parent POM in repository cache
**Solution**: Build with network access enabled or use CI profile

### Issue: TypeScript Compilation Errors
**Cause**: Node.js/Yarn not properly initialized by frontend-maven-plugin
**Solution**: Clean and rebuild:
```bash
mvn clean
rm -rf node_modules package-lock.json
mvn install
```

### Issue: Selenium Tests Fail
**Cause**: Missing Firefox or Geckodriver
**Solution**: Install Firefox and set environment variables:
```bash
export FIREFOX_BIN=$(which firefox)
export SELENIUM_BROWSER=firefox
```

### Issue: Out of Memory During Build
**Solution**: Increase Maven memory:
```bash
export MAVEN_OPTS="-Xmx4g -XX:MaxPermSize=512m"
```

## Working with Frontend Code

### Frontend Modules
Modules with `package.json` use Node.js build process integrated via `frontend-maven-plugin`.

### Manual Frontend Development
For modules like `mycore-viewer`:
```bash
cd mycore-viewer
yarn install
yarn typecheck    # TypeScript type checking
yarn build        # Build for production
```

### Frontend Build Integration
Maven automatically:
1. Downloads Node.js v20.11.0 and Yarn
2. Runs `yarn install`
3. Executes build scripts (Vite, TypeScript, etc.)
4. Packages output into JAR

## Code Conventions

### Commit Message Format
```
{JIRA-Ticket} {Ticket summary} {Commit summary}
```
Example: `MCR-1393 Git-Migration add .travis.yml`

### Branch Naming
```
issues/{JIRA-Ticket}-{Ticket_Summary}
```
Example: `issues/MCR-1393-Git_Migration`

### Code Style
- Follow [MyCoRe Code Style](https://www.mycore.de/documentation/developer/codestyle/)
- Follow [MyCoRe Naming Conventions](https://www.mycore.de/documentation/developer/conventions/)
- Line length: 120 characters maximum
- Use 4 spaces for indentation (Java)

## Key Configuration Files

- **Maven**: `pom.xml` (root and each module)
- **Checkstyle**: `checkstyle.xml`, `checkstyle-suppressions.xml`
- **PMD**: `ruleset.xml`, `rules.xml`
- **TypeScript**: `tslint.json`, `tslint-microsoft.json`
- **Git**: `.gitignore` (excludes target/, node_modules/, .idea/, etc.)

## Dependencies

### Key Java Dependencies
- Hibernate 6.6.13.Final (ORM)
- Jersey 3.1.10 (REST framework)
- Jackson 2.19.0 (JSON)
- Apache Solr (search)
- Log4j 2.24.3 (logging)
- JUnit 5.12.2 (testing)

### Key Frontend Dependencies
- Vue.js 3.5+
- TypeScript 5.2+
- Vite 5.4+ or 6.0+
- Bootstrap 5.3+

## Important Notes

1. **ALWAYS use Java 21** - Set JAVA_HOME before any Maven command
2. **Use CI profiles** - Build with `-Plocal-testing,!standard-with-extra-repos` for consistency with CI
3. **Code quality is enforced** - Checkstyle and PMD failures will break builds
4. **Parallel builds recommended** - Use `-T1C` for faster builds
5. **Frontend builds are integrated** - Maven handles Node.js/Yarn automatically
6. **Test artifacts location** - Check `target/surefire-reports` and `target/failsafe-reports` for test results
7. **Clean builds** - Run `mvn clean` if experiencing build cache issues

## Trust These Instructions

These instructions are comprehensive and tested. Only search for additional information if:
- These instructions are incomplete for your specific task
- You encounter an error not documented here
- You need module-specific implementation details

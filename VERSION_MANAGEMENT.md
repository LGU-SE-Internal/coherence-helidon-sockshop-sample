# Version Management Guide

This document explains the centralized version management system for the Coherence Helidon Sock Shop project.

## Overview

The version management has been centralized to eliminate the need for manual find-and-replace across multiple files when updating versions. All version information is now controlled from a few key locations.

## Architecture

## Architecture

### Maven POMs

**Root POM** (`pom.xml`):
- Defines the project version: `<version>2.11.0</version>`
- Acts as parent POM for all child modules
- Imports Helidon BOM for dependency management
- All child modules inherit the version automatically

**Child Module POMs** (carts, catalog, orders, payment, shipping, users):
- Declare root POM as parent: `<parent>...</parent>`
- Inherit version from parent automatically (no version tag needed)
- No need to define version property locally
- Simply update the root POM version to update all modules

**Note**: Child modules now inherit from the root POM (not directly from Helidon). The root POM imports the Helidon BOM in its dependencyManagement section, providing all Helidon dependencies while enabling true version inheritance.

### Build Script

**build-and-push.sh**:
- Dynamically extracts version from root `pom.xml` using Maven help:evaluate
- Uses `grep -F` for fixed-string matching (no regex interpretation)
- No longer hardcodes version numbers in grep commands
- Automatically adapts to version changes

### Helm Charts

**values.yaml**:
- Defines `global.imageTag: "2.11.0"` for all backend services
- Backend services (carts, catalog, orders, payment, shipping, users) have empty tags: `tag: ""`
- Frontend and loadgen can specify custom tags if needed

**_helpers.tpl**:
- Contains fallback logic: if a service's tag is empty, use `global.imageTag`
- Allows per-service overrides when necessary

## How to Update Version

To update the project version from `2.11.0` to a new version (e.g., `2.12.0`):

### Step 1: Update Root POM Version

Update the root POM (`pom.xml`) version:
```xml
<version>2.12.0</version>
```

### Step 2: Update Parent Reference in Child POMs

Update the parent version reference in each child POM (6 files):
```xml
<parent>
    <groupId>com.oracle.coherence.examples.sockshop.helidon</groupId>
    <artifactId>sockshop-coh-parent</artifactId>
    <version>2.12.0</version>  <!-- Update this -->
    <relativePath>../pom.xml</relativePath>
</parent>
```

**Tip**: Use find-and-replace to update all parent references at once:
- Find: `<artifactId>sockshop-coh-parent</artifactId>\n        <version>2.11.0</version>`
- Replace with: `<artifactId>sockshop-coh-parent</artifactId>\n        <version>2.12.0</version>`

This updates the parent reference in all child POMs simultaneously.

### Step 2: Update Helm Chart

Update `helm/sockshop/values.yaml`:
```yaml
global:
  imageTag: "2.12.0"
```

### Step 3: Update Chart App Version (Optional)

If desired, update `helm/sockshop/Chart.yaml`:
```yaml
appVersion: "2.12.0"
```

### Step 4: Verify Changes

Run the following commands to verify:

```bash
# Verify Maven version
mvn help:evaluate -Dexpression=project.version -q -DforceStdout
cd carts && mvn help:evaluate -Dexpression=project.version -q -DforceStdout && cd ..

# Verify Helm renders correctly
helm template sockshop helm/sockshop/ | grep "ss-carts:" | head -1
```

You should see `2.12.0` in all outputs.

## Benefits

1. **Centralized Version Management**: Version defined in root POM, inherited by all children
2. **Simplified Updates**: Change version in 2 places (root POM + parent references) instead of 7+ separate module versions
3. **Automatic Inheritance**: All child modules inherit version from parent
4. **Find-Replace Friendly**: Parent version references can be updated with single find-replace
5. **Dynamic Build Script**: Build script automatically uses current version
6. **Flexible Helm**: Services can override global version if needed
7. **Proper Maven Structure**: Follows Maven best practices for parent-child relationships

## Current Version Locations

The version `2.11.0` is currently defined in:

1. ✅ `pom.xml` (root) - `<version>2.11.0</version>` **← Main definition**
2. ✅ Child POMs parent reference (6 files) - Can be updated with single find-replace
3. ✅ `helm/sockshop/values.yaml` - `imageTag: "2.11.0"`
4. ✅ `helm/sockshop/Chart.yaml` - `appVersion: "2.11.0"` (optional)

Child modules automatically inherit their version from parent - no module version tags!

## Migration from Old System

**Before** (Multiple locations to update):
- Version hardcoded as `<version>2.11.0</version>` in each child POM (7 locations)
- Version hardcoded in build-and-push.sh grep command
- Version hardcoded for each service in Helm values

**After** (Simplified with inheritance):
- Version defined in root POM
- Parent version reference in child POMs (can be updated with find-replace)
- Child modules inherit their version automatically
- Build script extracts version dynamically
- Helm uses global.imageTag with per-service override capability

**Version Update Complexity:**
- Before: Update 9+ files manually with multiple different patterns
- After: Update 2 locations (root POM + parent refs via find-replace) + Helm = simplified!

## Future Improvements

Consider these enhancements for even better version management:

1. **Maven CI Friendly Versions**: Use `${revision}` property with flatten-maven-plugin
2. **Automated Scripts**: Create a script to update version in all necessary files
3. **Git Tagging**: Automatically create git tags matching version numbers
4. **Version Validation**: Add pre-commit hooks to ensure version consistency

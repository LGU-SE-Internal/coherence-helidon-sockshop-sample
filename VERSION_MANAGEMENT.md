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

### Step 1: Update Root POM Only

Update **ONLY** the root POM (`pom.xml`):
```xml
<version>2.12.0</version>
```

That's it! All child modules automatically inherit the new version.

**Important**: Do NOT update child module POMs - they inherit the version automatically from the parent.

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

1. **True Single Source of Truth**: Version is defined only in the root POM
2. **Minimal Updates**: Change version in just ONE place (root pom.xml only!)
3. **Automatic Inheritance**: All child modules inherit version from parent
4. **No Duplication**: No need to update child module POMs
5. **Dynamic Build Script**: Build script automatically uses current version
6. **Flexible Helm**: Services can override global version if needed
7. **Proper Maven Structure**: Follows Maven best practices for parent-child relationships

## Current Version Locations

The version `2.11.0` is currently defined in:

1. ✅ `pom.xml` (root) - `<version>2.11.0</version>` **← ONLY HERE!**
2. ✅ `helm/sockshop/values.yaml` - `imageTag: "2.11.0"`
3. ✅ `helm/sockshop/Chart.yaml` - `appVersion: "2.11.0"` (optional)

Child modules automatically inherit from root POM - no version definitions needed!

## Migration from Old System

**Before** (Multiple locations to update):
- Version hardcoded as `<version>2.11.0</version>` in each child POM (7 locations)
- Version hardcoded in build-and-push.sh grep command
- Version hardcoded for each service in Helm values

**After** (Single location):
- Version defined only in root POM
- Child modules inherit automatically
- Build script extracts version dynamically
- Helm uses global.imageTag with per-service override capability

**Version Update Complexity:**
- Before: Update 9+ files manually
- After: Update 1 file (root POM) + 1 file (Helm values) = 2 files total!

## Future Improvements

Consider these enhancements for even better version management:

1. **Maven CI Friendly Versions**: Use `${revision}` property with flatten-maven-plugin
2. **Automated Scripts**: Create a script to update version in all necessary files
3. **Git Tagging**: Automatically create git tags matching version numbers
4. **Version Validation**: Add pre-commit hooks to ensure version consistency

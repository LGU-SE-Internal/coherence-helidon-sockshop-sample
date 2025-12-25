# Version Management Guide

This document explains the centralized version management system for the Coherence Helidon Sock Shop project.

## Overview

The version management has been centralized to eliminate the need for manual find-and-replace across multiple files when updating versions. All version information is now controlled from a few key locations.

## Architecture

### Maven POMs

**Root POM** (`pom.xml`):
- Defines the `sockshop.version` property: `<sockshop.version>2.11.0</sockshop.version>`
- This property is used by all child modules

**Child Module POMs** (carts, catalog, orders, payment, shipping, users):
- Use `<version>${sockshop.version}</version>` instead of hardcoded versions
- Must also define the `sockshop.version` property locally (inherited from aggregator context)
- Automatically inherit the version from the root POM's property

### Build Script

**build-and-push.sh**:
- Dynamically extracts version from root `pom.xml` using Maven
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

### Step 1: Update Maven POMs

Update the `sockshop.version` property in **all** POM files:

1. Root POM (`pom.xml`):
   ```xml
   <sockshop.version>2.12.0</sockshop.version>
   ```

2. Each child module POM (carts, catalog, orders, payment, shipping, users):
   ```xml
   <properties>
       <sockshop.version>2.12.0</sockshop.version>
       ...
   </properties>
   ```

**Tip**: Use a text editor's find-and-replace feature to change all occurrences of `<sockshop.version>2.11.0</sockshop.version>` to `<sockshop.version>2.12.0</sockshop.version>` across all POM files.

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

1. **Single Source of Truth**: Version is defined in properties, reducing duplication
2. **Easier Updates**: Change version in 8 places (root + 6 children + helm) instead of throughout codebase
3. **Reduced Errors**: Less risk of version mismatch between modules
4. **Dynamic Build Script**: Build script automatically uses current version
5. **Flexible Helm**: Services can override global version if needed

## Current Version Locations

The version `2.11.0` is currently defined in:

1. ✅ `pom.xml` (root) - `<sockshop.version>2.11.0</sockshop.version>`
2. ✅ `carts/pom.xml` - `<sockshop.version>2.11.0</sockshop.version>`
3. ✅ `catalog/pom.xml` - `<sockshop.version>2.11.0</sockshop.version>`
4. ✅ `orders/pom.xml` - `<sockshop.version>2.11.0</sockshop.version>`
5. ✅ `payment/pom.xml` - `<sockshop.version>2.11.0</sockshop.version>`
6. ✅ `shipping/pom.xml` - `<sockshop.version>2.11.0</sockshop.version>`
7. ✅ `users/pom.xml` - `<sockshop.version>2.11.0</sockshop.version>`
8. ✅ `helm/sockshop/values.yaml` - `imageTag: "2.11.0"`
9. ✅ `helm/sockshop/Chart.yaml` - `appVersion: "2.11.0"` (optional)

## Migration from Old System

**Before** (Manual find-and-replace required):
- Version hardcoded as `<version>2.11.0</version>` in each child POM
- Version hardcoded in build-and-push.sh grep command
- Version hardcoded for each service in Helm values

**After** (Property-based):
- Version referenced as `<version>${sockshop.version}</version>` in child POMs
- Build script extracts version dynamically
- Helm uses global.imageTag with per-service override capability

## Future Improvements

Consider these enhancements for even better version management:

1. **Maven CI Friendly Versions**: Use `${revision}` property with flatten-maven-plugin
2. **Automated Scripts**: Create a script to update version in all necessary files
3. **Git Tagging**: Automatically create git tags matching version numbers
4. **Version Validation**: Add pre-commit hooks to ensure version consistency

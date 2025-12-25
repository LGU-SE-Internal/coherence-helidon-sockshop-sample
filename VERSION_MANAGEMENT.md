# Version Management Guide

This document explains the centralized version management system for the Coherence Helidon Sock Shop project.

## Overview

The version management has been centralized to eliminate the need for manual find-and-replace across multiple files when updating versions. All version information is now controlled from a few key locations.

## Architecture

### Maven POMs

**Root POM** (`pom.xml`):
- Defines the `sockshop.version` property: `<sockshop.version>2.11.0</sockshop.version>`
- Serves as an aggregator POM for multi-module builds
- This property is used as a convention across all modules

**Child Module POMs** (carts, catalog, orders, payment, shipping, users):
- Use `<version>${sockshop.version}</version>` instead of hardcoded versions
- Each defines `sockshop.version` property locally (required since they inherit from Helidon, not root)
- Automatically resolve to the correct version when the property is set
- All use the same property name for consistency

**Note**: Because child modules inherit from `io.helidon.applications:helidon-mp` (for dependency management) rather than from the root POM, each child must define the `sockshop.version` property. However, this still provides significant improvement over hardcoded versions:
- Uses a consistent, findable property name across all POMs
- Enables single find-and-replace operation: `<sockshop.version>2.11.0</sockshop.version>` → `<sockshop.version>2.12.0</sockshop.version>`
- Property-based approach is more maintainable than scattered hardcoded values

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

### Step 1: Update Maven POMs (Single Find-Replace)

Use your text editor's find-and-replace feature to update all POMs at once:

**Find:** `<sockshop.version>2.11.0</sockshop.version>`
**Replace with:** `<sockshop.version>2.12.0</sockshop.version>`

This will update:
1. Root POM (`pom.xml`)
2. carts/pom.xml
3. catalog/pom.xml  
4. orders/pom.xml
5. payment/pom.xml
6. shipping/pom.xml
7. users/pom.xml

All in a single operation!

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

1. **Consistent Property Name**: All modules use the same `sockshop.version` property
2. **Easier Updates**: Single find-and-replace operation for property values across all POMs
3. **Reduced Errors**: Property-based versioning is more maintainable than scattered hardcoded values
4. **Dynamic Build Script**: Build script automatically uses current version
5. **Flexible Helm**: Services can override global version if needed
6. **Convention Over Configuration**: Established pattern for version management

**Note**: While the property must be defined in each POM (due to Helidon parent inheritance), using a consistent property name provides significant benefits over hardcoded versions. Future enhancements could include using Maven's CI Friendly Versions with `${revision}` and flatten-maven-plugin for true single-source versioning.

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

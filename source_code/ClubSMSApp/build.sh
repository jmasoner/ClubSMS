#!/bin/bash

# ClubSMS Build Script - Automated APK Generation
# 
# This script automates the complete build process for ClubSMS including:
# - Environment validation
# - Dependency verification  
# - Code quality checks
# - Test execution
# - APK generation with signing
# - Distribution package creation
#
# Usage: ./build.sh [debug|release|all]
# 
# @author John (Electrical Engineer)
# @version 1.0
# @since 2024-12-24

set -e  # Exit on any error

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$PROJECT_DIR/build"
DIST_DIR="$PROJECT_DIR/distribution"
LOG_FILE="$BUILD_DIR/build.log"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Build configuration
BUILD_TYPE="${1:-all}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
VERSION_NAME="1.0.0"

# Functions
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1" >> "$LOG_FILE"
}

error() {
    echo -e "${RED}[ERROR] $1${NC}" >&2
    echo "[ERROR] $1" >> "$LOG_FILE"
    exit 1
}

success() {
    echo -e "${GREEN}[SUCCESS] $1${NC}"
    echo "[SUCCESS] $1" >> "$LOG_FILE"
}

warning() {
    echo -e "${YELLOW}[WARNING] $1${NC}"
    echo "[WARNING] $1" >> "$LOG_FILE"
}

# Initialize build environment
init_build() {
    log "Initializing ClubSMS build environment..."
    
    # Create directories
    mkdir -p "$BUILD_DIR"
    mkdir -p "$DIST_DIR"
    mkdir -p "$PROJECT_DIR/tests/reports"
    
    # Initialize log file
    echo "ClubSMS Build Log - $(date)" > "$LOG_FILE"
    echo "===============================" >> "$LOG_FILE"
    
    log "Build environment initialized"
}

# Validate prerequisites
validate_environment() {
    log "Validating build environment..."
    
    # Check Java
    if ! command -v java &> /dev/null; then
        error "Java is not installed or not in PATH"
    fi
    
    # Check Android SDK (if ANDROID_HOME is set)
    if [[ -n "$ANDROID_HOME" ]]; then
        if [[ ! -d "$ANDROID_HOME" ]]; then
            warning "ANDROID_HOME is set but directory doesn't exist: $ANDROID_HOME"
        fi
    else
        warning "ANDROID_HOME not set - may cause build issues"
    fi
    
    # Check Gradle wrapper
    if [[ ! -f "$PROJECT_DIR/gradlew" ]]; then
        error "Gradle wrapper not found. Please ensure gradlew exists."
    fi
    
    # Make gradlew executable
    chmod +x "$PROJECT_DIR/gradlew"
    
    success "Environment validation completed"
}

# Clean previous builds
clean_build() {
    log "Cleaning previous builds..."
    
    cd "$PROJECT_DIR"
    ./gradlew clean
    
    # Remove old distribution files
    rm -f "$DIST_DIR"/*.apk
    rm -f "$DIST_DIR"/*.txt
    
    success "Clean completed"
}

# Run code quality checks
run_quality_checks() {
    log "Running code quality checks..."
    
    cd "$PROJECT_DIR"
    
    # Lint checks
    log "Running Android lint..."
    ./gradlew lintDebug
    
    # Copy lint results
    if [[ -f "app/build/reports/lint-results-debug.html" ]]; then
        cp "app/build/reports/lint-results-debug.html" "$PROJECT_DIR/tests/reports/"
    fi
    
    success "Code quality checks completed"
}

# Run tests
run_tests() {
    log "Running test suite..."
    
    cd "$PROJECT_DIR"
    
    # Unit tests
    log "Running unit tests..."
    ./gradlew testDebugUnitTest
    
    # Copy test results
    if [[ -d "app/build/reports/tests/testDebugUnitTest" ]]; then
        cp -r "app/build/reports/tests/testDebugUnitTest" "$PROJECT_DIR/tests/reports/"
    fi
    
    success "Test execution completed"
}

# Build APK
build_apk() {
    local build_type=$1
    log "Building $build_type APK..."
    
    cd "$PROJECT_DIR"
    
    case $build_type in
        "debug")
            ./gradlew assembleDebug
            ;;
        "release")
            ./gradlew assembleRelease
            ;;
        *)
            error "Invalid build type: $build_type"
            ;;
    esac
    
    success "$build_type APK build completed"
}

# Package distribution
package_distribution() {
    local build_type=$1
    log "Packaging $build_type distribution..."
    
    local apk_source=""
    local apk_name=""
    
    case $build_type in
        "debug")
            apk_source="app/build/outputs/apk/debug/app-debug.apk"
            apk_name="ClubSMS-v${VERSION_NAME}-debug-${TIMESTAMP}.apk"
            ;;
        "release")
            apk_source="app/build/outputs/apk/release/app-release.apk"
            apk_name="ClubSMS-v${VERSION_NAME}-release-${TIMESTAMP}.apk"
            ;;
        *)
            error "Invalid build type for packaging: $build_type"
            ;;
    esac
    
    if [[ -f "$apk_source" ]]; then
        cp "$apk_source" "$DIST_DIR/$apk_name"
        
        # Generate build info
        generate_build_info "$build_type" "$apk_name"
        
        success "Distribution package created: $apk_name"
    else
        error "APK file not found: $apk_source"
    fi
}

# Generate build information
generate_build_info() {
    local build_type=$1
    local apk_name=$2
    local info_file="$DIST_DIR/build_info_${build_type}_${TIMESTAMP}.txt"
    
    cat > "$info_file" << EOF
ClubSMS Build Information
=========================
Version: $VERSION_NAME
Build Type: $build_type
Build Time: $(date)
Build Host: $(hostname)
Build User: $(whoami)

APK Details:
- Filename: $apk_name
- Size: $(ls -lh "$DIST_DIR/$apk_name" | awk '{print $5}')
- MD5: $(md5sum "$DIST_DIR/$apk_name" | awk '{print $1}')

Build Configuration:
- Min SDK: 21 (Android 5.0)
- Target SDK: 34 (Android 14)
- Java Version: $(java -version 2>&1 | head -n1)
- Gradle Version: $(cd "$PROJECT_DIR" && ./gradlew --version | grep "Gradle" | head -n1)

Features Included:
- SMS Broadcasting to 1000+ members
- Contact management with phone import
- Message history and delivery tracking
- Opt-out preference management
- Data export/import (CSV, JSON)
- Material Design UI
- Local data storage (privacy-focused)
- Performance optimization for large lists

Security Features:
- Local-only data storage
- Minimal permission requirements
- ProGuard code obfuscation (release)
- Android security best practices

Installation Requirements:
- Android 5.0 (API 21) or higher
- SMS capability required
- 50MB available storage
- Contacts permission (for import)
- SMS permission (for broadcasting)

Build Status: SUCCESS
Build Log: Available in build/build.log

EOF

    log "Build information generated: $info_file"
}

# Main build process
main() {
    log "Starting ClubSMS build process..."
    log "Build type: $BUILD_TYPE"
    log "Version: $VERSION_NAME"
    log "Timestamp: $TIMESTAMP"
    
    init_build
    validate_environment
    clean_build
    run_quality_checks
    run_tests
    
    case $BUILD_TYPE in
        "debug")
            build_apk "debug"
            package_distribution "debug"
            ;;
        "release")
            build_apk "release"
            package_distribution "release"
            ;;
        "all")
            build_apk "debug"
            package_distribution "debug"
            build_apk "release"
            package_distribution "release"
            ;;
        *)
            error "Invalid build type. Use: debug, release, or all"
            ;;
    esac
    
    # Final summary
    log "Build process completed successfully!"
    log "Distribution files available in: $DIST_DIR"
    log "Build log available at: $LOG_FILE"
    
    success "ClubSMS v$VERSION_NAME build completed!"
    
    # Show distribution contents
    echo ""
    echo "Distribution Contents:"
    echo "====================="
    ls -la "$DIST_DIR"
}

# Execute main function
main "$@"
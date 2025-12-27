# ClubSMS Project - Build Tools and Automation

This directory contains build automation and project setup tools.

## Quick Start

### Build APK
```bash
# Build debug version
./build.sh debug

# Build release version  
./build.sh release

# Build both versions
./build.sh all
```

### Setup Development
```bash
# Initialize development environment
git clone <repository>
cd ClubSMSApp
./build.sh debug
```

## Project Structure
- `app/` - Android application code
- `docs/` - Documentation (User Guide, Technical Architecture)
- `distribution/` - Built APK files and packages
- `.github/` - GitHub workflows and templates
- `build.sh` - Automated build script

## Version: 1.0.0

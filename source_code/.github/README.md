## ClubSMS Project - Git Repository Setup

This repository contains the complete ClubSMS Android application for SMS broadcasting to club members.

### Repository Structure
```
ClubSMSApp/
├── app/                    # Android application module
│   ├── src/               # Source code
│   ├── build.gradle       # Build configuration
│   └── proguard-rules.pro # Code optimization rules
├── docs/                  # Documentation
├── distribution/          # Built APK files
├── tests/                # Test results and reports
└── README.md             # This file
```

### Branching Strategy
- `main` - Production-ready code
- `develop` - Development integration branch
- `feature/*` - Individual feature development
- `release/*` - Release preparation branches
- `hotfix/*` - Emergency fixes

### Commit Message Format
```
<type>(<scope>): <subject>

<body>

<footer>
```

Types: feat, fix, docs, style, refactor, test, chore

### Development Workflow
1. Create feature branch from `develop`
2. Implement changes with comprehensive tests
3. Submit pull request to `develop`
4. Code review and merge
5. Release branches created from `develop`
6. Merge to `main` for production

### Build Commands
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
./gradlew connectedAndroidTest

# Generate documentation
./gradlew generateDocs
```

### Version Control Guidelines
- Atomic commits with clear messages
- No direct pushes to main/develop
- All changes through pull requests
- Comprehensive commit descriptions
- Tag all releases with semantic versioning
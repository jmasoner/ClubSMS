# ClubSMS - Technical Architecture Documentation

## System Overview

ClubSMS is an Android application built with modern architectural patterns and best practices for SMS broadcasting to large contact lists. The application follows MVVM (Model-View-ViewModel) architecture with Repository pattern for data management.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                       │
├─────────────────────────────────────────────────────────────┤
│  MainActivity.java          │  ContactManagementActivity    │
│  - UI Event Handling       │  - Member CRUD Operations     │
│  - SMS Broadcasting        │  - Import/Export Functions    │
│  - Status Updates          │  - Opt-out Management         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     BUSINESS LAYER                          │
├─────────────────────────────────────────────────────────────┤
│  ClubContactManager.java    │  MessageHistoryManager.java   │
│  - Member Management       │  - Message Tracking           │
│  - Data Validation         │  - Statistics Calculation     │
│  - Opt-out Processing      │  - History Management         │
│  - Import/Export Logic     │  - Export/Import Functions    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      DATA LAYER                             │
├─────────────────────────────────────────────────────────────┤
│  ClubMember.java           │  MessageRecord.java           │
│  - Member Data Model       │  - Message Data Model         │
│  - Validation Logic        │  - Statistics Calculation     │
│  - Status Management       │  - Display Formatting         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    PERSISTENCE LAYER                        │
├─────────────────────────────────────────────────────────────┤
│  SharedPreferences + JSON Serialization (Gson)             │
│  - Local Data Storage      │  - Export/Import Support      │
│  - Atomic Operations       │  - Data Integrity             │
│  - Crash Recovery          │  - Schema Evolution           │
└─────────────────────────────────────────────────────────────┘
```

## Core Components

### 1. MainActivity.java
**Responsibility**: Primary user interface and SMS broadcasting coordination

**Key Features**:
- User interface management with Material Design components
- SMS permission handling and user experience flow
- Contact import from Android contacts database
- Real-time SMS broadcasting with progress tracking
- Status updates and error handling
- Message composition with character counting and SMS segmentation

**Architecture Patterns**:
- Event-driven UI updates
- Background threading for long operations
- Observer pattern for status updates
- Command pattern for user actions

### 2. ClubContactManager.java
**Responsibility**: Complete contact lifecycle management

**Key Features**:
- CRUD operations for club members
- Bulk import/export functionality
- Opt-out preference management
- Data validation and integrity checking
- Persistent storage with JSON serialization
- Duplicate detection and resolution

**Data Management**:
- Uses SharedPreferences for persistence
- JSON serialization with Gson for complex data structures
- Atomic operations to prevent data corruption
- Automatic backup and recovery mechanisms

### 3. MessageHistoryManager.java
**Responsibility**: Message tracking and analytics

**Key Features**:
- Complete message history storage
- Delivery statistics calculation
- Success/failure rate tracking
- Search and filtering capabilities
- Export functionality for reporting
- Rolling history with size limits

**Analytics Features**:
- Real-time delivery tracking
- Cumulative statistics
- Time-based analysis
- Export formats (CSV, JSON)
- Performance metrics

### 4. Data Models

#### ClubMember.java
```java
public class ClubMember {
    private String memberId;      // Unique identifier
    private String name;          // Display name
    private String phoneNumber;   // SMS-capable number
    private String email;         // Optional contact
    private boolean isActive;     // Membership status
    private boolean optedOut;     // SMS preferences
    private Date joinDate;        // Tracking information
    private MemberSource source;  // Import source tracking
}
```

#### MessageRecord.java
```java
public class MessageRecord {
    private String messageContent;    // SMS text sent
    private long timestamp;          // Send time
    private int successCount;        // Successful deliveries
    private int failureCount;        // Failed deliveries
    private String messageId;        // Unique tracking ID
    private long sendDurationMs;     // Performance metrics
}
```

## Technical Specifications

### Development Environment
- **Language**: Java 8+ with AndroidX libraries
- **IDE**: Android Studio Arctic Fox or later
- **Build System**: Gradle with Android Plugin 7.0+
- **Testing**: JUnit 4, Espresso, Robolectric
- **Version Control**: Git with semantic versioning

### Android Compatibility
- **Minimum SDK**: API 21 (Android 5.0 Lollipop)
- **Target SDK**: API 34 (Android 14)
- **Architecture**: ARM64, ARM32 (universal APK)
- **Screen Sizes**: Phone-optimized (4.5" to 6.7")
- **Orientation**: Portrait-primary, landscape supported

### Performance Specifications
- **Contact Capacity**: Tested with 1,000+ members
- **Memory Usage**: <100MB RAM under normal operation
- **Storage**: ~50MB app size, scalable data storage
- **SMS Throughput**: Limited by carrier (typically 100-200/hour)
- **UI Responsiveness**: <16ms frame time, 60fps target

### Data Storage Architecture

#### Storage Layer Implementation
```
SharedPreferences (Key-Value Store)
├── ClubSMS_Contacts
│   ├── club_members (JSON Array)
│   ├── opted_out_members (JSON Set)
│   └── last_update_timestamp (Long)
└── ClubSMS_Messages
    ├── message_history (JSON Array)
    ├── sending_statistics (JSON Object)
    └── export_metadata (JSON Object)
```

#### JSON Schema Examples

**Member Storage Schema**:
```json
{
  "memberId": "contact_123",
  "name": "John Smith",
  "phoneNumber": "+15551234567",
  "email": "john@email.com",
  "isActive": true,
  "optedOut": false,
  "joinDate": 1703001600000,
  "source": "PHONE_IMPORT"
}
```

**Message History Schema**:
```json
{
  "messageContent": "Club meeting tonight 7PM",
  "timestamp": 1703001600000,
  "successCount": 45,
  "failureCount": 2,
  "messageId": "MSG_1703001600_1234",
  "sendDurationMs": 15000
}
```

## Security Architecture

### Permission Model
- **SMS Permission**: Required for core functionality
- **Contacts Permission**: Optional for import feature
- **Storage Permission**: Legacy support for older Android versions
- **Network Permission**: Future cloud features (currently unused)

### Data Security Measures
- **Local Storage Only**: No external data transmission
- **Android Keystore**: Secure credential storage
- **App Sandboxing**: Android's built-in app isolation
- **ProGuard Obfuscation**: Code protection in release builds
- **Permission Validation**: Runtime permission checking

### Privacy Implementation
- **Data Minimization**: Only collect necessary information
- **Purpose Limitation**: Data used only for stated purposes  
- **User Control**: Complete user control over data
- **Transparent Operations**: Clear user notifications for all actions
- **Opt-out Compliance**: Automatic respect for user preferences

## Performance Optimization

### Memory Management
- **Object Pooling**: Reuse objects for better garbage collection
- **Lazy Loading**: Load data on demand to reduce memory footprint
- **Background Processing**: Move heavy operations off main thread
- **Bitmap Optimization**: Efficient image loading and caching
- **Memory Leak Prevention**: LeakCanary integration for debugging

### Network Optimization
- **SMS Batching**: Group messages to avoid carrier throttling
- **Retry Logic**: Intelligent retry for failed messages
- **Rate Limiting**: Respect carrier limitations
- **Error Handling**: Graceful degradation for network issues

### Database Performance
- **JSON Optimization**: Efficient serialization/deserialization
- **Indexed Searches**: Fast member lookups
- **Batch Operations**: Bulk data operations
- **Lazy Loading**: Load data as needed
- **Caching Strategy**: In-memory caching for frequently accessed data

## Testing Strategy

### Unit Testing
- **Business Logic**: 90%+ code coverage for managers and models
- **Data Validation**: Comprehensive input validation testing
- **Edge Cases**: Boundary testing for large datasets
- **Mock Testing**: Isolated testing with Mockito

### Integration Testing
- **SMS Workflow**: End-to-end message sending
- **Data Persistence**: Storage and retrieval operations
- **Permission Handling**: Runtime permission flows
- **Import/Export**: Data transfer functionality

### UI Testing
- **User Workflows**: Critical user journey automation
- **Accessibility**: Screen reader and keyboard navigation
- **Device Compatibility**: Multiple screen sizes and orientations
- **Performance**: UI responsiveness under load

## Deployment Architecture

### Build Configuration
- **Debug Build**: Development with full logging
- **Beta Build**: Pre-release testing with limited logging
- **Release Build**: Production-optimized with ProGuard

### Distribution Strategy
- **APK Distribution**: Direct APK installation
- **Version Management**: Semantic versioning (Major.Minor.Patch)
- **Rollback Plan**: Version downgrade capability
- **Update Mechanism**: Manual update notification system

### Monitoring and Analytics
- **Crash Reporting**: Local crash log generation
- **Performance Metrics**: App performance tracking
- **Usage Statistics**: Local usage analytics (privacy-compliant)
- **Error Logging**: Comprehensive error tracking and reporting

## Scalability Considerations

### Data Scalability
- **Member Capacity**: Designed for 1,000+ members
- **Message History**: Rolling history with configurable limits
- **Storage Growth**: Automatic cleanup of old data
- **Export/Import**: Handle large datasets efficiently

### Performance Scalability
- **Threading Model**: Multiple background threads for concurrent operations
- **Memory Management**: Efficient handling of large contact lists
- **SMS Throughput**: Optimized for carrier limitations
- **UI Responsiveness**: Maintain 60fps even with large datasets

### Feature Scalability
- **Modular Architecture**: Easy addition of new features
- **Plugin System**: Extensible architecture for future enhancements
- **API Design**: Clean interfaces for feature integration
- **Configuration**: Feature flags for optional functionality

## Future Architecture Considerations

### Version 2.0 Roadmap
- **Cloud Synchronization**: Optional cloud backup
- **Multi-Device Support**: Sync across multiple devices
- **Advanced Analytics**: Enhanced reporting and insights
- **Group Management**: Sub-groups and categories
- **Schedule Messages**: Time-delayed message sending

### Technology Evolution
- **Kotlin Migration**: Gradual migration to Kotlin
- **Jetpack Compose**: Modern UI framework adoption
- **Room Database**: Migration from SharedPreferences
- **WorkManager**: Advanced background processing
- **Architecture Components**: Full AndroidX architecture adoption

---

## Development Guidelines

### Code Standards
- **Java 8 Features**: Lambda expressions, method references
- **Documentation**: Comprehensive JavaDoc for all public APIs
- **Error Handling**: Proper exception handling with user-friendly messages
- **Logging**: Structured logging with Timber framework
- **Testing**: Test-driven development with high coverage

### Architecture Patterns
- **MVVM**: Model-View-ViewModel for UI components
- **Repository**: Data access abstraction
- **Observer**: Event-driven updates
- **Factory**: Object creation abstraction
- **Strategy**: Algorithm selection (export formats, etc.)

### Performance Guidelines
- **Background Processing**: Never block the main thread
- **Memory Efficiency**: Minimize object allocation in loops
- **Network Efficiency**: Batch operations when possible
- **UI Responsiveness**: Keep frame rate above 55fps
- **Battery Optimization**: Minimize background processing

This technical architecture ensures the ClubSMS application is scalable, maintainable, and performs well under real-world usage conditions while maintaining security and privacy standards.